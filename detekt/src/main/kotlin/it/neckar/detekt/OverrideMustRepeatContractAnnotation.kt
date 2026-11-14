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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Forces an override to repeat every **contract annotation** its overridden declaration carries.
 *
 * Kotlin/the JVM do **not** inherit method annotations onto overrides: if an interface method is
 * annotated `@UiThread` and an implementation overrides it, the override does not carry `@UiThread`.
 * The contract silently disappears at exactly the place a reader looks — the implementation. This
 * rule makes the contract explicit on every override.
 *
 * A "contract annotation" is any annotation that is itself meta-annotated with one of
 * [contractMetaAnnotationFqns] — currently `@ThreadDescribingAnnotation` (which marks `@UiThread`,
 * `@NonUiThread`, `@Blocking`, `@NonBlocking`) and `@AllocationDescribingAnnotation` (which marks
 * `@Hot`). The set is deliberately hardcoded, like [LayerConventions]; adding a meta-annotation is
 * a central decision requiring a new plugin release.
 */
class OverrideMustRepeatContractAnnotation(config: Config) : Rule(
  config,
  description = "An override must repeat every contract annotation (e.g. `@UiThread`, `@Blocking`) of the declaration it overrides. Method annotations are not inherited, so the contract is otherwise lost on the override.",
), RequiresAnalysisApi {

  override fun visitNamedFunction(function: KtNamedFunction) {
    super.visitNamedFunction(function)

    analyze(function) {
      val symbol = function.symbol
      val overriddenSymbols = symbol.allOverriddenSymbols.toList()
      if (overriddenSymbols.isEmpty()) {
        return@analyze
      }

      val ownAnnotationFqns = symbol.annotations.mapNotNull { it.classId?.asFqNameString() }.toSet()

      overriddenSymbols
        .flatMap { it.annotations }
        .mapNotNull { it.classId }
        .distinctBy { it.asFqNameString() }
        .filter { it.asFqNameString() !in ownAnnotationFqns && isContractAnnotation(it) }
        .forEach { annotationClassId ->
          report(
            Finding(
              Entity.from(function.nameIdentifier ?: function),
              "`${function.name}` overrides a declaration annotated `@${annotationClassId.shortClassName.asString()}` " +
                "but does not repeat it. Method annotations are not inherited — repeat `@${annotationClassId.shortClassName.asString()}` " +
                "on the override so the contract stays visible where it is implemented.",
            ),
          )
        }
    }
  }

  /**
   * True when the annotation class [annotationClassId] is itself meta-annotated with one of the
   * [contractMetaAnnotationFqns] - i.e. it is a contract annotation whose meaning must be inherited.
   */
  private fun KaSession.isContractAnnotation(annotationClassId: ClassId): Boolean {
    val annotationClassSymbol = findClass(annotationClassId) ?: return false
    return annotationClassSymbol.annotations.any { it.classId?.asFqNameString() in contractMetaAnnotationFqns }
  }

  companion object {
    /**
     * Meta-annotations that mark an annotation as an inheritable contract. Hardcoded on purpose (see
     * [LayerConventions]); extend here when a new contract family is introduced.
     */
    private val contractMetaAnnotationFqns: Set<String> = setOf(
      "it.neckar.open.annotations.meta.ThreadDescribingAnnotation",
      "it.neckar.open.annotations.meta.AllocationDescribingAnnotation",
    )
  }
}
