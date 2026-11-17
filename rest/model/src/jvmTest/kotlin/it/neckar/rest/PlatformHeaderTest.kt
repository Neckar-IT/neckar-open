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
package it.neckar.rest

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.http.HttpHeaders
import it.neckar.common.featureflags.FeatureFlags
import it.neckar.rest.tracking.XClientId
import it.neckar.rest.tracking.XClientRole
import it.neckar.rest.tracking.XServedBy
import it.neckar.rest.tracking.XSessionId
import it.neckar.rest.tracking.XTraceId
import it.neckar.rest.version.RestProtocolVersionInfo
import org.junit.jupiter.api.Test

class PlatformHeaderTest {
  @Test
  fun `wire values match expected HTTP header names`() {
    assertThat(PlatformHeader.SessionId.headerName).isEqualTo("NeckarIT-Session-Id")
    assertThat(PlatformHeader.ClientId.headerName).isEqualTo("NeckarIT-Client-Id")
    assertThat(PlatformHeader.ClientRole.headerName).isEqualTo("NeckarIT-Client-Role")
    assertThat(PlatformHeader.ProtocolVersion.headerName).isEqualTo("NeckarIT-Protocol-Version")
    assertThat(PlatformHeader.FeatureFlags.headerName).isEqualTo("NeckarIT-Feature-Flags")
    assertThat(PlatformHeader.TraceId.headerName).isEqualTo("NeckarIT-Trace-Id")
  }

  @Test
  fun `existing Ktor HttpHeaders extensions delegate to PlatformHeader`() {
    assertThat(HttpHeaders.XSessionId).isEqualTo(PlatformHeader.SessionId.headerName)
    assertThat(HttpHeaders.XClientId).isEqualTo(PlatformHeader.ClientId.headerName)
    assertThat(HttpHeaders.XClientRole).isEqualTo(PlatformHeader.ClientRole.headerName)
    assertThat(HttpHeaders.XTraceId).isEqualTo(PlatformHeader.TraceId.headerName)
    assertThat(RestProtocolVersionInfo.ProtocolVersionHeaderKey).isEqualTo(PlatformHeader.ProtocolVersion.headerName)
  }

  @Test
  fun `XServedBy stays on the legacy X- prefix because it is set across multiple stack layers`() {
    assertThat(HttpHeaders.XServedBy).isEqualTo("X-Served-By")
  }

  @Test
  fun `feature flags header key matches platform header`() {
    assertThat(FeatureFlags.HeaderKey).isEqualTo(PlatformHeader.FeatureFlags.headerName)
  }
}
