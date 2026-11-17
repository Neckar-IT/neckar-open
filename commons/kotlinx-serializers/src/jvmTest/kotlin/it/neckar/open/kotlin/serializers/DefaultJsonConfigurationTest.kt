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
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class DefaultJsonConfigurationTest {

  @Serializable
  private data class Sample(val a: Int, val b: String)

  @Test
  fun `compact mode builds and produces single-line json`() {
    // Regression: prettyPrintEnabled=false used to throw "Indent should not be specified when
    // default printing mode is used" because prettyPrintIndent was set unconditionally.
    val json = Json { defaultJsonConfiguration(prettyPrintEnabled = false) }

    val encoded = json.encodeToString(Sample.serializer(), Sample(1, "x"))

    assertThat(encoded).doesNotContain("\n")
    assertThat(encoded).isEqualTo("""{"a":1,"b":"x"}""")
  }

  @Test
  fun `pretty mode indents with two spaces`() {
    val json = Json { defaultJsonConfiguration(prettyPrintEnabled = true) }

    val encoded = json.encodeToString(Sample.serializer(), Sample(1, "x"))

    assertThat(encoded).contains("\n  \"a\": 1")
  }
}
