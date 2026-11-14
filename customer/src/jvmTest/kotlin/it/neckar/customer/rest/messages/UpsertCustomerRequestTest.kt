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
package it.neckar.customer.rest.messages

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import it.neckar.contact.PhoneNumber
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class UpsertCustomerRequestTest {

  private fun decode(phone: String, cellphone: String): UpsertCustomerRequest = Json.decodeFromString<UpsertCustomerRequest>(
    """
      {
        "id": "ce0a2430-fc92-4d41-bd92-8a75407e882e",
        "firstName": "Max",
        "lastName": "Mustermann",
        "address": null,
        "phone": $phone,
        "cellphone": $cellphone,
        "email": null,
        "company": null
      }
    """.trimIndent()
  )

  @Test
  fun `JSON decode turns an empty phone into null`() {
    val request = decode(phone = "\"\"", cellphone = "null")

    assertThat(request.phone).isNull()
    assertThat(request.cellphone).isNull()
  }

  @Test
  fun `JSON decode turns an empty cellphone into null`() {
    val request = decode(phone = "null", cellphone = "\"\"")

    assertThat(request.phone).isNull()
    assertThat(request.cellphone).isNull()
  }

  @Test
  fun `JSON decode keeps a filled phone and cellphone`() {
    val request = decode(phone = "\"07473 123\"", cellphone = "\"+49 171 456\"")

    assertThat(request.phone).isEqualTo(PhoneNumber("07473 123"))
    assertThat(request.cellphone).isEqualTo(PhoneNumber("+49 171 456"))
  }
}
