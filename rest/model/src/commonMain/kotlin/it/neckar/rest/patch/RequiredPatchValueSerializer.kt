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
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for [RequiredPatchValue].
 *
 * This serializer handles the two states:
 * - [RequiredPatchValue.Update]: Serializes/deserializes the contained value
 * - [RequiredPatchValue.Keep]: Represented by absence of the field (handled via default value)
 *
 * Unlike [PatchValueSerializer], this serializer does NOT handle null values.
 * If null is received during deserialization, an exception is thrown.
 *
 * IMPORTANT: For [RequiredPatchValue.Keep] to work correctly, the field must have a default value:
 * ```kotlin
 * @Serializable
 * data class MyPatch(
 *   val field: RequiredPatchValue<String> = RequiredPatchValue.Keep  // <-- default value required!
 * )
 * ```
 */
class RequiredPatchValueSerializer<T : Any>(
  private val valueSerializer: KSerializer<T>,
) : KSerializer<RequiredPatchValue<T>> {

  override val descriptor: SerialDescriptor = valueSerializer.descriptor

  override fun serialize(encoder: Encoder, value: RequiredPatchValue<T>) {
    when (value) {
      is RequiredPatchValue.Update -> encoder.encodeSerializableValue(valueSerializer, value.value)
      // Keep stands for an absent field, and a serializer cannot omit one. Encoding null instead
      // writes a document whose own deserializer rejects it, which moves the error to the reader.
      is RequiredPatchValue.Keep -> throw IllegalArgumentException(
        "Cannot serialize RequiredPatchValue.Keep, which stands for an absent field. Encode with encodeDefaults = false, or mark the property @EncodeDefault(NEVER)."
      )
    }
  }

  override fun deserialize(decoder: Decoder): RequiredPatchValue<T> {
    return if (decoder.decodeNotNullMark()) {
      RequiredPatchValue.Update(decoder.decodeSerializableValue(valueSerializer))
    } else {
      decoder.decodeNull()
      // Null received for a required field - this should not happen in normal usage.
      // If the field is absent, Keep is used (via default value).
      // If null is explicitly sent, it's an error for required fields.
      throw IllegalArgumentException("Cannot deserialize null for RequiredPatchValue. Use PatchValue for nullable fields.")
    }
  }
}
