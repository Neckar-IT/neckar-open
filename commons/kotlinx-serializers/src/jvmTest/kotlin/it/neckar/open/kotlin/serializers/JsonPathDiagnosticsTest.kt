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
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test

/**
 * What a failing migration is allowed to say about the area it ran on. Every string here goes into an
 * exception message and from there into a log, so each case is checked for what it says *and* is
 * built from values that would be recognisable if they leaked.
 */
class JsonPathDiagnosticsTest {
  private val area = parseJsonObject(
    //language=JSON
    """
      {
        "email": "info@stadtwerke.de",
        "abrechnung": { "stand": 14523.5, "datum": "2026-01-31" },
        "geprueft": true,
        "kontakte": [],
        "notiz": null
      }
    """.trimIndent()
  )

  @Test
  fun `a missing key names its position and the keys that are there instead`() {
    assertThat(area.describeAlong(JsonPath("zaehlerstand"))).isEqualTo(
      "<zaehlerstand> is not there, the area has the keys [abrechnung, email, geprueft, kontakte, notiz]"
    )

    //Sorted, not in insertion order: the same area always produces the same text
    assertThat(area.describeAlong(JsonPath("abrechnung", "standKwh"))).isEqualTo(
      "<abrechnung.standKwh> is not there, <abrechnung> has the keys [datum, stand]"
    )
  }

  @Test
  fun `a key that holds no object stops the path and says what it holds`() {
    assertThat(area.describeAlong(JsonPath("email", "lokalteil"))).isEqualTo(
      "<email> holds a string, so <lokalteil> cannot be reached"
    )

    assertThat(area.describeAlong(JsonPath("abrechnung", "stand", "einheit"))).isEqualTo(
      "<abrechnung.stand> holds a number, so <einheit> cannot be reached"
    )
  }

  @Test
  fun `a path that does resolve is described too, the root included`() {
    assertThat(area.describeAlong(JsonPath("abrechnung", "stand"))).isEqualTo("<abrechnung.stand> holds a number")
    assertThat(area.describeAlong(JsonPath.Root)).isEqualTo("the area holds an object")
  }

  @Test
  fun `every JSON type has a word, and the word is not the value`() {
    assertThat(area.describeAlong(JsonPath("geprueft"))).isEqualTo("<geprueft> holds a boolean")
    assertThat(area.describeAlong(JsonPath("kontakte"))).isEqualTo("<kontakte> holds an array")
    assertThat(area.describeAlong(JsonPath("notiz"))).isEqualTo("<notiz> holds null")
    assertThat(area.describeAlong(JsonPath("abrechnung"))).isEqualTo("<abrechnung> holds an object")

    //A string that reads like a boolean stays a string - the check is the type, not the text
    assertThat(JsonPrimitive("true").jsonTypeName()).isEqualTo("a string")
  }
}
