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
package it.neckar.ktor.server.openapi.ktor

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import it.neckar.open.http.Url
import it.neckar.projects.common.ApiReference
import it.neckar.projects.common.Port
import it.neckar.projects.common.ServiceDescriptor
import org.junit.jupiter.api.Test

/**
 * [ApiReference.ScalarRoute] lives in `build-logic/descriptors`, out of reach of [OpenApiEndpoints],
 * which installs the route. The landing page links what the descriptor states, so a route changed
 * in [OpenApiEndpoints] alone puts a 404 behind every API entry.
 */
class ServiceDescriptorApiReferenceTest {

  private val patternsApi = ServiceDescriptor("PatternsApi", "api/patterns", Port(10100), ApiReference.Scalar)

  @Test
  fun `states the route the OpenApiPlugin installs the scalar ui under`() {
    val installedRoute = Url.root + Url.pattern(patternsApi.apiPrefix).resolve() + OpenApiEndpoints.apiUrl()

    assertThat(installedRoute.value).isEqualTo("/api/patterns/doc/api")
    assertThat(patternsApi.apiReferencePath).isEqualTo(installedRoute.value)
  }

  @Test
  fun `has no reference path without a reference`() {
    val specEditor = ServiceDescriptor("SpecEditorService", "api/spec-editor", Port(11100))

    assertThat(specEditor.apiReference).isEqualTo(ApiReference.None)
    assertThat(specEditor.apiReferencePath).isNull()
  }
}
