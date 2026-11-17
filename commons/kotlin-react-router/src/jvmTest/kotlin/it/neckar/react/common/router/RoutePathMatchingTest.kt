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
package it.neckar.react.common.router

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

class RoutePathMatchingTest {
  @Test
  fun testLiteralPath() {
    assertThat(routePathMatches("/customers", "/customers", emptyMap())).isTrue()
    assertThat(routePathMatches("/customers", "/orders", emptyMap())).isFalse()
    assertThat(routePathMatches("/customers", "/customers/17", emptyMap())).isFalse()
  }

  @Test
  fun testSingleParam() {
    assertThat(routePathMatches("/customers/:customerId", "/customers/17", mapOf("customerId" to "17"))).isTrue()
    assertThat(routePathMatches("/customers/:customerId", "/customers/17", mapOf("customerId" to "18"))).isFalse()
    assertThat(routePathMatches("/customers/:customerId", "/customers/17", emptyMap())).isFalse()
  }

  @Test
  fun testCollidingParamValues() {
    val params = mapOf("customerId" to "5", "orderId" to "5")

    assertThat(routePathMatches("/customers/:customerId/orders/:orderId", "/customers/5/orders/5", params)).isTrue()
    assertThat(routePathMatches("/customers/:customerId/orders", "/customers/5/orders/5", params)).isFalse()
  }

  @Test
  fun testParamValueIsSubstringOfOtherSegment() {
    val params = mapOf("customerId" to "5")

    assertThat(routePathMatches("/customers/:customerId/57", "/customers/5/57", params)).isTrue()
    assertThat(routePathMatches("/customers/57/:customerId", "/customers/57/5", params)).isTrue()
  }

  @Test
  fun testParamValueEqualsLiteralSegment() {
    val params = mapOf("name" to "orders")

    assertThat(routePathMatches("/orders/:name", "/orders/orders", params)).isTrue()
    assertThat(routePathMatches("/:name/orders", "/orders/orders", params)).isTrue()
  }

  @Test
  fun testStar() {
    val params = mapOf("customerId" to "5")

    assertThat(routePathMatches("/customers/:customerId/*", "/customers/5", params)).isTrue()
    assertThat(routePathMatches("/customers/:customerId/*", "/customers/5/details", params)).isTrue()
    assertThat(routePathMatches("/customers/:customerId/*", "/customers/5/details/more", params)).isTrue()
    assertThat(routePathMatches("/customers/:customerId/*", "/customers/5/a/b/c/d", params)).isTrue()
    assertThat(routePathMatches("/customers/:customerId/*", "/customers/6/details", params)).isFalse()
    assertThat(routePathMatches("/customers/:customerId/*", "/orders/5/details", params)).isFalse()
  }
}
