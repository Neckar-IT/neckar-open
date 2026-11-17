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
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Forbids Domain classes from referencing REST/DataView classes.
 *
 * Domain code lives in-memory and never touches the wire. The wire contract (REST DTOs, view
 * projections) belongs in `<domain>/rest/messages/` or `<domain>/rest/views/`. Conversion runs
 * via `Book.toDetails()` / `Book.toPreview()` extension functions in the REST layer.
 *
 * "REST/DataView class" is detected via the `@DataView` annotation or one of the canonical
 * suffixes (`Request`, `Response`, `DataView`, `Details`, `Preview`, `TableEntry`).
 */
class DomainMustNotReferenceDataView(config: Config) : Rule(
  config,
  description = "Domain classes must not reference REST/DataView classes. Convert via `toDetails()` / `toPreview()` / `toTableEntry()` extensions in the REST layer.",
), RequiresAnalysisApi {

  override fun visitTypeReference(typeReference: KtTypeReference) {
    super.visitTypeReference(typeReference)

    val hostClass = typeReference.getStrictParentOfType<KtClassOrObject>() ?: return

    analyze(typeReference) {
      val hostSymbol = hostClass.classSymbol ?: return@analyze
      if (!hostSymbol.isDomainClass()) return@analyze

      val referencedSymbol = (typeReference.type as? KaClassType)?.symbol as? KaClassSymbol ?: return@analyze
      if (referencedSymbol.isCrossLayerExempt()) return@analyze
      if (!referencedSymbol.isRestDataView()) return@analyze

      val referencedName = referencedSymbol.classId?.shortClassName?.asString() ?: "<dataview>"
      val hostName = hostClass.name ?: "<anonymous>"

      report(
        Finding(
          Entity.from(typeReference),
          "Domain class `$hostName` references the REST/DataView class `$referencedName`. " +
            "Domain code must not depend on wire types — convert via `$hostName.toDetails()` / " +
            "`$hostName.toPreview()` / `$hostName.toTableEntry()` extension functions in the " +
            "REST layer. If `$referencedName` is genuinely shared across layers, mark it " +
            "`@CrossLayer(\"…\")` instead.",
        ),
      )
    }
  }
}
