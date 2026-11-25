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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import it.neckar.open.http.Url
import org.junit.jupiter.api.Test

/**
 * Verifies that [OpenApiMetaData.withoutInternalServers] strips internal bind targets so they
 * are never disclosed to public clients.
 */
class OpenApiMetaDataServersTest {
  @Test
  fun `keeps public server urls`() {
    val metadata = OpenApiMetaData(
      title = "Test",
      description = "Test",
      servers = listOf(Url.absolute("https://patterns.neckar.it/")),
    )

    assertThat(metadata.withoutInternalServers().servers).containsExactly(Url.absolute("https://patterns.neckar.it/"))
  }

  @Test
  fun `drops internal bind urls`() {
    val metadata = OpenApiMetaData(
      title = "Test",
      description = "Test",
      servers = listOf(
        Url.absolute("http://localhost:10900/"),
        Url.absolute("http://app.turboangebot.localdev/"),
        Url.absolute("http://127.0.0.1:8080/"),
        Url.absolute("http://10.0.0.5/"),
        Url.absolute("http://192.168.1.10/"),
        Url.absolute("http://172.16.0.3/"),
      ),
    )

    assertThat(metadata.withoutInternalServers().servers).isEmpty()
  }

  @Test
  fun `keeps public and drops internal in a mixed list`() {
    val metadata = OpenApiMetaData(
      title = "Test",
      description = "Test",
      servers = listOf(
        Url.absolute("https://huddle.neckar.it/"),
        Url.absolute("http://localhost:10900/"),
      ),
    )

    assertThat(metadata.withoutInternalServers().servers).containsExactly(Url.absolute("https://huddle.neckar.it/"))
  }
}
