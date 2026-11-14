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
package it.neckar.csv

import assertk.*
import assertk.assertions.*
import it.neckar.open.http.Url
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test

/**
 * Verifies that [loadCSV] loads a classpath resource and hands its complete content to the parser.
 */
class LoadCsvTest {
  @Test
  fun loadsOneRowPerLineOfTheResource() {
    runTest {
      val rows = loadCSV(Url.rootRelative(BirthStatisticsResource))

      //The resource has 878 lines - the trailing line break must not add an empty row
      assertThat(rows).hasSize(878)
      assertThat(rows).each { row ->
        row.hasSize(5)
      }
    }
  }

  @Test
  fun loadsTheContentOfTheFirstAndLastRow() {
    runTest {
      val rows = loadCSV(Url.rootRelative(BirthStatisticsResource))

      assertThat(rows.first()).containsExactly("1950", "Januar", "49505", "46331", "95836")
      assertThat(rows.last()).containsExactly("2023", "Februar", "26288", "24911", "51199")
    }
  }

  @Test
  fun decodesUmlautsWithTheDefaultUtf8Charset() {
    runTest {
      val rows = loadCSV(Url.rootRelative(BirthStatisticsResource))

      assertThat(rows[2]).containsExactly("1950", "März", "53604", "49499", "103103")
    }
  }

  @Test
  fun deprecatedStringOverloadLoadsTheSameRows() {
    runTest {
      @Suppress("DEPRECATION")
      val fromString: List<List<String>> = loadCSV(BirthStatisticsResource)

      assertThat(fromString).isEqualTo(loadCSV(Url.rootRelative(BirthStatisticsResource)))
    }
  }
}

private const val BirthStatisticsResource: String = "/data/StatistikGeburtenDE.csv"
