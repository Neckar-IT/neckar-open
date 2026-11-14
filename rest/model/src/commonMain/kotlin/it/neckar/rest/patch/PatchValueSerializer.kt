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
package it.neckar.rest.patch

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for [PatchValue].
 *
 * This serializer handles the three states:
 * - [PatchValue.Update]: Serializes/deserializes the contained value
 * - [PatchValue.Keep]: Represented by absence of the field (handled via default value)
 * - [PatchValue.Delete]: Serializes/deserializes as JSON null
 *
 * IMPORTANT: For [PatchValue.Keep] to work correctly, the field must have a default value of [PatchValue.Keep]:
 * ```kotlin
 * @Serializable
 * data class MyPatch(
 *   val field: PatchValue<String> = Keep  // <-- default value required!
 * )
 * ```
 */
class PatchValueSerializer<T : Any>(
  private val valueSerializer: KSerializer<T>,
) : KSerializer<PatchValue<T>> {

  /**
   * The descriptor is nullable because [PatchValue.Delete] serializes as `null`.
   */
  override val descriptor: SerialDescriptor = valueSerializer.nullable.descriptor

  override fun serialize(encoder: Encoder, value: PatchValue<T>) {
    when (value) {
      is PatchValue.Update -> encoder.encodeSerializableValue(valueSerializer, value.value)
      is PatchValue.Delete -> encoder.encodeNull()
      // Keep stands for an absent field, and a serializer cannot omit one. Encoding null instead
      // would be read back as Delete, so a configuration that encodes defaults clears the field it
      // meant to leave alone.
      is PatchValue.Keep -> throw IllegalArgumentException(
        "Cannot serialize PatchValue.Keep, which stands for an absent field. Encode with encodeDefaults = false, or mark the property @EncodeDefault(NEVER)."
      )
    }
  }

  override fun deserialize(decoder: Decoder): PatchValue<T> {
    // Try to decode a nullable value using decodeNullableSerializableValue
    return if (decoder.decodeNotNullMark()) {
      PatchValue.Update(decoder.decodeSerializableValue(valueSerializer))
    } else {
      decoder.decodeNull()
      PatchValue.Delete
    }
  }
}
