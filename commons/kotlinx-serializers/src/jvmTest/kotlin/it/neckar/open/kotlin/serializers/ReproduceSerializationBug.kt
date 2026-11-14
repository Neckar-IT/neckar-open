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
@file:Suppress("RETURN_VALUE_NOT_USED")

package it.neckar.open.kotlin.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

fun main() {
  println("Hello")

  val json = Json.encodeToString(MySimpleSerializer, "Hello, world") //works
  println("Encoding to string worked: $json")


  //does *not* work - prints whatever the buggy serializer produced
  val jsonElement = Json.encodeToJsonElement(MySimpleSerializer, "Hello, world")
  println("Encoding to JsonElement produced: $jsonElement")
}

object MySimpleSerializer : KSerializer<Any> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MySimpleSerializer") {
  }

  override fun serialize(encoder: Encoder, value: Any) {
    try {
      val serializer = serializer(value::class.java)
      println("Serializer: $serializer of class ${serializer::class.qualifiedName} }")
      serializer.serialize(encoder, value)
    } catch (e: Exception) {
      throw SerializationException("Failed to serialize [${value::class.java.name}]", e)
    }
  }

  override fun deserialize(decoder: Decoder): Any {
    throw UnsupportedOperationException("Not supported")
  }
}
