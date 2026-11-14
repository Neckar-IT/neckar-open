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
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

/**
 * Flags **capturing lambdas that become a real instance** inside a `@Hot` body.
 *
 * A lambda that reads values from its enclosing scope (local variables, parameters, explicit
 * `this`) cannot be compiled to a singleton — every evaluation allocates a fresh instance holding
 * the captured values. Per frame, in a `@Hot` path, that is a hidden allocation the other hot-path
 * rules cannot see (instantiations are constructor-level, not call-level).
 *
 * What does NOT allocate and is therefore not flagged:
 * - **Non-capturing** lambdas (`{ it >= 0.0 }`, `{}`-defaults, `{ null }`) — the compiler hoists
 *   them to a static singleton automatically.
 * - Capturing lambdas passed to an **inline function's function-type parameter** — the lambda is
 *   compiled away entirely, captures are free.
 *
 * What DOES allocate and is flagged (when capturing):
 * - Lambda arguments to non-inline callees and to constructors.
 * - Lambda arguments to `noinline` parameters of inline callees.
 * - **SAM conversions onto a `fun interface` parameter — even on inline callees**: only real
 *   function types are inlined; a `fun interface` parameter always materializes a SAM instance.
 * - Lambdas without a call context (assignments, returns) — no inlining can rescue those.
 *
 * A deliberate instance is acknowledged with `@HotAllocation("reason")` at the statement.
 *
 * Honest limits: implicit `this` captures (bare member access like `paintCount++` without an
 * explicit `this`) and bound callable references (`x::foo`) are not detected in this version —
 * the runtime allocation recorder covers them. Explicit `this` inside nested receiver scopes may
 * over-report; acknowledge with a reason if the receiver is provably static.
 */
class HotFunctionMustNotAllocateCapturingLambda(config: Config) : Rule(
  config,
  description = "A capturing lambda inside a `@Hot` body allocates a fresh instance per evaluation unless it is inlined. Hoist the state, use an inline callee, or acknowledge with `@HotAllocation(\"reason\")`.",
), RequiresAnalysisApi {

  override fun visitNamedFunction(function: KtNamedFunction) {
    super.visitNamedFunction(function)

    if (!function.hasHotAnnotationEntry()) return
    val body = function.bodyBlockExpression ?: function.bodyExpression ?: return

    analyze(function) {
      if (!function.symbol.isHotAnnotated()) return@analyze

      body.collectDescendantsOfType<KtLambdaExpression>().forEach { lambda ->
        reportIfAllocatingCapturingLambda(lambda)
      }
    }
  }

  private fun KaSession.reportIfAllocatingCapturingLambda(lambda: KtLambdaExpression) {
    if (isInlinedFunctionTypeArgument(lambda)) return

    val capturedName = findCapturedSymbolName(lambda) ?: return

    if (lambda.isAcknowledgedHotAllocation()) return

    report(
      Finding(
        Entity.from(lambda),
        "This lambda captures `$capturedName` from the enclosing scope and is not inlined — every " +
          "evaluation allocates a fresh instance carrying the captured values. In a `@Hot` body this " +
          "is a per-call allocation. Hoist the state out of the lambda (non-capturing lambdas become " +
          "compiler singletons), route the call through an inline function-type parameter, or " +
          "acknowledge a deliberate instance with `@HotAllocation(\"reason\")`.",
      ),
    )
  }

  /**
   * True when the lambda is passed to an inline callee's function-type parameter (not `noinline`) —
   * the compiler inlines the body, no instance is created. A `fun interface` parameter does NOT
   * qualify: SAM conversion materializes an instance even on inline callees.
   */
  private fun KaSession.isInlinedFunctionTypeArgument(lambda: KtLambdaExpression): Boolean {
    val callExpression = findEnclosingCallFor(lambda) ?: return false
    val call = callExpression.resolveToCall()?.successfulCallOrNull<KaFunctionCall<*>>() ?: return false
    val calleeSymbol = call.symbol as? KaNamedFunctionSymbol ?: return false
    if (!calleeSymbol.isInline) return false

    // With a labeled lambda (`foo label@{ }`) the mapping key is the wrapping KtLabeledExpression
    val argumentExpression = (lambda.parent as? KtLabeledExpression) ?: lambda
    val parameterSymbol: KaValueParameterSymbol = call.argumentMapping.entries
      .firstOrNull { it.key === argumentExpression || it.key.textRange == argumentExpression.textRange }
      ?.value?.symbol
      ?: return false

    if (parameterSymbol.isNoinline) return false

    val parameterTypeFqn = (parameterSymbol.returnType as? KaClassType)?.classId?.asFqNameString() ?: return false
    return parameterTypeFqn.matches(functionTypeFqnRegex)
  }

  /**
   * The call expression this lambda is an argument of — trailing (`foo { }`), parenthesized
   * (`foo({ })`), or labeled (`foo label@{ }`, where a [KtLabeledExpression] wraps the lambda).
   */
  private fun findEnclosingCallFor(lambda: KtLambdaExpression): KtCallExpression? {
    var candidate: PsiElement? = lambda.parent
    if (candidate is KtLabeledExpression) {
      candidate = candidate.parent
    }
    return when (candidate) {
      is KtLambdaArgument -> candidate.parent as? KtCallExpression
      is KtValueArgument -> candidate.parent?.parent as? KtCallExpression
      else -> null
    }
  }

  /**
   * Name of the first symbol the lambda captures from its enclosing scope, or `null` for a
   * non-capturing lambda. Captures detected: local variables and value parameters declared outside
   * the lambda, and explicit `this`. Implicit `this` (bare member access) is a known blind spot.
   */
  private fun KaSession.findCapturedSymbolName(lambda: KtLambdaExpression): String? {
    val literal = lambda.functionLiteral
    val lambdaBody = literal.bodyExpression ?: return null

    if (lambdaBody.collectDescendantsOfType<KtThisExpression>().isNotEmpty()) {
      return "this"
    }

    lambdaBody.collectDescendantsOfType<KtSimpleNameExpression>().forEach { reference ->
      val symbol = reference.resolveToCall()?.successfulCallOrNull<KaVariableAccessCall>()?.symbol ?: return@forEach
      if (symbol !is KaLocalVariableSymbol && symbol !is KaValueParameterSymbol) return@forEach

      // psi == null: implicit lambda parameter `it` — declared by this (or an outer) lambda, not a capture from the function scope
      val declarationPsi = symbol.psi ?: return@forEach
      if (literal.isAncestor(declarationPsi, false)) return@forEach

      return reference.getReferencedName()
    }

    return null
  }

  companion object {
    /** `kotlin.Function0`..`kotlin.FunctionN` — real function types that inline callees compile away. */
    private val functionTypeFqnRegex: Regex = Regex("""kotlin\.Function\d+""")
  }
}
