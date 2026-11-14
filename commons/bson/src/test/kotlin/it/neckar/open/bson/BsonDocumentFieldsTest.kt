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
package it.neckar.open.bson

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.messageContains
import org.bson.BsonArray
import org.bson.BsonBinary
import org.bson.BsonDateTime
import org.bson.BsonDocument
import org.bson.BsonDouble
import org.bson.BsonInt32
import org.bson.BsonInt64
import org.bson.BsonNull
import org.bson.BsonString
import org.junit.jupiter.api.Test

/**
 * Tests the field readers of [BsonDocument]: absent and BSON null read as null, a wrong type throws
 * naming the field, and [findAt] walks a dotted path the same way.
 */
class BsonDocumentFieldsTest {

  private val document: BsonDocument = BsonDocument()
    .append("name", BsonString("Süd-Dach"))
    .append("width", BsonInt32(8))
    .append("roof", BsonDocument().append("inclination", BsonDouble(35.0)))
    .append("moduleAreas", BsonArray(listOf(BsonDocument())))
    .append("id", BsonBinary(ByteArray(16)))
    .append("unset", BsonNull.VALUE)

  @Test
  fun `a field of the expected type reads as its value`() {
    assertThat(document.stringOrNull("name")).isEqualTo("Süd-Dach")
    assertThat(document.numberOrNull("width")).isEqualTo(8.0)
    assertThat(document.documentOrNull("roof")).isEqualTo(BsonDocument().append("inclination", BsonDouble(35.0)))
    assertThat(document.arrayOrNull("moduleAreas")).isEqualTo(BsonArray(listOf(BsonDocument())))
    assertThat(document.binaryOrNull("id")).isEqualTo(BsonBinary(ByteArray(16)))
  }

  @Test
  fun `an absent field and BSON null read as null`() {
    assertThat(document.stringOrNull("absent")).isNull()
    assertThat(document.stringOrNull("unset")).isNull()
    assertThat(document.documentOrNull("unset")).isNull()
    assertThat(document.arrayOrNull("absent")).isNull()
  }

  @Test
  fun `a field of another type throws naming the field and both types`() {
    assertFailure { document.stringOrNull("width") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("<width> holds INT32 instead of STRING")
    assertFailure { document.numberOrNull("name") }.messageContains("<name> holds STRING instead of a number")
    assertFailure { document.documentOrNull("name") }.messageContains("<name> holds STRING instead of DOCUMENT")
    assertFailure { document.arrayOrNull("roof") }.messageContains("<roof> holds DOCUMENT instead of ARRAY")
    assertFailure { document.binaryOrNull("width") }.messageContains("<width> holds INT32 instead of BINARY")
  }

  @Test
  fun `a point in time reads as its value, null when absent or BSON null, and throws naming the document for another type`() {
    val stored = BsonDocument().append("_id", BsonString("order-1"))
      .append("orderDate", BsonDateTime(1_000L))
      .append("value", BsonInt64(42))
      .append("unset", BsonNull.VALUE)

    assertThat(stored.dateTimeOrNull("orderDate")).isEqualTo(BsonDateTime(1_000L))
    assertThat(stored.dateTimeOrNull("absent")).isNull()
    assertThat(stored.dateTimeOrNull("unset")).isNull()
    assertFailure { stored.dateTimeOrNull("value") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("<value> holds INT64 instead of DATE_TIME in document <order-1>")
  }

  @Test
  fun `the documents of an array read as a list, an element of another type throws`() {
    assertThat(document.documentsOrEmpty("moduleAreas")).isEqualTo(listOf(BsonDocument()))
    assertThat(document.documentsOrEmpty("absent")).isEqualTo(emptyList())
    assertThat(document.documentsOrEmpty("unset")).isEqualTo(emptyList())
    assertFailure { document.documentsOrEmpty("name") }.messageContains("<name> holds STRING instead of ARRAY")

    val mixed = BsonDocument().append("areas", BsonArray(listOf(BsonDocument(), BsonInt32(1))))
    assertFailure { mixed.documentsOrEmpty("areas") }.messageContains("<areas[1]> holds INT32 instead of DOCUMENT")
  }

  @Test
  fun `a message names the document by its id where it has one`() {
    val stored = BsonDocument().append("_id", BsonString("roof-1")).append("width", BsonInt32(8)).append("roof", BsonString("none"))

    assertFailure { stored.stringOrNull("width") }.messageContains("<width> holds INT32 instead of STRING in document <roof-1>")
    assertFailure { stored.findAt("roof.inclination") }.messageContains("so <inclination> cannot be reached in document <roof-1>")
  }

  @Test
  fun `findAt walks a dotted path to the value`() {
    assertThat(document.findAt("roof.inclination")).isEqualTo(BsonDouble(35.0))
    assertThat(document.findAt("roof")).isEqualTo(BsonDocument().append("inclination", BsonDouble(35.0)))
  }

  @Test
  fun `findAt answers null where a segment is absent or BSON null`() {
    assertThat(document.findAt("roof.azimuth")).isNull()
    assertThat(document.findAt("absent.inclination")).isNull()
    assertThat(document.findAt("unset.inclination")).isNull()
    assertThat(document.findAt("unset")).isNull()
  }

  @Test
  fun `a typed read along a path answers the value, null for an absent step, and throws for another leaf type`() {
    assertThat(document.findDocumentAt("roof")).isEqualTo(BsonDocument().append("inclination", BsonDouble(35.0)))
    assertThat(document.findDocumentAt("absent.roof")).isNull()
    assertThat(document.findBooleanAt("roof.confirmed")).isNull()
    assertFailure { document.findDocumentAt("roof.inclination") }.messageContains("<roof.inclination> holds DOUBLE instead of DOCUMENT")
    assertFailure { document.findBooleanAt("name") }.messageContains("<name> holds STRING instead of BOOLEAN")
  }

  @Test
  fun `an integer read along a path answers INT32 and INT64 and throws for a DOUBLE`() {
    val stored = BsonDocument().append("_id", BsonString("order-1"))
      .append("number", BsonDocument().append("year", BsonInt32(2026)).append("value", BsonInt64(42)).append("share", BsonDouble(0.6)))

    assertThat(stored.findIntegerAt("number.year")).isEqualTo(2026L)
    assertThat(stored.findIntegerAt("number.value")).isEqualTo(42L)
    assertThat(stored.findIntegerAt("number.missing")).isNull()
    assertFailure { stored.findIntegerAt("number.share") }.messageContains("<number.share> holds DOUBLE instead of INT32 or INT64 in document <order-1>")
  }

  @Test
  fun `a string read along a path answers the text and throws naming path and document for another type`() {
    val stored = BsonDocument().append("_id", BsonString("company-1"))
      .append("legalInformation", BsonDocument().append("taxId", BsonString("DE123456789")).append("registrationNumber", BsonInt32(1)))

    assertThat(stored.findStringAt("legalInformation.taxId")).isEqualTo("DE123456789")
    assertThat(stored.findStringAt("legalInformation.director")).isNull()
    assertFailure { stored.findStringAt("legalInformation.registrationNumber") }
      .messageContains("<legalInformation.registrationNumber> holds INT32 instead of STRING in document <company-1>")
  }

  @Test
  fun `findAt throws where a segment holds something other than a document`() {
    assertFailure { document.findAt("name.first") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("<name> holds STRING instead of DOCUMENT, so <first> cannot be reached")
  }
}
