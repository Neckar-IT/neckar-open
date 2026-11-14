/*
 * Copyright (C) 2013-2026 Neckar IT GmbH, Mössingen, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules, regardless of
 * the license terms of these independent modules, and to copy and distribute
 * the resulting combined work under terms of your choice, provided that every
 * copy of the combined work is accompanied by a complete copy of the source
 * code of this library.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package it.neckar.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * Flags every call of a MongoDB-coroutine-API `MongoCollection` method that runs inside a
 * `context(ClientSession)` / `context(ActiveTransaction)` / `ClientSession`-parameter /
 * `withTransaction { … }` / `withTransactionRetrying { … }` scope but does not pass the in-scope `ClientSession` as its first
 * argument: the context parameter itself, `<tx>.clientSession` of an `ActiveTransaction`, or
 * `contextOf<ActiveTransaction>().clientSession` inside a transaction block.
 *
 * The Kotlin MongoDB coroutine driver publishes two overloads per operation — one with an explicit
 * `ClientSession` first parameter and one without. Only the former participates in the surrounding
 * transaction. The compiler does **not** inject the session automatically from the context receiver,
 * so it is easy to pick the wrong overload by omission.
 *
 * The rule uses Detekt's Analysis API ([RequiresAnalysisApi]) to resolve each call to its target
 * function and compares the target's declaring class FQN against
 * `com.mongodb.kotlin.client.coroutine.MongoCollection`. This way, project-local extension wrappers
 * declared in `MongoExt.kt` (which already propagate the session correctly) are not mis-flagged as
 * violations. Only direct calls into the bare MongoDB API are candidates.
 */
class MissingClientSessionRule(config: Config) : Rule(
  config,
  description = "MongoDB operations inside a ClientSession or ActiveTransaction scope must pass the session as the first argument — otherwise the call runs outside the surrounding transaction.",
), RequiresAnalysisApi {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    val methodName = resolveDirectMongoApiMethod(expression) ?: return

    val sessionNamesInScope = collectInScopeSessionNames(expression)
    if (sessionNamesInScope.isEmpty()) return

    if (callForwardsSession(expression, sessionNamesInScope)) return

    val callee = expression.calleeExpression ?: return
    report(
      Finding(
        Entity.from(callee),
        "Call to MongoCollection.$methodName does not pass the in-scope ClientSession " +
          "(${sessionNamesInScope.joinToString()}) as first argument — it will run outside the surrounding transaction. " +
          "Pass the session explicitly: collection.$methodName(${sessionNamesInScope.first()}, …).",
      ),
    )
  }

  /**
   * Returns the method name if [expression] resolves to a direct call on
   * `com.mongodb.kotlin.client.coroutine.MongoCollection` (one of the methods that requires a
   * session as first argument). Returns `null` for anything else — in particular, for the
   * project-local extension wrappers defined in `MongoExt.kt`, which resolve to
   * `it.neckar.open.mongodb.*` and already propagate the session correctly.
   */
  private fun resolveDirectMongoApiMethod(expression: KtCallExpression): String? {
    return analyze(expression) {
      val call = expression.resolveToCall()?.successfulCallOrNull<KaCallableMemberCall<*, *>>()
        ?: return@analyze null
      val functionCall = call as? KaFunctionCall ?: return@analyze null
      val symbol = functionCall.symbol as? KaNamedFunctionSymbol ?: return@analyze null

      val callableId = symbol.callableId ?: return@analyze null
      val classId = callableId.classId ?: return@analyze null

      val isMongoCollection = classId.packageFqName.asString() == MongoCollectionPackage &&
        classId.shortClassName.asString() == MongoCollectionSimpleName
      if (!isMongoCollection) return@analyze null

      val methodName = callableId.callableName.asString()
      methodName.takeIf { it in mongoCollectionMethods }
    }
  }

  /** The expressions under which a `ClientSession` is reachable at [callSite]: context parameters, value parameters, the session of an `ActiveTransaction`. */
  private fun collectInScopeSessionNames(callSite: KtElement): Set<String> {
    val sessionNames = mutableSetOf<String>()
    var current: KtElement? = callSite

    while (current != null) {
      when (current) {
        // Before KtFunction: a function literal is a KtFunction too
        is KtFunctionLiteral -> {
          if (isTransactionalLambda(current)) {
            sessionNames.add(ContextActiveTransactionSession)
          }
        }

        is KtFunction -> {
          collectContextReceiverNames(current, sessionNames)
          current.valueParameters
            .filter { it.typeReference?.text?.substringAfterLast('.') == ClientSessionSimpleName }
            .mapNotNullTo(sessionNames) { it.name }
        }

        is KtClassOrObject -> {
          collectContextReceiverNames(current, sessionNames)
        }
      }

      current = current.parent as? KtElement
    }

    return sessionNames
  }

  /**
   * PSI-version-agnostic context-parameter/receiver discovery. Scans the text before the
   * function's body / value-parameter-list for `context(… ClientSession …)` and
   * `context(… ActiveTransaction …)` declarations. Works
   * under both the K1 `KtContextReceiverList` (Kotlin 2.3.0, Detekt-bundled) and the K2
   * `KtContextParameterList` (Kotlin 2.3.20+) without importing a PSI class that exists in only
   * one of them.
   */
  private fun collectContextReceiverNames(host: KtElement, sessionNames: MutableSet<String>) {
    if (host !is KtFunction && host !is KtClassOrObject) return

    val bodyOffset = when (host) {
      is KtFunction -> host.valueParameterList?.startOffsetInParent ?: return
      is KtClassOrObject -> host.getPrimaryConstructorParameterList()?.startOffsetInParent ?: host.body?.startOffsetInParent ?: return
      else -> return
    }

    val header = host.text?.substring(0, bodyOffset) ?: return
    val contextMatch = CONTEXT_CLAUSE_REGEX.find(header) ?: return
    val inside = contextMatch.groupValues[1]

    CONTEXT_PARAM_REGEX.findAll(inside).forEach { match ->
      val paramName = match.groupValues[1].ifEmpty { ClientSessionContextDefaultName }
      sessionNames.add(paramName)
    }
    ACTIVE_TRANSACTION_PARAM_REGEX.findAll(inside).forEach { match ->
      val paramName = match.groupValues[1]
      sessionNames.add(if (paramName.isEmpty() || paramName == "_") ContextActiveTransactionSession else "$paramName.clientSession")
    }
  }

  private fun isTransactionalLambda(lambda: KtFunctionLiteral): Boolean {
    val lambdaExpr = lambda.parent as? KtLambdaExpression ?: return false
    val lambdaArg = (lambdaExpr.parent as? KtLambdaArgument)
      ?: (lambdaExpr.parent as? KtValueArgument)
      ?: return false

    val outerCall = lambdaArg.parent as? KtCallExpression
      ?: (lambdaArg.parent?.parent as? KtCallExpression)
      ?: return false

    val outerCallee = outerCall.calleeExpression?.text ?: return false
    return outerCallee in transactionalLambdaCallees
  }

  private fun callForwardsSession(
    expression: KtCallExpression,
    sessionNamesInScope: Set<String>,
  ): Boolean {
    val firstArg = expression.valueArguments.firstOrNull() ?: return false
    val argExpression = firstArg.getArgumentExpression() ?: return false

    val argumentName = firstArg.getArgumentName()?.asName?.asString()
    if (argumentName != null && argumentName != ClientSessionNamedArg) return false

    val argText = (argExpression as? KtNameReferenceExpression)?.getReferencedName()
      ?: argExpression.text

    return argText in sessionNamesInScope || argText == "this"
  }

  companion object {
    private const val MongoCollectionPackage = "com.mongodb.kotlin.client.coroutine"
    private const val MongoCollectionSimpleName = "MongoCollection"
    private const val ClientSessionSimpleName = "ClientSession"
    private const val ClientSessionContextDefaultName = "clientSession"
    private const val ClientSessionNamedArg = "clientSession"
    private const val ContextActiveTransactionSession = "contextOf<ActiveTransaction>().clientSession"

    private val mongoCollectionMethods = setOf(
      "find", "updateOne", "updateMany", "replaceOne", "deleteOne", "deleteMany",
      "insertOne", "insertMany", "bulkWrite", "aggregate", "findOneAndUpdate",
      "findOneAndReplace", "findOneAndDelete", "countDocuments", "distinct",
    )

    private val transactionalLambdaCallees = setOf("withTransaction", "withTransactionRetrying")

    /** Matches a top-level `context(…)` clause before the function body. */
    private val CONTEXT_CLAUSE_REGEX = Regex("""context\s*\(([^)]*)\)""")

    /** Matches `name: ClientSession` or `_: ClientSession` or bare `ClientSession` inside a context clause. */
    private val CONTEXT_PARAM_REGEX = Regex("""(?:(\w+)\s*:\s*)?(?:[\w.]*\.)?ClientSession\b""")

    /** Matches `name: ActiveTransaction` or `_: ActiveTransaction` or bare `ActiveTransaction` inside a context clause. */
    private val ACTIVE_TRANSACTION_PARAM_REGEX = Regex("""(?:(\w+)\s*:\s*)?(?:[\w.]*\.)?ActiveTransaction\b""")
  }
}
