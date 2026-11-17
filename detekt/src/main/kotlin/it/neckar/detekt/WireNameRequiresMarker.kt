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
import org.jetbrains.kotlin.psi.KtClassOrObject

/**
 * Requires the marker a wire-sounding name is already promising.
 *
 * A serializable class called `…Details`, `…Response`, `…Payload`, `…Event` or `…Message` says it
 * belongs on a wire. Whether it does is decided by a marker — `@DataView`, `@Messaging`, a
 * `MongoDocument` supertype — or by a `rest` / `mongo` / `messaging` package. Where the name says
 * one thing and nothing else says anything, one of the two is wrong.
 *
 * The rule exists because the name used to decide (ADL 0179). That failed in the wrong direction: a
 * class was taken out of the layer rules because of what it was called, without anyone deciding so.
 * Turning the name from an answer into a question keeps the signal — a good name still tells the
 * reader what a class is — and puts the decision back where it belongs.
 *
 * Three ways out, all of them explicit:
 *
 * - the class carries a wire marker, or
 * - it lives in a wire package, or
 * - it is Domain after all and loses the name that says otherwise.
 *
 * Only serializable classes are considered. A class that never goes anywhere may be called
 * `MouseEvent` in peace — that is the case `it.neckar.events` makes, and it is why the transport
 * words alone were never enough to classify anything.
 */
class WireNameRequiresMarker(config: Config) : Rule(
  config,
  description = "A serializable class whose name promises a wire layer must carry the marker that confirms it — `@DataView`, `@Messaging`, a Mongo supertype, or a `rest` / `mongo` / `messaging` package.",
), RequiresAnalysisApi {

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    super.visitClassOrObject(classOrObject)

    analyze(classOrObject) {
      val symbol = classOrObject.classSymbol ?: return@analyze
      if (!symbol.isSerializableClass()) return@analyze
      if (!symbol.hasWireNameSignal()) return@analyze
      // Asks the question without the name: is anything else saying this is not Domain?
      if (!symbol.isDomainClass(considerRestNames = false)) return@analyze

      report(
        Finding(
          Entity.from(classOrObject.nameIdentifier ?: classOrObject),
          "The name of `${classOrObject.name}` promises a wire type, but nothing confirms it. Add the marker " +
            "that says which layer it belongs to (`@DataView` for REST, `@Messaging` for a message, a " +
            "`MongoDocument` supertype for persistence), move it into a `rest` / `mongo` / `messaging` package, " +
            "or rename it if it is Domain after all. Until then it counts as Domain and the layer rules apply " +
            "to it — which is what a name must not decide (ADL 0179).",
        ),
      )
    }
  }
}
