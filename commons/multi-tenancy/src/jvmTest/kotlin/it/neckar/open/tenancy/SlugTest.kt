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
package it.neckar.open.tenancy

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import it.neckar.open.resources.getResourceSafe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Holds [Slug] to the conformance corpus `Slug.cases.json`, which the Vitest of `tenantUpsertRequestSchema` in patterns
 * admin-ui reads as well.
 */
class SlugTest {

  private val corpus: Corpus = Json.decodeFromString(javaClass.getResourceSafe("/Slug.cases.json").readText())

  @TestFactory
  fun `Slug decides every conformance case`(): List<DynamicTest> = corpus.cases.map { case ->
    DynamicTest.dynamicTest("\"${case.input}\" valid=${case.valid}") {
      if (case.valid) {
        assertThat(Slug(case.input).value).isEqualTo(case.input)
      } else {
        assertFailure { Slug(case.input) }.isInstanceOf(IllegalArgumentException::class)
      }
    }
  }

  @Serializable
  private data class Corpus(val purpose: String, val cases: List<Case>)

  @Serializable
  private data class Case(val input: String, val valid: Boolean)
}
