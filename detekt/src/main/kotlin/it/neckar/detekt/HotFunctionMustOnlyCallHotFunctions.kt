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
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Enforces **function coloring** for `@Hot`: a `@Hot` method may only call other `@Hot` methods
 * (plus provably-neutral ones). Like `suspend` or `@Composable`, the contract is a local invariant
 * that propagates transitively — every `@Hot` function being held to "call only `@Hot`" forces the
 * whole reachable call chain to be `@Hot`, so allocation-sensitivity is visible end to end.
 *
 * Neutral callees (not flagged):
 * - `inline` functions — the compiler inlines their body into the caller; there is no separate
 *   call frame. Honest limit: this rule does *not* re-analyze the inline body at the call site —
 *   the body is only checked when the inline function is itself `@Hot`. A neutral inline wrapper
 *   around a non-hot call is a known blind spot (the runtime recorder covers it).
 * - stdlib / JDK / platform-canvas functions (packages in [neutralPackagePrefixes], incl. JavaFX
 *   and the W3C DOM API) — treated as allocation-neutral leaves; coloring foreign platform APIs
 *   is neither possible nor useful.
 * - constructors — object construction is an allocation concern, not a coloring one (a `@Hot` body
 *   allocating a throwaway object is a separate smell); coloring only tracks function-to-function
 *   propagation of the marker.
 * - property reads — accessors are assumed allocation-free by design; a property whose accessor
 *   does allocate must carry `@Allocates` (see deny list below).
 *
 * Deny list (always flagged, **before** every neutral exemption): callees and properties marked
 * `@Slow` or `@Allocates` ([hotPathDenyAnnotationFqns]) are explicitly anti-hot. This
 * deliberately beats the `inline` exemption — `inline` removes the call frame, not the
 * allocations of the inlined body.
 *
 * A deliberate allocation is acknowledged at the call site with `@HotAllocation("reason")` (on
 * the expression or the local variable) — the finding is suppressed and the decision stays
 * visible and greppable ([isAcknowledgedHotAllocation]).
 *
 * A **polymorphic delegation point** over a deliberately un-colored interface (e.g. a wrapper
 * layer's `delegate.paint(...)` over `Layer.paint` with its ~284 implementations) is acknowledged
 * with `@HotBoundary("reason")` — it suppresses only the plain "not itself `@Hot`" finding, never
 * the `@Slow`/`@Allocates` deny ([isAcknowledgedHotBoundary]). Every `@HotBoundary` is a greppable
 * TODO until the target interface gets colored at the end of the rollout.
 *
 * Honest limits: the annotation is contagious — marking one render entry point
 * `@Hot` pulls the marker down the entire render chain. `inline` wrappers (`fastForEach`) hide the
 * boxing callee. The whitelist is hardcoded (like [LayerConventions]); extending it is a central
 * decision requiring a new plugin release. The runtime allocation recorder remains the
 * stronger guardrail; this rule is the early, compile-time complement.
 */
class HotFunctionMustOnlyCallHotFunctions(config: Config) : Rule(
  config,
  description = "A `@Hot` method may only call `@Hot` methods (plus `inline` / stdlib / JDK neutrals) and must never touch `@Slow` / `@Allocates` code. Coloring propagates the hot-path allocation contract transitively along the call chain, like `suspend`.",
), RequiresAnalysisApi {

  override fun visitNamedFunction(function: KtNamedFunction) {
    super.visitNamedFunction(function)

    if (!function.hasHotAnnotationEntry()) return
    val body = function.bodyBlockExpression ?: function.bodyExpression ?: return

    analyze(function) {
      if (!function.symbol.isHotAnnotated()) return@analyze

      body.collectDescendantsOfType<KtCallExpression>().forEach { callExpression ->
        reportIfCalleeNotHot(callExpression)
      }
      body.collectDescendantsOfType<KtNameReferenceExpression>().forEach { reference ->
        reportIfReadsDenyMarkedProperty(reference)
      }
    }
  }

  private fun KaSession.reportIfCalleeNotHot(callExpression: KtCallExpression) {
    val call = callExpression.resolveToCall()?.successfulCallOrNull<KaFunctionCall<*>>() ?: return
    val calleeSymbol = call.symbol as? KaNamedFunctionSymbol ?: return

    val denyAnnotationShortName = calleeSymbol.findHotPathDenyAnnotationShortName()
    if (denyAnnotationShortName != null) {
      if (callExpression.isAcknowledgedHotAllocation()) return
      val calleeName = calleeSymbol.callableId?.callableName?.asString() ?: calleeSymbol.name.asString()
      report(
        Finding(
          Entity.from(callExpression.calleeExpression ?: callExpression),
          "`$calleeName(...)` is annotated `@$denyAnnotationShortName` and is called from a `@Hot` " +
            "method. `@$denyAnnotationShortName` marks explicitly anti-hot code — it must never run per " +
            "frame, and the marker beats every exemption (`inline` removes the call frame, not the " +
            "allocations). Move the call out of the hot path, fix the callee, or acknowledge a deliberate " +
            "allocation at the call site: `@HotAllocation(\"reason\")`.",
        ),
      )
      return
    }

    if (calleeSymbol.isInline) return
    if (calleeSymbol.isHotAnnotated()) return

    val calleeName = calleeSymbol.callableId?.callableName?.asString() ?: return
    val packageName = calleeSymbol.callableId?.packageName?.asString() ?: return
    if (isNeutralPackage(packageName)) return

    if (callExpression.isAcknowledgedHotBoundary()) return

    report(
      Finding(
        Entity.from(callExpression.calleeExpression ?: callExpression),
        "`$calleeName(...)` is called from a `@Hot` method but is not itself `@Hot`. The hot-path " +
          "allocation contract is contagious: annotate `$calleeName` with `@Hot` (and hold it to the same " +
          "no-boxing rule) so the guarantee propagates along the call chain. Neutral callees (`inline`, " +
          "stdlib, JDK) are exempt. A polymorphic delegation point over a deliberately un-colored " +
          "interface is acknowledged with `@HotBoundary(\"reason\")`.",
      ),
    )
  }

  /**
   * Flags reading a property marked `@Slow` / `@Allocates` (on the property or its getter)
   * from a `@Hot` body. Plain property reads are neutral — accessors are assumed allocation-free;
   * the deny marker declares the exception.
   */
  private fun KaSession.reportIfReadsDenyMarkedProperty(reference: KtNameReferenceExpression) {
    val variableAccess = reference.resolveToCall()?.successfulCallOrNull<KaVariableAccessCall>() ?: return
    val propertySymbol = variableAccess.symbol as? KaPropertySymbol ?: return

    val denyAnnotationShortName = propertySymbol.findHotPathDenyAnnotationShortName()
      ?: propertySymbol.getter?.findHotPathDenyAnnotationShortName()
      ?: return

    if (reference.isAcknowledgedHotAllocation()) return

    report(
      Finding(
        Entity.from(reference),
        "`${reference.getReferencedName()}` is a property annotated `@$denyAnnotationShortName` and is " +
          "read from a `@Hot` method. Property accessors are assumed allocation-free; " +
          "`@$denyAnnotationShortName` declares this one is not — it must never run per frame. Cache the " +
          "value outside the hot path, fix the accessor, or acknowledge a deliberate allocation at the " +
          "read site: `@HotAllocation(\"reason\")`.",
      ),
    )
  }

  private fun isNeutralPackage(packageName: String): Boolean =
    neutralPackagePrefixes.any { prefix -> packageName == prefix || packageName.startsWith("$prefix.") }

  companion object {
    /**
     * Package prefixes whose functions count as allocation-neutral leaves. Hardcoded on purpose
     * (see [LayerConventions]); extend here when a new neutral library is introduced.
     */
    private val neutralPackagePrefixes: Set<String> = setOf(
      "kotlin",
      "kotlinx",
      "java",
      "javax",
      "javafx",
      "jdk",
      "sun",
      "org.w3c",
      // Provider SAM boundary (MultiProvider.valueAt & co.): implementations are mostly lambdas,
      // which coloring cannot reach anyway — @Hot on the interfaces would cascade onto ~100
      // valueAt overrides for sham guarantees. Protection stays twofold: the @Slow/@Allocates
      // deny runs *before* this neutral check, and HotFunctionMustNotBoxValueClass still flags
      // value-class boxing at the generic valueAt boundary.
      "it.neckar.open.provider",
    )
  }
}
