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
package it.neckar.customer.company.rest.messages

import assertk.assertThat
import assertk.assertions.isEqualTo
import it.neckar.customer.company.RegistrationNumber
import it.neckar.customer.company.TaxId
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class CompanyLegalInformationFieldsTest {

  @Test
  fun `JSON decode trims director and registerCourt`() {
    val json = """
      {
        "director": "  Anna Schmidt  ",
        "registerCourt": "  Amtsgericht Stuttgart  ",
        "taxId": "DE192736450",
        "registrationNumber": "HRB 25478"
      }
    """.trimIndent()

    val fields = Json.decodeFromString<CompanyLegalInformationFields>(json)

    assertThat(fields.director).isEqualTo("Anna Schmidt")
    assertThat(fields.registerCourt).isEqualTo("Amtsgericht Stuttgart")
  }

  @Test
  fun `JSON decode normalizes taxId and registrationNumber via value class serializers`() {
    val json = """
      {
        "director": "Anna Schmidt",
        "registerCourt": "Amtsgericht Stuttgart",
        "taxId": "de 192 736 450",
        "registrationNumber": "hrb: 25478"
      }
    """.trimIndent()

    val fields = Json.decodeFromString<CompanyLegalInformationFields>(json)

    assertThat(fields.taxId).isEqualTo(TaxId("DE192736450"))
    assertThat(fields.registrationNumber).isEqualTo(RegistrationNumber("HRB 25478"))
  }
}
