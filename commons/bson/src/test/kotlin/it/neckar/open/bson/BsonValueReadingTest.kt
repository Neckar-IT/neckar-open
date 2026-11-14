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
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import org.bson.BsonBinary
import org.bson.BsonInt32
import org.bson.BsonNull
import org.bson.BsonObjectId
import org.bson.BsonString
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test

/**
 * Tests [stringOrNull] and [toReadableString]: reading text out of a document whose shape is not
 * guaranteed, and naming a value in a message a human then has to act on.
 */
class BsonValueReadingTest {

  @Test
  fun `stringOrNull returns the text of a string value`() {
    assertThat(BsonString("Springer").stringOrNull()).isEqualTo("Springer")
  }

  @Test
  fun `stringOrNull returns null for an absent value`() {
    assertThat(null.stringOrNull()).isNull()
  }

  @Test
  fun `stringOrNull returns null for BSON null`() {
    assertThat(BsonNull.VALUE.stringOrNull()).isNull()
  }

  @Test
  fun `stringOrNull throws for a value of another type`() {
    // A field that was supposed to hold text and holds a number is a defect in the data. Returning
    // null would hide it behind "no value here", and the caller would carry on past it.
    assertFailure { BsonInt32(42).stringOrNull() }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("not a string but a INT32")
  }

  @Test
  fun `toReadableString prints a binary UUID as that UUID`() {
    val uuid = Uuid.parse("11111111-1111-1111-1111-111111111111")

    assertThat(BsonBinary(uuid.toJavaUuid()).toReadableString()).isEqualTo("11111111-1111-1111-1111-111111111111")
  }

  @Test
  fun `toReadableString prints an ObjectId as its hex string`() {
    val objectId = ObjectId("6a8dd43b39e9e5ca8fd6b940")

    assertThat(BsonObjectId(objectId).toReadableString()).isEqualTo("6a8dd43b39e9e5ca8fd6b940")
  }

  @Test
  fun `toReadableString prints a string as itself, without BSON quoting`() {
    assertThat(BsonString("publishers").toReadableString()).isEqualTo("publishers")
  }

  @Test
  fun `toReadableString names an absent value`() {
    assertThat(null.toReadableString()).isEqualTo("none")
  }

  @Test
  fun `toReadableString falls back to toString for a type it does not special-case`() {
    assertThat(BsonInt32(42).toReadableString()).isEqualTo(BsonInt32(42).toString())
  }
}
