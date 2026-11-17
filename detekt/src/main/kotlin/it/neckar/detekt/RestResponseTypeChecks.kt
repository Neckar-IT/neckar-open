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

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType

/**
 * Shared type-resolution helpers for the two `RestResponse` discriminator-protection rules
 * ([RestResponseConstructorMustUseFactory], [RespondWithSealedSubclass]).
 *
 * Both rules answer the same central question: is a given Kotlin type a *leaf subclass* of a
 * `@Serializable sealed` parent that itself extends `it.neckar.rest.RestResponse`? Answering it
 * needs resolved supertypes and class symbols, so all functions here must run inside an
 * `analyze { … }` block.
 */

internal const val RestResponseFqn: String = "it.neckar.rest.RestResponse"

/**
 * Returns the `@Serializable sealed` `RestResponse` parent of [type] when [type] is a concrete
 * leaf subclass of one. Returns `null` for anything else — types that are themselves the sealed
 * parent, types whose direct supertypes are not sealed/`@Serializable`/`RestResponse`-rooted,
 * generic / type-parameter types.
 *
 * Only direct supertypes of [type] are considered for the "sealed parent" role. The transitive
 * walk happens *upwards from the candidate parent*, not from [type], because the sealed
 * polymorphism boundary is one hop away by construction.
 *
 * Must be called inside a `KaSession` (i.e. inside `analyze(element) { … }`).
 */
internal fun KaSession.findSealedRestResponseParent(type: KaType): KaClassSymbol? {
  val classSymbol = (type as? KaClassType)?.symbol as? KaClassSymbol ?: return null
  if (classSymbol.modality == KaSymbolModality.SEALED) return null

  return classSymbol.superTypes.firstNotNullOfOrNull { superType ->
    val superSymbol = (superType as? KaClassType)?.symbol as? KaClassSymbol
      ?: return@firstNotNullOfOrNull null
    if (superSymbol.modality != KaSymbolModality.SEALED) return@firstNotNullOfOrNull null
    if (!superSymbol.isAnnotatedSerializable()) return@firstNotNullOfOrNull null
    if (!transitivelyExtends(superType, RestResponseFqn)) return@firstNotNullOfOrNull null
    superSymbol
  }
}

private fun KaClassSymbol.isAnnotatedSerializable(): Boolean {
  return annotations.any { it.classId?.asFqNameString() == LayerConventions.SerializableFqn }
}

private fun KaSession.transitivelyExtends(type: KaType, expectedFqn: String): Boolean {
  if (type.classIdFqString() == expectedFqn) return true
  return type.allSupertypes.any { it.classIdFqString() == expectedFqn }
}

internal fun KaType.classIdFqString(): String? = (this as? KaClassType)?.classId?.asFqNameString()

internal fun KaClassSymbol.shortName(): String = classId?.shortClassName?.asString() ?: "<anonymous>"
