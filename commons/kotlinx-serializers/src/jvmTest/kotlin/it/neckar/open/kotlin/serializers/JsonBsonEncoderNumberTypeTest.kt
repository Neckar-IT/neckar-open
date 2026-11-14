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
package it.neckar.open.kotlin.serializers

import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.version.Version
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.serializer
import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.BsonDocumentWriter
import org.bson.BsonType
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.kotlinx.BsonConfiguration
import org.bson.codecs.kotlinx.KotlinSerializerCodec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URL
import java.util.jar.JarFile

/**
 * Demands what a JSON number ought to do across a BSON round trip: keep its type and its value.
 *
 * From [DefectiveSince] on, `JsonBsonEncoder.encodeJsonPrimitive` runs
 * `BigDecimal(content).stripTrailingZeros()` and writes DOUBLE only for a positive scale, so `3.0`
 * (scale 0) is stored as INT32 and `1.0E20` (negative scale) as DECIMAL128. Introduced by
 * https://github.com/mongodb/mongo-java-driver/pull/1937 (JAVA-6165).
 */
class JsonBsonEncoderNumberTypeTest {
  @Serializable
  private data class Container(val payload: JsonObject)

  @Test
  fun `JSON numbers keep their BSON type`() {
    val version: Version = bsonKotlinxVersion()
    assumeTrue(version < DefectiveSince || version >= ArmedAgainFrom) {
      "bson-kotlinx $version damages integral number literals — skipped from $DefectiveSince up to $ArmedAgainFrom"
    }

    //language=JSON
    val json = """{"integral": 3.0, "fractional": 15.9, "scientific": 1.0E20}"""
    val container = Container(Json.parseToJsonElement(json).jsonObject)

    val codec = KotlinSerializerCodec.create(
      Container::class,
      serializer<Container>(),
      EmptySerializersModule(),
      BsonConfiguration(),
    )

    val encoded = BsonDocument()
    codec.encode(BsonDocumentWriter(encoded), container, EncoderContext.builder().build())
    val decoded: Container = codec.decode(BsonDocumentReader(encoded), DecoderContext.builder().build())

    //Read this when the test is red on a version at or above ArmedAgainFrom: the major did not bring
    //the fix. Either raise ArmedAgainFrom to the next major, or drop the skip and keep the guarantee.
    val failureMessage = "bson-kotlinx $version damages a JSON number across a BSON round trip"

    val payload: BsonDocument = encoded.getDocument("payload")
    assertEquals(BsonType.DOUBLE, payload.getValue("integral").bsonType, failureMessage)
    assertEquals(BsonType.DOUBLE, payload.getValue("scientific").bsonType, failureMessage)
    assertEquals(BsonType.DOUBLE, payload.getValue("fractional").bsonType, failureMessage)
    assertEquals(container, decoded, failureMessage)
  }

  /**
   * The bson-kotlinx on the test classpath, taken from the `Bundle-Version` of its own jar - the
   * driver's build writes no `Implementation-Version`, so [Package.getImplementationVersion] is null.
   */
  private fun bsonKotlinxVersion(): Version {
    val location: URL = KotlinSerializerCodec::class.java.protectionDomain?.codeSource?.location.requireNotNull {
      "no code source for ${KotlinSerializerCodec::class.java.name} — cannot tell which bson-kotlinx is loaded"
    }
    require(location.path.endsWith(".jar")) {
      "bson-kotlinx is loaded from $location, which is not a jar — cannot tell which bson-kotlinx is loaded"
    }

    val bundleVersion: String = JarFile(File(location.toURI())).use { jar ->
      jar.manifest?.mainAttributes?.getValue("Bundle-Version").requireNotNull {
        "no Bundle-Version in the manifest of $location — cannot tell which bson-kotlinx is loaded"
      }
    }

    return Version.parse(bundleVersion)
  }

  companion object {
    /**
     * The first version carrying the defect, https://github.com/mongodb/mongo-java-driver/pull/1937.
     * 5.6.x and everything below writes an integral literal as DOUBLE and passes this test.
     */
    private val DefectiveSince: Version = Version.of(5, 7, 0)

    /**
     * The version the test runs again on. A fix changes the BSON type an existing document reads
     * back as, which the driver ships in a major, so every 5.x skips.
     *
     * Compared numerically through [Version]: a lexicographic comparison sorts 5.11.1 below 5.9.2.
     */
    private val ArmedAgainFrom: Version = Version.of(6, 0, 0)
  }
}
