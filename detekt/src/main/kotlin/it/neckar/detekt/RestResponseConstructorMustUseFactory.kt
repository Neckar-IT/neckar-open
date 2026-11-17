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

import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Forbids creating a leaf instance of a `@Serializable sealed RestResponse` outside the sealed
 * parent's declaration file (where the companion-object factories live).
 *
 * `call.respond(x)` resolves the kotlinx-serialization serializer over the **runtime type** of `x`.
 * If the runtime type is a leaf subclass of a `@Serializable sealed` parent, the polymorphic
 * `type` discriminator is omitted from the response body, and the frontend `switch (response.type)`
 * silently falls through. The fix is to ensure the runtime type is always the sealed parent itself,
 * which requires a factory whose declared return type is the sealed parent.
 *
 * This rule attacks the bug at the **construction site**: a leaf-typed instance must never come into
 * existence outside the sealed parent's file. Companion-object factories (`fun good(...): MyResponse =
 * Good(...)`) and top-level helpers in the same file are the only legitimate places to construct a
 * leaf — they upcast to the sealed parent on return, so no leaf-typed value ever reaches a caller.
 *
 * Triggered by:
 * - constructor calls (`MyResponse.Good(data)`) of a leaf subclass of a `@Serializable sealed RestResponse`,
 * - simple-name references (`MyResponse.AlreadyArchived`) to a `data object` / `object` of such a leaf type.
 *
 * Whitelisted (no finding):
 * - the call site is inside the same `KtFile` as the sealed parent class — covers companion-object
 *   factories and top-level helpers next to the response declaration,
 * - the reference is inside a `KtTypeReference` (a type position, not a value position),
 * - the reference appears as a `when`-condition pattern (`when (x) { MyResponse.AlreadyArchived -> … }`) —
 *   this is an equality check against the singleton, not a value-creation site that flows into `respond`,
 * - test sources excluded via `excludes` in `internal/buildplatform/config/detekt/detekt.yml`.
 *
 * Companion: [RespondWithSealedSubclass] — catches the same bug at the `call.respond` argument site,
 * for cases where a leaf-typed value reaches a `respond` call despite this rule (suppressed
 * construction, or a factory with the wrong return type).
 *
 * See `internal/patterns/docs/kotlin/rest-response.md`.
 */
class RestResponseConstructorMustUseFactory(config: Config) : Rule(
  config,
  description = "Construct subclasses of a `@Serializable sealed RestResponse` only inside the sealed parent's file (companion factories) — direct construction at a call site causes `call.respond` to drop the polymorphic discriminator.",
), RequiresAnalysisApi {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    analyze(expression) {
      val call = expression.resolveToCall()?.successfulCallOrNull<KaFunctionCall<*>>() ?: return@analyze
      val constructorSymbol = call.symbol as? KaConstructorSymbol ?: return@analyze
      val constructedType = constructorSymbol.returnType
      val sealedParent = findSealedRestResponseParent(constructedType) ?: return@analyze
      if (isInsideSealedParentFile(expression, sealedParent)) return@analyze

      val leafName = constructedType.classIdFqString()?.substringAfterLast('.') ?: "subclass"
      val parentName = sealedParent.shortName()

      report(
        Finding(
          Entity.from(expression.calleeExpression ?: expression),
          buildMessage(leafName, parentName),
        ),
      )
    }
  }

  override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
    super.visitSimpleNameExpression(expression)
    if (expression !is KtNameReferenceExpression) return
    if (expression.isCalleeOfCallExpression()) return
    if (expression.isReceiverOfDotQualifiedExpression()) return
    if (expression.getStrictParentOfType<KtTypeReference>() != null) return
    if (expression.isInsideWhenCondition()) return

    analyze(expression) {
      val type = expression.expressionType ?: return@analyze
      val classSymbol = (type as? KaClassType)?.symbol as? KaClassSymbol ?: return@analyze
      if (classSymbol.classKind != KaClassKind.OBJECT) return@analyze

      val sealedParent = findSealedRestResponseParent(type) ?: return@analyze
      if (isInsideSealedParentFile(expression, sealedParent)) return@analyze

      val leafName = classSymbol.shortName()
      val parentName = sealedParent.shortName()

      report(
        Finding(
          Entity.from(expression),
          buildMessage(leafName, parentName),
        ),
      )
    }
  }

  /**
   * True when [element] lives in the same `KtFile` as the sealed parent's class declaration.
   * The companion object and top-level helper functions of a response sit in the same file as the
   * sealed `interface` / `class` itself, so this exempts every legitimate construction site at once.
   */
  private fun isInsideSealedParentFile(element: KtElement, sealedParent: KaClassSymbol): Boolean {
    val parentPsi = sealedParent.psi as? KtClassOrObject ?: return false
    val parentFile = parentPsi.containingKtFile
    val callSiteFile = element.containingFile as? KtFile ?: return false
    return parentFile == callSiteFile
  }

  private fun KtNameReferenceExpression.isCalleeOfCallExpression(): Boolean {
    val parentCall = parent as? KtCallExpression ?: return false
    return parentCall.calleeExpression === this
  }

  private fun KtNameReferenceExpression.isReceiverOfDotQualifiedExpression(): Boolean {
    val parent = parent as? KtDotQualifiedExpression ?: return false
    return parent.receiverExpression === this
  }

  /**
   * True when this reference (or the dot-qualified expression it is the selector of) sits directly
   * under a `when`-condition expression — i.e. it is matched against the `when` subject, not used
   * as a value source. `when (x) { MyResponse.AlreadyArchived -> … }` compiles to an equality
   * check against the singleton and never lets a leaf-typed value flow to `respond`.
   */
  private fun KtNameReferenceExpression.isInsideWhenCondition(): Boolean {
    var current: PsiElement = this
    while (true) {
      val parent = current.parent
      if (parent is KtDotQualifiedExpression && parent.selectorExpression === current) {
        current = parent
      } else {
        break
      }
    }
    return current.parent is KtWhenConditionWithExpression
  }

  private fun buildMessage(leafName: String, parentName: String): String {
    return "`$leafName` is a leaf subclass of the `@Serializable sealed` parent `$parentName`. " +
      "Construct it only inside `$parentName`'s file — typically via a companion factory like " +
      "`fun ${leafName.replaceFirstChar { it.lowercase() }}(...): $parentName = $leafName(...)`. " +
      "Direct construction here causes `call.respond` to serialize using the runtime leaf type and " +
      "drop the polymorphic discriminator. " +
      "See `internal/patterns/docs/kotlin/rest-response.md`."
  }
}
