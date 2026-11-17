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
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Forbids raw `kotlin.time.Instant` and `kotlinx.datetime.Instant` as property types in
 * `@kotlinx.serialization.Serializable` classes — these classes must use `NativeInstant`
 * (`it.neckar.open.kotlin.serializers.NativeInstant`) instead.
 *
 * Why: serializing raw `Instant` produces an ISO-8601 string. In BSON this means the database
 * stores ISO strings instead of native `DATE_TIME`, which breaks range queries, chronological
 * sorting, and migration assumptions. On the JSON wire it forces every frontend to parse strings
 * by hand instead of receiving a typed value. `NativeInstant` is a typealias with a custom
 * serializer that emits native types in BSON and a typed wrapper in JSON.
 *
 * Scope: every class annotated `@Serializable`. Domain classes are excluded by construction —
 * [DomainMustNotBeSerializable] rejects `@Serializable` on Domain classes, so they never use
 * raw `Instant` in a serialized context.
 */
class SerializableMustUseNativeInstant(config: Config) : Rule(
  config,
  description = "`@Serializable` classes must use `NativeInstant` instead of `kotlin.time.Instant` or `kotlinx.datetime.Instant`. Raw `Instant` serializes to an ISO string in BSON and forces frontend parsing on the JSON wire.",
), RequiresAnalysisApi {

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    super.visitClassOrObject(classOrObject)

    analyze(classOrObject) {
      val classSymbol = classOrObject.classSymbol ?: return@analyze
      if (!classSymbol.isSerializableClass()) return@analyze

      classOrObject.body?.properties?.forEach { property ->
        property.flagIfRawInstant()
      }
      classOrObject.primaryConstructorParameters.forEach { parameter ->
        val type = parameter.typeReference ?: return@forEach
        analyze(type) {
          val resolvedType = type.type
          if (typeIsInstant(resolvedType)) {
            report(
              Finding(
                Entity.from(parameter.nameIdentifier ?: parameter),
                buildMessage(parameter.name ?: "<anonymous>"),
              ),
            )
          }
        }
      }
    }
  }

  private fun KtProperty.flagIfRawInstant() {
    val typeReference = typeReference ?: return
    analyze(typeReference) {
      val resolvedType = typeReference.type
      if (typeIsInstant(resolvedType)) {
        report(
          Finding(
            Entity.from(nameIdentifier ?: this@flagIfRawInstant),
            buildMessage(name ?: "<anonymous>"),
          ),
        )
      }
    }
  }

  private fun buildMessage(propertyName: String): String =
    "Property `$propertyName` uses raw `Instant`. `@Serializable` classes must use `NativeInstant` " +
      "(`it.neckar.open.kotlin.serializers.NativeInstant`) — raw `Instant` serializes to an ISO string " +
      "instead of a native BSON `DATE_TIME` and forces every frontend to parse strings by hand."
}
