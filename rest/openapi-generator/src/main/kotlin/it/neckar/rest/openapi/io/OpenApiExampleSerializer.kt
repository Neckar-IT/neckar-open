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
package it.neckar.rest.openapi.io

import it.neckar.rest.openapi.dsl.OpenApiExample
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * Serializes [OpenApiExample] to JSON
 */
@Deprecated("Not required")
object OpenApiExampleSerializer : KSerializer<OpenApiExample> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("OpenApiExampleSerializer") {
    element<String?>("description")
    element<String?>("summary")
    element<String>("className")
    element<JsonElement>("content")
  }

  override fun serialize(encoder: Encoder, value: OpenApiExample) {
    encoder.encodeStructure(descriptor) {
      value.description?.let { encodeStringElement(descriptor, 0, it) }
      value.summary?.let { encodeStringElement(descriptor, 1, it) }

      val qualifiedName = value.example::class.java.name
      encodeStringElement(descriptor, 2, qualifiedName)

      val serializer = try {
        serializer(value.example::class.java)
      } catch (e: SerializationException) {
        throw IllegalStateException("Please annotate [$qualifiedName] with @Serializable", e)
      }

      encodeSerializableElement(descriptor, 3, serializer, value.example)
    }
  }

  override fun deserialize(decoder: Decoder): OpenApiExample {
    throw UnsupportedOperationException("Not implemented")
  }
}
