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
 * Forbids Domain classes from referencing Mongo-Entity classes.
 *
 * Domain code lives in-memory and depends only on other Domain types, value classes, enums, and
 * cross-layer-allowed types. Persistence schema must not leak into business logic — conversion
 * is performed in `*Access` data layers via `BookEntity.toModel()` / `Book.toEntity()` extension
 * functions.
 *
 * "Mongo Entity" is detected via the [LayerConventions.MongoDocumentFqn] supertype — every
 * subtype (`MongoEntity`, `MongoUuidDocument`, `EmbeddedMongoDocument`, …) is in scope.
 *
 * The rule visits every type reference inside a Domain class body and checks whether the
 * referenced type implements `MongoDocument`. Generic type arguments are visited recursively by
 * the PSI walker (each type argument is a separate `KtTypeReference`).
 */
class DomainMustNotReferenceEntity(config: Config) : Rule(
  config,
  description = "Domain classes must not reference Mongo-Entity classes (subtypes of `MongoDocument`). Convert via `toModel()` / `toEntity()` extensions in the data layer.",
), RequiresAnalysisApi {

  override fun visitTypeReference(typeReference: KtTypeReference) {
    super.visitTypeReference(typeReference)

    val hostClass = typeReference.getStrictParentOfType<KtClassOrObject>() ?: return

    analyze(typeReference) {
      val hostSymbol = hostClass.classSymbol ?: return@analyze
      if (!hostSymbol.isDomainClass()) return@analyze

      val referencedSymbol = (typeReference.type as? KaClassType)?.symbol as? KaClassSymbol ?: return@analyze
      if (referencedSymbol.isCrossLayerExempt()) return@analyze
      if (!referencedSymbol.isMongoEntity()) return@analyze

      val referencedName = referencedSymbol.classId?.shortClassName?.asString() ?: "<entity>"
      val hostName = hostClass.name ?: "<anonymous>"

      report(
        Finding(
          Entity.from(typeReference),
          "Domain class `$hostName` references the Mongo entity `$referencedName`. " +
            "Domain code must not depend on persistence types — convert via " +
            "`$referencedName.toModel()` / `${hostName}.toEntity()` extension functions in the " +
            "data layer (`*Access`). If `$referencedName` is genuinely shared across layers, " +
            "mark it `@CrossLayer(\"…\")` instead.",
        ),
      )
    }
  }
}
