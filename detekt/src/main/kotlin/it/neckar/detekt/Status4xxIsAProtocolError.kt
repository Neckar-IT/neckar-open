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
import dev.detekt.api.Rule
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

/**
 * Flags a 4xx status a Ktor server file names. ADL 0075 answers a domain outcome with HTTP 200 and a
 * `bad/<Name>` variant of its sealed response, and reserves 4xx for a protocol error — a malformed
 * request, a missing credential, a missing route.
 *
 * The frontend reads the body of a 200 and switches on `response.type`; a 4xx answer loses the domain
 * outcome before that switch ever runs.
 *
 * Four shapes reach the rule, each of them decidable in the syntax tree:
 *
 * | Shape | Example |
 * |---|---|
 * | the constant | `HttpStatusCode.BadRequest` |
 * | the lookup with an integer literal | `HttpStatusCode.fromValue(409)` |
 * | the constructor with an integer literal | `HttpStatusCode(428, "Precondition Required")` |
 * | the import of a single constant | `import io.ktor.http.HttpStatusCode.BadRequest` |
 *
 * An import alias moves the spelling and not the meaning, so the import list decides which local names
 * carry the class: `import io.ktor.http.HttpStatusCode as Status` makes `Status.Conflict` a finding.
 *
 * A status a condition tests against is read rather than answered, so an operand of a comparison and the
 * condition of a `when` subject report nothing. A status code behind a variable
 * (`HttpStatusCode.fromValue(code)`), a star import of the companion and a typealias over the class stay
 * undecidable here and report nothing either.
 *
 * The import list also decides scope: a file importing `io.ktor.server.` answers requests, a file without
 * that import answers none and stays untouched. `internal/buildplatform/config/detekt/detekt.yml` narrows it further, by test
 * source set and by the one maintenance-only product it names.
 *
 * Companions from the same ADL 0075: [RespondWithSealedSubclass], [RestResponseConstructorMustUseFactory].
 *
 * See `internal/patterns/docs/kotlin/rest-response.md`.
 */
class Status4xxIsAProtocolError(config: Config) : Rule(
  config,
  description = "A 4xx status answers a protocol error; a domain outcome carries HTTP 200 with a `bad/<Name>` variant of its sealed response.",
) {

  override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
    super.visitDotQualifiedExpression(expression)

    if (expression.isComparisonOperand()) return
    val statusCodeNames: Set<String> = expression.statusCodeNames() ?: return
    val receiver: String = expression.receiverExpression.statusCodeSpelling() ?: return
    if (receiver !in statusCodeNames) return

    when (val selector: KtExpression? = expression.selectorExpression) {
      is KtNameReferenceExpression -> {
        val statusName: String = selector.getReferencedName()
        if (statusName !in ClientErrorStatusNames) return
        reportStatus(expression, "$receiver.$statusName", SuppressAtTheDeclaration)
      }

      is KtCallExpression -> {
        if ((selector.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() != FromValueName) return
        val status: Int = selector.integerArgument() ?: return
        if (status !in ClientErrorStatusCodes) return
        reportStatus(expression, "$receiver.$FromValueName($status)", SuppressAtTheDeclaration)
      }

      else -> return
    }
  }

  /** The constructor `HttpStatusCode(428, …)`; `HttpStatusCode.fromValue(…)` arrives at [visitDotQualifiedExpression]. */
  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    if (expression.isComparisonOperand()) return
    val statusCodeNames: Set<String> = expression.statusCodeNames() ?: return
    val callee: String = (expression.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return
    if (callee !in statusCodeNames) return

    val status: Int = expression.integerArgument() ?: return
    if (status !in ClientErrorStatusCodes) return
    reportStatus(expression, "$callee($status, …)", SuppressAtTheDeclaration)
  }

  /**
   * The import of a single constant, `import io.ktor.http.HttpStatusCode.BadRequest`. It is the one place
   * the name is unambiguous: telling the bare name it leaves in the body from a local declaration of the
   * same name needs a resolved type.
   */
  override fun visitImportDirective(directive: KtImportDirective) {
    super.visitImportDirective(directive)

    val imported: String = directive.importedFqName?.asString() ?: return
    if (imported.startsWith("$httpStatusCodeFqName.").not()) return

    val statusName: String = imported.substringAfterLast('.')
    if (statusName !in ClientErrorStatusNames) return
    if (directive.containingKtFile.importsKtorServer().not()) return

    reportStatus(directive, imported, NameTheStatusThroughTheClass)
  }

  private fun reportStatus(element: KtElement, status: String, remedy: String) {
    report(
      Finding(
        Entity.from(element),
        "`$status` answers a client error. A domain outcome answers HTTP 200 with a `bad/<Name>` variant of " +
          "its sealed response — the frontend `switch (response.type)` reads the body of a 200 and loses the " +
          "outcome otherwise. A status that really answers a protocol error stays, and $remedy " +
          "See `internal/patterns/docs/kotlin/rest-response.md`.",
      ),
    )
  }

  /**
   * The names that may denote `io.ktor.http.HttpStatusCode` here — the class name itself, plus whatever
   * the import list binds to the class or to its companion — or `null` where the rule stays silent: an
   * element inside the import list, and a file that imports no Ktor server package and answers no request.
   */
  private fun KtElement.statusCodeNames(): Set<String>? {
    if (getNonStrictParentOfType<KtImportDirective>() != null) return null

    val file: KtFile = containingKtFile
    if (file.importsKtorServer().not()) return null

    val names: MutableSet<String> = mutableSetOf(httpStatusCodeName)
    file.importDirectives.forEach { directive ->
      val imported: String = directive.importedFqName?.asString() ?: return@forEach
      when (imported) {
        httpStatusCodeFqName -> directive.aliasName?.let { alias -> names += alias }
        CompanionFqName -> names += directive.aliasName ?: CompanionName
      }
    }
    return names
  }

  /**
   * A status a condition tests against is read, never answered: `response.status >= HttpStatusCode.BadRequest`
   * and the `when` subject condition `when (status) { HttpStatusCode.Conflict -> … }`. Parentheses around the
   * operand change nothing.
   */
  private fun KtExpression.isComparisonOperand(): Boolean {
    return when (val enclosing: PsiElement? = enclosingExpression()) {
      is KtBinaryExpression -> enclosing.operationToken in ComparisonTokens
      is KtWhenConditionWithExpression -> true
      else -> false
    }
  }

  /**
   * The first parent outside the qualified chain this expression belongs to, brackets skipped:
   * `(HttpStatusCode.Conflict)`, `HttpStatusCode.Conflict.value` and `io.ktor.http.HttpStatusCode(428, "…")`
   * all stand for the status they name.
   */
  private tailrec fun KtExpression.enclosingExpression(): PsiElement? {
    val enclosing: PsiElement = parent ?: return null
    return when (enclosing) {
      is KtParenthesizedExpression -> enclosing.enclosingExpression()
      is KtDotQualifiedExpression -> enclosing.enclosingExpression()
      else -> enclosing
    }
  }

  /**
   * The class name a receiver names, read from the tree so that a package qualifier, a line break inside
   * it and a `Companion` step leave the answer alone: `io.ktor.http.HttpStatusCode.Companion` names
   * `HttpStatusCode`.
   */
  private fun KtExpression.statusCodeSpelling(): String? {
    return when (this) {
      is KtNameReferenceExpression -> getReferencedName()

      is KtDotQualifiedExpression -> {
        val selector: String = (selectorExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return null
        if (selector == CompanionName) receiverExpression.statusCodeSpelling() else selector
      }

      else -> null
    }
  }

  /** Only a file importing the Ktor server answers requests; a file without that import answers none. */
  private fun KtFile.importsKtorServer(): Boolean {
    return importDirectives.any { directive ->
      val imported: String = directive.importedFqName?.asString() ?: return@any false
      imported == KtorServerPackage || imported.startsWith("$KtorServerPackage.")
    }
  }

  /**
   * The first argument spelled as an integer literal — the status code in every shape the call takes,
   * `HttpStatusCode(428, "…")` as well as `HttpStatusCode(description = "…", value = 428)`.
   * A status behind a variable stays undecidable in the syntax tree.
   */
  private fun KtCallExpression.integerArgument(): Int? {
    return valueArguments.firstNotNullOfOrNull { argument ->
      (argument.getArgumentExpression() as? KtConstantExpression)?.text?.toIntOrNull()
    }
  }

  companion object {
    private const val httpStatusCodeName: String = "HttpStatusCode"
    private const val httpStatusCodeFqName: String = "io.ktor.http.$httpStatusCodeName"
    private const val CompanionName: String = "Companion"
    private const val CompanionFqName: String = "$httpStatusCodeFqName.$CompanionName"

    /** An operand of one of these reads a status; every other position answers with it. */
    private val ComparisonTokens: Set<IElementType> = setOf(
      KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ,
      KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ,
    )

    private const val SuppressAtTheDeclaration: String =
      "carries `@Suppress(\"Status4xxIsAProtocolError\")` on its declaration or on its own statement, with the reason on the line above."

    /** An import directive takes no annotation of its own; the `@file:Suppress` it leaves lifts the rule for the whole file. */
    private const val NameTheStatusThroughTheClass: String =
      "names the status through `$httpStatusCodeName` at the place it answers, where `@Suppress(\"Status4xxIsAProtocolError\")` has a declaration to sit on."
    private const val FromValueName: String = "fromValue"
    private const val KtorServerPackage: String = "io.ktor.server"

    private val ClientErrorStatusCodes: IntRange = 400..499

    /** Every `HttpStatusCode` constant Ktor defines from 400 to 499, mirroring its companion in `io.ktor.http`. */
    private val ClientErrorStatusNames: Set<String> = setOf(
      "BadRequest", "Unauthorized", "PaymentRequired", "Forbidden", "NotFound", "MethodNotAllowed", "NotAcceptable",
      "ProxyAuthenticationRequired", "RequestTimeout", "Conflict", "Gone", "LengthRequired", "PreconditionFailed",
      "PayloadTooLarge", "RequestURITooLong", "UnsupportedMediaType", "RequestedRangeNotSatisfiable", "ExpectationFailed",
      "UnprocessableEntity", "Locked", "FailedDependency", "TooEarly", "UpgradeRequired", "PreconditionRequired",
      "TooManyRequests", "RequestHeaderFieldTooLarge",
    )
  }
}
