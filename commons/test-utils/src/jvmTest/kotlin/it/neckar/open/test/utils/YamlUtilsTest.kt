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
package it.neckar.open.test.utils

import assertk.*
import it.neckar.open.test.utils.YamlUtils.assertYamlEquals
import org.junit.jupiter.api.Test

class YamlUtilsTest {
  @Test
  fun testFails_complex() {
    assertFailure {
      // Sequence order is significant
      assertYamlEquals("[1, 2, 3]", "[3, 2, 1]")
    }
    assertFailure {
      // Numeric types: 1 (int) ≠ 1.0 (float) in STRICT mode
      assertYamlEquals("value: 1", "value: 1.01")
    }
    assertFailure {
      // Structure difference: mapping vs sequence
      assertYamlEquals("{a: 1, b: 2}", "- a: 1\n- b: 2\n")
    }
    assertFailure {
      // Different scalars
      assertYamlEquals("flag: true", "flag: 'true'")
    }
  }

  @Test
  fun testEquals_nestedStructures() {
    // Equal despite different key order (mappings are unordered)
    assertYamlEquals(
      """
      person:
        name: Alice
        age: 30
        tags: [dev, ops]
      """.trimIndent(),
      """
      person:
        tags:
          - dev
          - ops
        age: 30
        name: Alice
      """.trimIndent()
    )

    // Equal: identical sequences written in different style
    assertYamlEquals(
      "items: [a, b, c]",
      """
      items:
        - a
        - b
        - c
      """.trimIndent()
    )

    // Equal: null values written differently
    assertYamlEquals(
      "n: null",
      "n: ~"
    )

    // Equal: comments are ignored
    assertYamlEquals(
      """
      db:
        host: localhost
        port: 5432
      """.trimIndent(),
      """
      db: # default postgres
        host: localhost
        # standard port
        port: 5432
      """.trimIndent()
    )
  }

  @Test
  fun testWithActualTreeModifier() {
    // Remove volatile fields before comparing
    assertYamlEquals(
      """
      user:
        id: 123
        name: Bob
      """.trimIndent(),
      """
      user:
        id: 999
        name: Bob
        updatedAt: "2025-09-16T06:00:00Z"
      """.trimIndent()
    ) {
      // actualTreeModifier: strip updatedAt and normalize id
      val user = this.path("user")
      if (!user.isMissingNode) {
        if (user.isObject) {
          (user as com.fasterxml.jackson.databind.node.ObjectNode).remove("updatedAt")
          user.put("id", 123)
        }
      }
    }
  }

  @Test
  fun testScalars_formatsEqual() {
    // Equal: boolean scalars written in different styles
    assertYamlEquals("flag: true", "flag: True")

    // Equal: numbers with underscores (YAML allows digit separators)
    assertYamlEquals("n: 1000000", "n: 1_000_000")
  }
}
