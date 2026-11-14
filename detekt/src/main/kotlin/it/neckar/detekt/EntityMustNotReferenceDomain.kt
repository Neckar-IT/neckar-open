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
 * Forbids Mongo-Entity classes from referencing Domain classes inside their declaration.
 *
 * Persistence and business types are kept separate. Conversion lives in **top-level extension
 * functions** named `toModel` / `toEntity`, declared outside both class bodies — those are
 * naturally exempt because their type references have no enclosing class. Member methods named
 * `toModel` (defined inside the entity class) are flagged: extensions are the convention, not
 * member methods.
 *
 * "Mongo Entity" is detected via the [LayerConventions.MongoDocumentFqn] supertype.
 */
class EntityMustNotReferenceDomain(config: Config) : Rule(
  config,
  description = "Mongo-Entity classes must not reference Domain classes inside the class body. Conversion belongs in top-level `toModel` / `toEntity` extension functions.",
), RequiresAnalysisApi {

  override fun visitTypeReference(typeReference: KtTypeReference) {
    super.visitTypeReference(typeReference)

    val hostClass = typeReference.getStrictParentOfType<KtClassOrObject>() ?: return

    analyze(typeReference) {
      val hostSymbol = hostClass.classSymbol ?: return@analyze
      if (!hostSymbol.isMongoEntity()) return@analyze
      if (hostSymbol.isCrossLayerExempt()) return@analyze

      val referencedSymbol = (typeReference.type as? KaClassType)?.symbol as? KaClassSymbol ?: return@analyze
      if (referencedSymbol.isCrossLayerExempt()) return@analyze
      if (!referencedSymbol.isDomainClass()) return@analyze

      val referencedName = referencedSymbol.classId?.shortClassName?.asString() ?: "<domain>"
      val hostName = hostClass.name ?: "<anonymous>"

      report(
        Finding(
          Entity.from(typeReference),
          "Mongo entity `$hostName` references the Domain class `$referencedName`. " +
            "Move the conversion to a top-level extension function `fun $hostName.toModel(): $referencedName` " +
            "(or `fun $referencedName.toEntity(): $hostName`) outside both class bodies. If " +
            "`$referencedName` is genuinely shared across layers, mark it `@CrossLayer(\"…\")`.",
        ),
      )
    }
  }
}
