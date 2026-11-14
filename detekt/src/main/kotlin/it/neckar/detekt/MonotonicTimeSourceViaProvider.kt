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
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Flags every reference to `kotlin.time.TimeSource.Monotonic` outside the file that declares
 * `it.neckar.open.time.monotonicTimeSource`.
 *
 * PSI-based, so it fires without type resolution. Reported are
 * - `TimeSource.Monotonic` and `kotlin.time.TimeSource.Monotonic`, also behind an import alias of `TimeSource`,
 * - `import kotlin.time.TimeSource.Monotonic`, whose path is a qualified expression of its own.
 *
 * The bare name `TimeSource` counts as `kotlin.time.TimeSource` unless an import binds it to another class,
 * so a `TimeSource` class of the file's own package is reported as well.
 */
class MonotonicTimeSourceViaProvider(config: Config) : Rule(
  config,
  description = "A measured duration reads `it.neckar.open.time.monotonicTimeSource.markNow()`, which a test replaces by a `VirtualNowProvider` — `TimeSource.Monotonic` keeps running on real time.",
) {

  override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
    super.visitDotQualifiedExpression(expression)

    val selector = expression.selectorExpression as? KtNameReferenceExpression ?: return
    if (selector.getReferencedName() != MonotonicName) return

    if (expression.receiverExpression.referencesKotlinTimeSource().not()) return

    reportUnlessDeclaringFile(expression)
  }

  private fun reportUnlessDeclaringFile(element: KtDotQualifiedExpression) {
    if (element.containingKtFile.declaresMonotonicTimeSource()) return

    report(
      Finding(
        Entity.from(element),
        "`TimeSource.Monotonic` keeps running on real time in a test. Use `monotonicTimeSource.markNow()` from `it.neckar.open.time`.",
      ),
    )
  }

  /**
   * Whether this receiver is `kotlin.time.TimeSource`: a simple name through the imports of the file, a qualified
   * name by all of its segments.
   */
  private fun KtExpression.referencesKotlinTimeSource(): Boolean = when (this) {
    is KtNameReferenceExpression -> getReferencedName() in containingKtFile.timeSourceNames()
    is KtDotQualifiedExpression -> qualifiedName() == TimeSourceFqName
    else -> false
  }

  /**
   * `kotlin.time.TimeSource` for the expression `kotlin.time.TimeSource`, or `null` when a segment is no plain name.
   */
  private fun KtExpression.qualifiedName(): String? = when (this) {
    is KtNameReferenceExpression -> getReferencedName()
    is KtDotQualifiedExpression -> {
      val receiver = receiverExpression.qualifiedName()
      val selector = (selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
      if (receiver == null || selector == null) null else "$receiver.$selector"
    }
    else -> null
  }

  /**
   * The names under which `kotlin.time.TimeSource` is visible in this file: every import alias, and its simple
   * name unless an import binds that name to another class.
   */
  private fun KtFile.timeSourceNames(): Set<String> {
    val aliases = importDirectives
      .filter { it.importedFqName?.asString() == TimeSourceFqName }
      .mapNotNull { it.aliasName }
      .toSet()
    val simpleNameTaken = importDirectives.any {
      it.importedName?.asString() == TimeSourceName && it.importedFqName?.asString() != TimeSourceFqName
    }
    return if (simpleNameTaken) aliases else aliases + TimeSourceName
  }

  private fun KtFile.declaresMonotonicTimeSource(): Boolean {
    if (packageFqName.asString() != DeclaringPackage) return false
    return declarations.any { it is KtProperty && it.name == DeclaredProperty }
  }

  companion object {
    private const val TimeSourceName = "TimeSource"
    private const val TimeSourceFqName = "kotlin.time.TimeSource"
    private const val MonotonicName = "Monotonic"
    private const val DeclaringPackage = "it.neckar.open.time"
    private const val DeclaredProperty = "monotonicTimeSource"
  }
}
