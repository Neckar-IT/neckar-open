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
package it.neckar.otel

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class EndpointGatedSdkEnablementTest {
  @Test
  fun `no endpoint and no explicit decision disables the SDK`() {
    assertThat(EndpointGatedSdkEnablement.enablementOverrides(endpointConfigured = false, sdkDisabledExplicitlySet = false))
      .isEqualTo(mapOf("otel.sdk.disabled" to "true"))
  }

  @Test
  fun `a configured endpoint leaves the SDK enabled`() {
    assertThat(EndpointGatedSdkEnablement.enablementOverrides(endpointConfigured = true, sdkDisabledExplicitlySet = false))
      .isEmpty()
  }

  @Test
  fun `an explicit otel_sdk_disabled is never overridden`() {
    // Operator forced a decision (either value) — respect it even without an endpoint.
    assertThat(EndpointGatedSdkEnablement.enablementOverrides(endpointConfigured = false, sdkDisabledExplicitlySet = true))
      .isEmpty()
    assertThat(EndpointGatedSdkEnablement.enablementOverrides(endpointConfigured = true, sdkDisabledExplicitlySet = true))
      .isEmpty()
  }

  @Test
  fun `endpoint detection is true when any endpoint value is non-blank`() {
    assertThat(EndpointGatedSdkEnablement.anyEndpointConfigured(listOf(null, null, null, null))).isFalse()
    assertThat(EndpointGatedSdkEnablement.anyEndpointConfigured(listOf(null, "", "  ", null))).isFalse()
    assertThat(EndpointGatedSdkEnablement.anyEndpointConfigured(listOf(null, null, "http://otel-agent:4317", null))).isTrue()
    assertThat(EndpointGatedSdkEnablement.anyEndpointConfigured(listOf("http://localhost:4317", null, null, null))).isTrue()
  }
}
