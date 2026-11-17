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
 * Forbids messaging wire types from referencing Mongo-Entity classes.
 *
 * A message is a contract with whoever reads the stream. Embedding a persistence type publishes the
 * collection schema to that reader and ties the next migration to it — the same leak
 * [DataViewMustNotReferenceEntity] prevents for REST, over a different transport. Project what the
 * message needs instead: convert `Entity.toModel()` in the data layer, then build the message from
 * the Domain type.
 *
 * "Messaging" is detected via [LayerConventions.MessagingSuffixes] or a
 * [LayerConventions.MessagingPackageSegment] package; "Mongo Entity" via the
 * [LayerConventions.MongoDocumentFqn] supertype.
 */
class MessagingMustNotReferenceEntity(config: Config) : Rule(
  config,
  description = "Messaging wire types must not reference Mongo-Entity classes (subtypes of `MongoDocument`). Convert via `toModel()` in the data layer and build the message from Domain.",
), RequiresAnalysisApi {

  override fun visitTypeReference(typeReference: KtTypeReference) {
    super.visitTypeReference(typeReference)

    val hostClass = typeReference.getStrictParentOfType<KtClassOrObject>() ?: return

    analyze(typeReference) {
      val hostSymbol = hostClass.classSymbol ?: return@analyze
      if (!hostSymbol.isMessagingType()) return@analyze

      val referencedSymbol = (typeReference.type as? KaClassType)?.symbol as? KaClassSymbol ?: return@analyze
      if (referencedSymbol.isCrossLayerExempt()) return@analyze
      if (!referencedSymbol.isMongoEntity()) return@analyze

      val referencedName = referencedSymbol.classId?.shortClassName?.asString() ?: "<entity>"
      val hostName = hostClass.name ?: "<anonymous>"

      report(
        Finding(
          Entity.from(typeReference),
          "Messaging type `$hostName` references the Mongo entity `$referencedName`. " +
            "A message must not publish the persistence schema to its readers — convert via " +
            "`$referencedName.toModel()` in the data layer (`*Access`) and build `$hostName` from " +
            "the Domain type. If `$referencedName` is genuinely shared across layers, mark it " +
            "`@CrossLayer(\"…\")` instead.",
        ),
      )
    }
  }
}
