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
package it.neckar.open.text

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.DefaultLocale

/**
 *
 */
class InflectorTest {
  /**
   * Turkish folds `I` to the dotless `ı` and `i` to `İ`, so case folding through the default locale
   * changes the result on a Turkish host.
   */
  @Test
  @DefaultLocale("tr")
  fun `case folding does not depend on the host locale`() {
    val inflector = Inflector()

    assertThat(inflector.capitalize("TITLE")).isEqualTo("Title")
    assertThat(inflector.isUncountable("Information")).isTrue()
    assertThat(inflector.pluralize("Information")).isEqualTo("Information")
    assertThat(inflector.underscore("InvoiceItem")).isEqualTo("invoice_item")
  }

  @Test
  fun testOrdinalize() {
    val inflector = Inflector()

    assertThat(inflector.capitalize("hello wOrld")).isEqualTo("Hello world")
    assertThat(inflector.ordinalize(13)).isEqualTo("13th")
    assertThat(inflector.ordinalize(1)).isEqualTo("1st")
    assertThat(inflector.ordinalize(2)).isEqualTo("2nd")
    assertThat(inflector.ordinalize(0)).isEqualTo("0th")

    // Teens inside larger numbers (n % 100 in 11..13) take "th", not the last-digit suffix.
    assertThat(inflector.ordinalize(111)).isEqualTo("111th")
    assertThat(inflector.ordinalize(112)).isEqualTo("112th")
    assertThat(inflector.ordinalize(113)).isEqualTo("113th")
    // Non-teen tens keep the last-digit suffix.
    assertThat(inflector.ordinalize(21)).isEqualTo("21st")
    assertThat(inflector.ordinalize(102)).isEqualTo("102nd")
    assertThat(inflector.ordinalize(103)).isEqualTo("103rd")
  }

  @Test
  fun testCapitalize() {
    val inflector = Inflector()
    assertThat(inflector.capitalize("hello wOrld")).isEqualTo("Hello world")
  }

  @Test
  fun testCamelCase() {
    val inflector = Inflector()
    assertThat(inflector.camelCase("foo_bar", true, '.')).isEqualTo("FooBar")
  }

  @Test
  fun testPluralize() {
    val inflector = Inflector()
    assertThat(inflector.pluralize("Foobar")).isEqualTo("Foobars")
    assertThat(inflector.pluralize("car")).isEqualTo("cars")
    assertThat(inflector.pluralize("batch")).isEqualTo("batches")
  }

  @Test
  fun testSingularize() {
    val inflector = Inflector()
    assertThat(inflector.singularize("Foobars")).isEqualTo("Foobar")
    assertThat(inflector.singularize("cars")).isEqualTo("car")
    assertThat(inflector.singularize("batches")).isEqualTo("batch")
  }

  @Test
  fun `a plural ending in ves keeps the singular the spec generator derives`() {
    val inflector = Inflector()
    assertThat(inflector.singularize("drives")).isEqualTo("drive")
    assertThat(inflector.singularize("knives")).isEqualTo("knife")
    assertThat(inflector.singularize("wives")).isEqualTo("wife")
    assertThat(inflector.singularize("kitchenKnives")).isEqualTo("kitchenKnife")
    assertThat(inflector.singularize("shelves")).isEqualTo("shelf")
    assertThat(inflector.singularize("wolves")).isEqualTo("wolf")
    assertThat(inflector.singularize("halves")).isEqualTo("half")
    assertThat(inflector.singularize("valves")).isEqualTo("valve")
    assertThat(inflector.singularize("curves")).isEqualTo("curve")
    assertThat(inflector.singularize("archives")).isEqualTo("archive")
    assertThat(inflector.singularize("moves")).isEqualTo("move")
  }
}
