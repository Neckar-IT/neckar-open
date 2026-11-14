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
import dev.detekt.api.config
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Forbids a package from depending on another. Which packages, and on what, is configured per
 * project — the rule itself carries no product knowledge.
 *
 * Unlike a grep over import lines this resolves the referenced type, so a fully-qualified inline
 * reference is caught as well. It complements a compilation boundary rather than replacing it: a
 * separate source set without the offending library on its classpath also rejects the transitive
 * route, which no source-level rule can see. Where a boundary would mean restructuring the build for
 * every product, this rule states the same intent in one config block.
 *
 * ```yaml
 * neckar-rules:
 *   ForbiddenPackageDependency:
 *     active: true
 *     # "<consuming package prefix> -> <forbidden package prefix>"
 *     forbidden:
 *       - "it.neckar.tools.gitlab.auto.merge.model -> org.gitlab4j"
 *       - "it.neckar.tools.gitlab.auto.merge.model -> it.neckar.tools.gitlab.auto.merge.actions"
 *     # File names that may cross the boundary anyway — the mappers that translate between the two
 *     # sides. Keep this list short; every entry is a hole.
 *     exemptFiles:
 *       - "MergeRequestStateFactory.kt"
 * ```
 *
 * Both sides match by package prefix, so one entry covers a package and everything under it. The
 * consuming side is the package of the file being analysed, not of the enclosing class — a top-level
 * extension function is judged like everything else in its file.
 */
class ForbiddenPackageDependency(config: Config) : Rule(
  config,
  description = "This package must not depend on the referenced one. See the `forbidden` list in the project's detekt configuration.",
), RequiresAnalysisApi {

  /** Entries of the form `<consuming package prefix> -> <forbidden package prefix>`. */
  private val forbidden: List<String> by config(emptyList<String>())

  /** File names allowed to cross the boundary — the deliberate mapping seams. */
  private val exemptFiles: List<String> by config(emptyList<String>())

  private val rules: List<PackageRule> by lazy {
    forbidden.mapNotNull { entry ->
      val parts = entry.split(Separator)
      if (parts.size != 2) return@mapNotNull null
      PackageRule(from = parts[0].trim(), to = parts[1].trim())
    }
  }

  override fun visitImportDirective(importDirective: KtImportDirective) {
    super.visitImportDirective(importDirective)

    val imported = importDirective.importedFqName?.asString() ?: return
    reportIfForbidden(importDirective, imported)
  }

  override fun visitTypeReference(typeReference: KtTypeReference) {
    super.visitTypeReference(typeReference)

    val referencedFqName = analyze(typeReference) {
      val symbol = (typeReference.type as? KaClassType)?.symbol as? KaClassSymbol ?: return@analyze null
      symbol.classId?.asFqNameString()
    } ?: return

    reportIfForbidden(typeReference, referencedFqName)
  }

  private fun reportIfForbidden(element: KtElement, referencedFqName: String) {
    if (element.containingKtFile.name in exemptFiles) return

    val consumingPackage = element.containingKtFile.packageFqName.asString()
    val violated = rules.firstOrNull { rule ->
      consumingPackage.startsWithPackage(rule.from) && referencedFqName.startsWithPackage(rule.to)
    } ?: return

    report(
      Finding(
        Entity.from(element),
        "`$consumingPackage` must not depend on `${violated.to}` — `$referencedFqName` crosses that boundary.",
      ),
    )
  }

  /** Prefix match on package segments, so `foo.bar` matches `foo.bar.Baz` but never `foo.barbecue`. */
  private fun String.startsWithPackage(prefix: String): Boolean =
    this == prefix || startsWith("$prefix.")

  private data class PackageRule(val from: String, val to: String)

  companion object {
    private const val Separator = "->"
  }
}
