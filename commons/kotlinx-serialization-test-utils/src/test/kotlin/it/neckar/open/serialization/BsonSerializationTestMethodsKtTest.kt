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
package it.neckar.open.serialization

import it.neckar.open.bson.asKotlinUuid
import it.neckar.open.bson.toBsonUuid
import it.neckar.open.test.utils.SlowTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.BsonBinarySubType
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

/**
 *
 */
@SlowTest
class BsonSerializationTestMethodsKtTest {
  @Test
  fun testSimple() {
    roundTripBson(MyObject("test", 1)) {
      //language=JSON
      """
        {
          "name" : "test",
          "age" : 1
        }
      """.trimIndent()
    }
  }

  @Test
  fun testWithUuid() {
    roundTripBson(MyObject2(Uuid.parse("ee7ba23e-d54a-401d-9f80-7a97976a1795"), 1)) {
      //language=JSON
      $$"""
        {
          "uuid" : {
            "$binary" : {
              "base64" : "7nuiPtVKQB2fgHqXl2oXlQ==",
              "subType" : "04"
            }
          },
          "age" : 1
        }
      """.trimIndent()
    }
  }

  @Serializable
  private data class MyObject(val name: String, val age: Int)

  @Serializable
  private data class MyObject2(val uuid: @Serializable(with = MyUuidSerializer::class) Uuid, val age: Int)
}

private object MyUuidSerializer : KSerializer<Uuid> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MyUuid", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Uuid) {
    when (encoder) {
      is BsonEncoder -> {
        //encoder.encodeBsonValue(BsonString(value.toString())) //61 Bytes
        //encoder.encodeBsonValue(BsonBinary(value.bytes)) //41 Bytes
        encoder.encodeBsonValue(value.toBsonUuid()) //41 Bytes
        //encoder.encodeString(value.toString())
      }

      else -> {
        encoder.encodeString(value.toString())
      }
    }
  }

  override fun deserialize(decoder: Decoder): Uuid {
    when (decoder) {
      is BsonDecoder -> {
        val bsonValue = decoder.decodeBsonValue()
        val asBinary = bsonValue.asBinary()
        require(asBinary.type == BsonBinarySubType.UUID_STANDARD.value) { "Expected UUID sub type but was ${asBinary.type}" }
        return asBinary.asKotlinUuid()
      }

      else -> {
        val string = decoder.decodeString()
        return Uuid.parse(string)
      }
    }
  }
}
