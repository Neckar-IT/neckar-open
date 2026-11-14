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
 * Forbids `@kotlinx.serialization.Serializable` on Domain classes.
 *
 * Domain classes live in-memory and represent business state. Serialization always crosses to a
 * counterpart layer — `*Entity` for persistence, REST/DataView for the wire, an event payload for
 * messaging, a cache entry for Redis, and so on. A Domain class that carries `@Serializable` is
 * either:
 *
 * - misclassified (it is actually an Entity / DataView / event payload that belongs in `.mongo`,
 *   `.rest`, or its own package), or
 * - leaking wire concerns into business logic.
 *
 * This rule catches both at build time and forces the architectural decision into the open. It
 * also unlocks the simpler form of [SerializableMustUseNativeInstant] / [SerializableMustUseNativeUuid]:
 * once Domain is never `@Serializable`, those rules can apply to all `@Serializable` classes
 * without accidentally reaching into Domain.
 *
 * "Domain class" is identified via [LayerConventions.isDomainClass]: a class under `it.neckar.`
 * that is not Entity, not DataView, not messaging (`*Event` / `*Payload` or a `messaging`
 * package), not infrastructure (`*Support` / `*Access` / `*Client` / …), not value class, not
 * enum, not `*Like` marker, not `@CrossLayer`-annotated.
 */
class DomainMustNotBeSerializable(config: Config) : Rule(
  config,
  description = "Domain classes must not carry `@Serializable`. Domain lives in-memory; serialization always crosses to Entity, REST, event, or cache counterparts.",
), RequiresAnalysisApi {

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    super.visitClassOrObject(classOrObject)

    analyze(classOrObject) {
      val symbol = classOrObject.classSymbol ?: return@analyze
      if (!symbol.isSerializableClass()) return@analyze
      if (!symbol.isDomainClass()) return@analyze

      report(
        Finding(
          Entity.from(classOrObject.nameIdentifier ?: classOrObject),
          "Domain class `${classOrObject.name}` carries `@Serializable`. Domain classes are never serialized — " +
            "introduce a counterpart layer (`*Entity` for Mongo, `*Details`/`*Preview`/`*Request` for REST, " +
            "`*Event`/`*Payload` for messaging) and convert via `toModel()` / `toEntity()` / `toDetails()`. " +
            "If the type is genuinely shared across layers, mark it `@CrossLayer(\"…\")` instead.",
        ),
      )
    }
  }
}
