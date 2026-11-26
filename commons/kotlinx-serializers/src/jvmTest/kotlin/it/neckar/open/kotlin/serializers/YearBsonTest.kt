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

import assertk.assertThat
import assertk.assertions.isEqualTo
import it.neckar.datetime.minimal.Year
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.serializer
import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.BsonDocumentWriter
import org.bson.BsonInt32
import org.bson.BsonType
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.kotlinx.BsonConfiguration
import org.bson.codecs.kotlinx.KotlinSerializerCodec
import org.junit.jupiter.api.Test

/**
 * [Year] holds a Long and reads a year stored as Int32 or as Int64.
 */
class YearBsonTest {
  @Serializable
  private data class Author(val yearOfBirth: Year)

  private val codec = KotlinSerializerCodec.create(Author::class, serializer<Author>(), EmptySerializersModule(), BsonConfiguration())

  @Test
  fun `an Int32 year decodes`() {
    val stored = BsonDocument("yearOfBirth", BsonInt32(1835))
    assertThat(codec.decode(BsonDocumentReader(stored), DecoderContext.builder().build())).isEqualTo(Author(Year(1835)))
  }

  @Test
  fun `a year beyond Int round trips as Int64`() {
    val written = BsonDocument()
    codec.encode(BsonDocumentWriter(written), Author(Year(-13_787_000_000L)), EncoderContext.builder().build())
    assertThat(written.getValue("yearOfBirth").bsonType).isEqualTo(BsonType.INT64)
    assertThat(codec.decode(BsonDocumentReader(written), DecoderContext.builder().build())).isEqualTo(Author(Year(-13_787_000_000L)))
  }
}
