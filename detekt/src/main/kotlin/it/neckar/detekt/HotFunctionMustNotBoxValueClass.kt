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
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Flags **value-class boxing inside a `@Hot` method body**.
 *
 * A `@JvmInline value class` is unboxed only while its static type stays that value class. The
 * moment it passes through an **erased / generic position** — a generic return `getValue(): T`, an
 * indexed read `list[i]` (`get(): E`), `provider.valueAt(i): T` — the JVM boxes it into its wrapper.
 * Per frame, in a `@Hot` path, that boxing allocates measurably.
 *
 * Detection is precise: the callee's **declared** return type must be a type parameter
 * ([KaTypeParameterType]) while the **actual** (substituted) type at the call site is a value class.
 * That is exactly the "value flowing through erasure" case. A function that declares a value class
 * directly (`fun foo(): Vc`) does not box and is not flagged. Primitive boxing (`T = Int`) is out of
 * scope here — it is covered by the `@NotBoxed` KSP verifier.
 *
 * A deliberate allocation is acknowledged at the call site with `@HotAllocation("reason")` (on
 * the expression or the local variable) — the finding is suppressed and the decision stays
 * visible and greppable ([isAcknowledgedHotAllocation]).
 *
 * Honest limits: `inline` functions (`fastForEach`, `gc.saved`) can hide the
 * boxing callee behind their body, so not every boxing site is reachable statically. The runtime
 * allocation recorder remains the stronger, exhaustive guardrail; this rule is the
 * early, compile-time complement.
 */
class HotFunctionMustNotBoxValueClass(config: Config) : Rule(
  config,
  description = "A `@Hot` method must not box a value class. Reading a value class through a generic position (`provider.valueAt(i)`, `list[i]`, generic `getValue(): T`) boxes it at the erased boundary — a per-frame allocation.",
), RequiresAnalysisApi {

  override fun visitNamedFunction(function: KtNamedFunction) {
    super.visitNamedFunction(function)

    if (!function.hasHotAnnotationEntry()) return
    val body = function.bodyBlockExpression ?: function.bodyExpression ?: return

    analyze(function) {
      if (!function.symbol.isHotAnnotated()) return@analyze

      val boxingCandidates: List<KtExpression> =
        body.collectDescendantsOfType<KtCallExpression>() + body.collectDescendantsOfType<KtArrayAccessExpression>()

      boxingCandidates.forEach { expression -> reportIfBoxesValueClass(expression) }
    }
  }

  private fun KaSession.reportIfBoxesValueClass(expression: KtExpression) {
    val call = expression.resolveToCall()?.successfulCallOrNull<KaFunctionCall<*>>() ?: return
    if (call.symbol.returnType !is KaTypeParameterType) return

    val boxedValueClass = (expression.expressionType as? KaClassType)?.symbol as? KaClassSymbol ?: return
    if (!boxedValueClass.isValueClass()) return

    if (expression.isAcknowledgedHotAllocation()) return

    val reportTarget = (expression as? KtCallExpression)?.calleeExpression ?: expression
    report(
      Finding(
        Entity.from(reportTarget),
        "`${expression.text}` reads a value class `${boxedValueClass.shortName()}` through a generic " +
          "position — the callee's return type is a type parameter, so the value class is boxed at the " +
          "erased boundary. In a `@Hot` body this allocates per call. Read the underlying primitive " +
          "directly (e.g. a value-class-typed extension `valueAt(vc)`/`getInt(i)`), acknowledge a " +
          "deliberate allocation with `@HotAllocation(\"reason\")`, or drop `@Hot` if this path is not " +
          "per-frame.",
      ),
    )
  }
}
