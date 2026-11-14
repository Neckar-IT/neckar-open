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

import assertk.*
import assertk.assertions.*
import it.neckar.logging.LoggerFactory
import it.neckar.open.bson.DefaultBsonConfiguration
import it.neckar.open.kotlin.serializers.JsonInclusionStrategy
import it.neckar.open.test.utils.JsonUtils
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import org.apache.commons.io.output.StringBuilderWriter
import org.bson.BsonBinaryReader
import org.bson.BsonBinaryWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.kotlinx.BsonConfiguration
import org.bson.codecs.kotlinx.KotlinSerializerCodec
import org.bson.io.BasicOutputBuffer
import org.bson.json.JsonReader
import org.bson.json.JsonWriter
import java.nio.ByteBuffer
import kotlin.reflect.KClass

private val logger: it.neckar.logging.Logger = LoggerFactory.getLogger("it.neckar.open.serialization.BsonSerializationTestMethods")


/**
 * Round trips the object through the MongoDB codec. [expectedBson] holds *extended JSON* — the textual BSON form with
 * `$binary`/`$date` wrappers, not the binary encoding: the binary form is round tripped but never compared as text.
 */
@IgnorableReturnValue
inline fun <reified T : Any> roundTripBson(
  objectToSerialize: T,
  serializer: KSerializer<T>,
  serializersModule: SerializersModule = EmptySerializersModule(),
  noinline comparisonCheck: ComparisonCheck<T> = { deserialized, originalObject ->
    assertThat(deserialized).isEqualTo(originalObject)
  },
  expectedBson: String?,
) {
  roundTripBson(objectToSerialize = objectToSerialize, serializer = serializer, serializersModule = serializersModule, comparisonCheck = comparisonCheck) { expectedBson }
}

/**
 * Round trips the object through a [KotlinSerializerCodec] built from [serializer]. [expectedBsonProvider] returns the
 * expected extended JSON; null skips that assertion and leaves only the round trip itself under test.
 */
@IgnorableReturnValue
inline fun <reified T : Any> roundTripBson(
  objectToSerialize: T,
  serializer: KSerializer<T> = serializer(),
  serializersModule: SerializersModule = EmptySerializersModule(),
  /**
   * The bson configuration - which is used to configure the bson serialization (e.g., include null values)
   */
  bsonConfiguration: BsonConfiguration = DefaultBsonConfiguration,
  noinline comparisonCheck: ComparisonCheck<T> = { deserialized, originalObject ->
    assertThat(deserialized).isEqualTo(originalObject)
  },
  noinline expectedBsonProvider: () -> String?,
): T {

  val codec = KotlinSerializerCodec.create(
    kClass = T::class,
    serializer = serializer,
    serializersModule = serializersModule,
    bsonConfiguration = bsonConfiguration
  )

  return roundTripBson(T::class, objectToSerialize, codec, comparisonCheck, expectedBsonProvider)
}

@IgnorableReturnValue
inline fun <reified T : Any> roundTripBson(
  objectToSerialize: T,
  codec: Codec<T>,
  noinline comparisonCheck: ComparisonCheck<T> = { deserialized, originalObject ->
    assertThat(deserialized).isEqualTo(originalObject)
  },
  expectedJson: String?,
): T {
  return roundTripBson(T::class, objectToSerialize, codec, comparisonCheck) { expectedJson }
}

/**
 * Encodes the object twice — once as extended JSON, once as binary BSON — and runs [comparisonCheck] against the result
 * of decoding each of them. Both paths must survive: a codec that only works on one of the two representations fails here.
 *
 * Returns the instance decoded from the JSON representation. [type] is not read by this function, the [codec] already
 * carries the type.
 */
@IgnorableReturnValue
fun <T : Any> roundTripBson(
  type: KClass<T>,
  objectToSerialize: T,
  codec: Codec<T>,
  /**
   * Comparison check that is called. Should throw an exception
   */
  comparisonCheck: ComparisonCheck<T> = { deserialized, originalObject ->
    assertThat(deserialized).isEqualTo(originalObject)
  },
  expectedJsonProvider: () -> String?,
): T {

  val stringBuilderWriter = StringBuilderWriter()
  codec.encode(JsonWriter(stringBuilderWriter), objectToSerialize, EncoderContext.builder().build())
  val json = stringBuilderWriter.builder.toString()

  val basicOutputBuffer = BasicOutputBuffer()
  val bsonBinaryWriter = BsonBinaryWriter(basicOutputBuffer)
  codec.encode(bsonBinaryWriter, objectToSerialize, EncoderContext.builder().build())

  logger.debug("JSON length: {}", json.toByteArray().size)
  logger.debug("Binary length: {}", basicOutputBuffer.size())
  logger.debug("Binary advantage: {}", json.toByteArray().size - basicOutputBuffer.size())

  expectedJsonProvider()?.let { expectedJson ->
    JsonUtils.assertJsonEquals(expectedJson, json)
  }

  val deserializedFromJson = codec.decode(JsonReader(json), DecoderContext.builder().build())
  comparisonCheck(deserializedFromJson, objectToSerialize)

  val deserializedFromBson = codec.decode(BsonBinaryReader(ByteBuffer.wrap(basicOutputBuffer.toByteArray())), DecoderContext.builder().build())
  comparisonCheck(deserializedFromBson, objectToSerialize)

  return deserializedFromJson
}

/**
 * Round trips the objects as a single array. Despite the name no BSON codec is involved: the list is encoded and decoded
 * with kotlinx JSON, so this asserts the plain JSON form of the elements — [roundTripBson] per element covers BSON.
 */
@IgnorableReturnValue
fun <T> roundTripBsonList(vararg objectsToSerialize: T, expectedJson: String?, inclusionStrategy: JsonInclusionStrategy, serializer: KSerializer<T>) {
  val encoder = Json {
    defaultJsonConfiguration(prettyPrintEnabled = true, inclusionStrategy = inclusionStrategy)
  }

  val listSerializer = ListSerializer(serializer)

  val objectsToSerializeList: List<T> = objectsToSerialize.toList()
  val json = encoder.encodeToString(listSerializer, objectsToSerializeList)

  //println("JSON length: ${json.toByteArray().size}")

  if (expectedJson != null) {
    JsonUtils.assertJsonEquals(expectedJson, json)
  }

  val deserialized = Json.decodeFromString(listSerializer, json)
  assertThat(deserialized).isEqualTo(objectsToSerializeList)
}
