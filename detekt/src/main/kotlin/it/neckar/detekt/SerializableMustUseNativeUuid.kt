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
 * Forbids raw `kotlin.uuid.Uuid` as a property type in `@kotlinx.serialization.Serializable`
 * classes — these classes must use `NativeUuid` (`it.neckar.uuid.NativeUuid`) instead.
 *
 * Why: serializing raw `Uuid` produces a string. In BSON this stores the value as a string
 * instead of `BinData(UUID)`, which inflates index size and prevents native UUID query
 * operators. On the JSON wire it strips the type from the API contract — a frontend cannot
 * tell a UUID string from any other string.
 *
 * Scope: every class annotated `@Serializable`. Domain classes are excluded by construction —
 * [DomainMustNotBeSerializable] rejects `@Serializable` on Domain classes.
 */
class SerializableMustUseNativeUuid(config: Config) : Rule(
  config,
  description = "`@Serializable` classes must use `NativeUuid` instead of `kotlin.uuid.Uuid`. Raw `Uuid` serializes to a string in BSON and strips the type from the JSON contract.",
), RequiresAnalysisApi {

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    super.visitClassOrObject(classOrObject)

    analyze(classOrObject) {
      val classSymbol = classOrObject.classSymbol ?: return@analyze
      if (!classSymbol.isSerializableClass()) return@analyze

      classOrObject.body?.properties?.forEach { property ->
        property.flagIfRawUuid()
      }
      classOrObject.primaryConstructorParameters.forEach { parameter ->
        val type = parameter.typeReference ?: return@forEach
        analyze(type) {
          val resolvedType = type.type
          if (typeIsUuid(resolvedType)) {
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

  private fun KtProperty.flagIfRawUuid() {
    val typeReference = typeReference ?: return
    analyze(typeReference) {
      val resolvedType = typeReference.type
      if (typeIsUuid(resolvedType)) {
        report(
          Finding(
            Entity.from(nameIdentifier ?: this@flagIfRawUuid),
            buildMessage(name ?: "<anonymous>"),
          ),
        )
      }
    }
  }

  private fun buildMessage(propertyName: String): String =
    "Property `$propertyName` uses raw `Uuid`. `@Serializable` classes must use `NativeUuid` " +
      "(`it.neckar.uuid.NativeUuid`) — raw `Uuid` serializes to a string in BSON and strips the " +
      "UUID type from the JSON wire contract."
}
