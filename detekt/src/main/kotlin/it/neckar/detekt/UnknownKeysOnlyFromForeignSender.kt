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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Reports a decoder that drops unknown keys — `strictMode = false` (kaml) or `ignoreUnknownKeys = true`
 * (kotlinx.serialization JSON) — unless the format comes from a sender outside this repository.
 *
 * A format this repository defines parses strictly: a misspelt key fails with file and key instead of vanishing.
 * Two ways out, both explicit:
 *
 * - the setting or an enclosing declaration carries `@ForeignFormat(sender = "…")` with a non-blank sender, or
 * - the enclosing declaration is on [OwnFormatAllowlist], which names why the format of this repository stays
 *   lenient there.
 *
 * PSI-based, so it fires without type resolution. Reported are a named argument `strictMode = false` or
 * `ignoreUnknownKeys = true` and an assignment `ignoreUnknownKeys = true`, as in `Json { ignoreUnknownKeys = true }`.
 */
class UnknownKeysOnlyFromForeignSender(config: Config) : Rule(
  config,
  description = "A decoder dropping unknown keys names the foreign sender of its format with `@ForeignFormat`; a format of this repository parses strictly.",
) {

  override fun visitArgument(argument: KtValueArgument) {
    super.visitArgument(argument)

    val name = argument.getArgumentName()?.asName?.asString() ?: return
    val lenientValue = LenientSettings.firstOrNull { it.name == name } ?: return
    if (argument.getArgumentExpression().isBooleanLiteral(lenientValue.value).not()) return

    reportUnlessExempt(argument, lenientValue)
  }

  override fun visitBinaryExpression(expression: KtBinaryExpression) {
    super.visitBinaryExpression(expression)

    if (expression.operationToken != KtTokens.EQ) return
    val name = (expression.left as? KtNameReferenceExpression)?.getReferencedName() ?: return
    val lenientValue = LenientSettings.firstOrNull { it.name == name } ?: return
    if (expression.right.isBooleanLiteral(lenientValue.value).not()) return

    reportUnlessExempt(expression, lenientValue)
  }

  private fun reportUnlessExempt(element: KtElement, setting: LenientSetting) {
    if (element.parents.filterIsInstance<KtAnnotated>().any { it.namesForeignSender() }) return
    if (element.parents.filterIsInstance<KtNamedDeclaration>().any { it.fqName?.asString() in AllowlistedDeclarations }) return

    report(
      Finding(
        Entity.from(element),
        "`${setting.name} = ${setting.value}` drops unknown keys, so a misspelt key vanishes. Parse a format of this " +
          "repository strictly; for a format from outside, name its sender with `@ForeignFormat(sender = \"…\")` " +
          "from `it.neckar.open.annotations` on the declaration.",
      ),
    )
  }

  private fun KtAnnotated.namesForeignSender(): Boolean = annotationEntries.any { it.isForeignFormatWithSender() }

  private fun KtAnnotationEntry.isForeignFormatWithSender(): Boolean {
    if (shortName?.asString() != ForeignFormatName) return false
    val sender = valueArguments.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression ?: return false
    return sender.entries.joinToString(separator = "") { it.text }.isNotBlank()
  }

  private fun KtExpression?.isBooleanLiteral(value: Boolean): Boolean = this is KtConstantExpression && text == value.toString()

  /** A decoder setting that drops unknown keys when it holds [value]. */
  private data class LenientSetting(val name: String, val value: Boolean)

  /**
   * A declaration whose format this repository defines and that still drops unknown keys, and why.
   */
  data class LenientOwnFormat(val declaration: String, val reason: String)

  companion object {
    private const val ForeignFormatName: String = "ForeignFormat"

    private val LenientSettings: List<LenientSetting> = listOf(
      LenientSetting(name = "strictMode", value = false),
      LenientSetting(name = "ignoreUnknownKeys", value = true),
    )

    /**
     * Every own format that drops unknown keys. An entry names a fully qualified declaration; the exemption covers
     * everything inside it.
     */
    val OwnFormatAllowlist: List<LenientOwnFormat> = listOf(
      LenientOwnFormat(
        declaration = "it.neckar.softwarefactory.spec.parser.SpecStrictness.Tolerant",
        reason = "The viewer renders a spec carrying an unknown key; `specLint` reads `Strict`.",
      ),
      LenientOwnFormat(
        declaration = "it.neckar.tools.review.store.ReviewStoreFormat",
        reason = "Review threads are stored documents an older CLI reads after a newer one added a field.",
      ),
      LenientOwnFormat(
        declaration = "it.neckar.deployments.log.DeliveryLogJson",
        reason = "Delivery-log events are stored payloads the service reads after a newer writer added a field.",
      ),
      LenientOwnFormat(
        declaration = "it.neckar.open.container.ContainerFormat",
        reason = "meta/type is a stored document an older reader opens after a newer minor added a field.",
      ),
      LenientOwnFormat(
        declaration = "it.neckar.worktreehud.state.stateJson",
        reason = "A worktree-hud state file is read by every version of its major; a newer minor adds a field.",
      ),
    )

    private val AllowlistedDeclarations: Set<String> = OwnFormatAllowlist.map { it.declaration }.toSet()
  }
}
