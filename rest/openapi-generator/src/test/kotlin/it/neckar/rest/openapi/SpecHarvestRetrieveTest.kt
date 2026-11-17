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
package it.neckar.rest.openapi

import assertk.*
import assertk.assertions.*
import it.neckar.open.annotations.GermanTerm
import it.neckar.rest.openapi.spec.harvest.HarvestedSpecSupport
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 *
 */
class SpecHarvestRetrieveTest {
  @Test
  fun testLoadSpec() {
    HarvestedSpecSupport.findProductionSpecFiles().also { specsFile ->
      assertThat(specsFile).hasSize(1)
      assertThat(specsFile.first()).hasName("neckarIT-internal-open-rest-openapi-generator.json")
    }

    HarvestedSpecSupport.findTestSpecFiles().also { specsFile ->
      assertThat(specsFile).hasSize(1)
      assertThat(specsFile.first()).hasName("neckarIT-internal-open-rest-openapi-generator-test.json")
    }

    HarvestedSpecSupport.loadTestSpecs().also { specs ->
      assertThat(specs).hasSize(1)

      val classSpec = specs.first().getClassSpec(FooBar::class)
      assertThat(classSpec.qualifiedName).isEqualTo(FooBar::class.qualifiedName)
    }

    HarvestedSpecSupport.loadProductionSpecs().also { specs ->
      assertThat(specs).hasSize(1)

      val classSpec = specs.first().getClassSpec(OpenApiMetaData.Category::class)
      assertThat(classSpec.qualifiedName).isEqualTo(OpenApiMetaData.Category::class.qualifiedName)
      assertThat(classSpec.documentation).isEqualTo(
        """
         The category of a route.
          Can be used to filter routes in the OpenAPI documentation.

          This is helpful to group routes and generate multiple openapi.json files for different categories.
        """.trimIndent()
      )
    }
  }

  /**
   * This is the doc
   */
  @GermanTerm("Beispiel")
  @Serializable
  private data class FooBar(
    val foo: String,
    val bar: Int
  )
}
