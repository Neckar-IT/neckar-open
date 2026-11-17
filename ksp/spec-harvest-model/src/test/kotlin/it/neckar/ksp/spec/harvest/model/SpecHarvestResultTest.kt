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
package it.neckar.ksp.spec.harvest.model

import assertk.*
import assertk.assertions.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 *
 */
class SpecHarvestResultTest {
  @Test
  fun testIt() {
    val result = SpecHarvestResult(
      classSpecs = mapOf(
        "com.example.MyClass" to HarvestedClassSpec(
          qualifiedName = "com.example.MyClass",
          properties = emptyList(),
          packageName = "the.package",
          simpleName = "MyClass",
          type = "class",
          modifiers = emptyList(),
          superTypes = emptyList(),
          documentation = null,
          annotations = emptyList(),
          typeParameters = emptyList(),
        ),
        "com.example.OtherClass" to HarvestedClassSpec(
          qualifiedName = "com.example.OtherClass",
          properties = emptyList(),
          packageName = "the.package",
          simpleName = "OtherClass",
          type = "class",
          modifiers = emptyList(),
          superTypes = emptyList(),
          documentation = null,
          annotations = emptyList(),
          typeParameters = emptyList(),
        ),
      ),
    )

    val json = Json {
      prettyPrint = true
      prettyPrintIndent = "  "
      encodeDefaults = true
      explicitNulls = false
    }

    assertThat(
      json.encodeToString(result)
    ).isEqualTo(
      //language=JSON
      $$"""
        {
          "classSpecs": {
            "com.example.MyClass": {
              "packageName": "the.package",
              "qualifiedName": "com.example.MyClass",
              "simpleName": "MyClass",
              "type": "class",
              "modifiers": [],
              "superTypes": [],
              "annotations": [],
              "typeParameters": [],
              "properties": [],
              "enumEntries": []
            },
            "com.example.OtherClass": {
              "packageName": "the.package",
              "qualifiedName": "com.example.OtherClass",
              "simpleName": "OtherClass",
              "type": "class",
              "modifiers": [],
              "superTypes": [],
              "annotations": [],
              "typeParameters": [],
              "properties": [],
              "enumEntries": []
            }
          }
        }
      """.trimIndent()
    )

  }
}
