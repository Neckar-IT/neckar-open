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
package it.neckar.uuid


import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.BsonBinary
import org.bson.BsonBinarySubType
import org.bson.BsonType
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

/**
 * Use like this:
 * `@file:UseSerializers(UuidSerializer::class)`
 */
actual object UuidSerializer : KSerializer<Uuid> {
  /**
   * The serial name for the UUID serializer
   */
  const val SerialName: String = "UuidNIT"

  actual override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(SerialName, PrimitiveKind.STRING)

  actual override fun serialize(encoder: Encoder, value: Uuid) {
    when (encoder) {
      is BsonEncoder -> {
        encoder.encodeBsonValue(BsonBinary(BsonBinarySubType.UUID_STANDARD, value.toByteArray()))
      }

      else -> {
        encoder.encodeString(value.toString())
      }
    }
  }

  actual override fun deserialize(decoder: Decoder): Uuid =
    when (decoder) {
      is BsonDecoder -> {
        val bsonValue = decoder.decodeBsonValue()
        require(bsonValue.bsonType == BsonType.BINARY) { "Expected binary but was ${bsonValue.bsonType}" }
        val asBinary: BsonBinary = bsonValue.asBinary()
        require(asBinary.type == BsonBinarySubType.UUID_STANDARD.value) { "Expected UUID sub type but was ${asBinary.type}" }
        asBinary.asUuid().toKotlinUuid()
      }

      else -> Uuid.parse(decoder.decodeString())
    }
}
