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
 * Forbids messaging wire types from embedding Domain classes as property types.
 *
 * A Domain type carries in-memory state, rich logic and unbounded collections. Putting one into a
 * message makes business internals the stream's contract: every reader is coupled to it, and the
 * next domain refactoring is a breaking wire change. Messages carry their own fields or a REST view
 * — a projection is the point, and referencing a `*Details` view stays allowed.
 *
 * Domain values that are legitimately shared — value classes, enums, `*Like` interfaces,
 * `@CrossLayer` types — are exempt, so an id or a status enum goes into a message unchanged.
 *
 * The REST counterpart is [DataViewMustNotReferenceDomain]; this rule is the same boundary over a
 * different transport.
 */
class MessagingMustNotReferenceDomain(config: Config) : Rule(
  config,
  description = "Messaging wire types must not embed Domain classes. Carry the fields the message needs, or a REST view.",
), RequiresAnalysisApi {

  override fun visitTypeReference(typeReference: KtTypeReference) {
    super.visitTypeReference(typeReference)

    val hostClass = typeReference.getStrictParentOfType<KtClassOrObject>() ?: return

    analyze(typeReference) {
      val hostSymbol = hostClass.classSymbol ?: return@analyze
      if (!hostSymbol.isMessagingType()) return@analyze
      if (hostSymbol.isCrossLayerExempt()) return@analyze

      val referencedSymbol = (typeReference.type as? KaClassType)?.symbol as? KaClassSymbol ?: return@analyze
      if (referencedSymbol.isCrossLayerExempt()) return@analyze
      if (!referencedSymbol.isDomainClass()) return@analyze

      val referencedName = referencedSymbol.classId?.shortClassName?.asString() ?: "<domain>"
      val hostName = hostClass.name ?: "<anonymous>"

      report(
        Finding(
          Entity.from(typeReference),
          "Messaging type `$hostName` embeds the Domain class `$referencedName`. " +
            "Business internals must not become the stream's contract — carry the fields " +
            "`$hostName` actually needs, or project a view (`fun $referencedName.toDetails()`). " +
            "If `$referencedName` is genuinely shared across layers, mark it `@CrossLayer(\"…\")` " +
            "instead.",
        ),
      )
    }
  }
}
