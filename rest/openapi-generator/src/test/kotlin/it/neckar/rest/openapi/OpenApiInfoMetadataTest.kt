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
import it.neckar.open.http.Url
import it.neckar.open.kotlin.serializers.JsonPath
import it.neckar.open.serialization.atPath
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

/**
 * The optional `info` fields (summary, termsOfService, contact, license) are only written when set.
 */
class OpenApiInfoMetadataTest {
  private val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

  @Test
  fun `minimal metadata emits only title, description and version`() {
    val info = generator.generate(routes = emptyList(), metaData = OpenApiMetaData("My API", "The description"))
      .jsonObject.atPath(JsonPath("info"))

    assertThat(info).isJsonEqualTo(
      //language=JSON
      """{ "title": "My API", "description": "The description", "version": "1.0.0" }"""
    )
  }

  @Test
  fun `contact, license, summary and terms of service are emitted when set`() {
    val metaData = OpenApiMetaData(
      title = "My API",
      description = "The description",
      summary = "A short summary",
      termsOfService = "https://neckar.it/tos",
      contact = OpenApiContact(name = "Neckar IT", url = Url.absolute("https://neckar.it"), email = "info@neckar.it"),
      license = OpenApiLicense(name = "GPL-3.0", url = Url.absolute("https://www.gnu.org/licenses/gpl-3.0")),
    )

    val info = generator.generate(routes = emptyList(), metaData = metaData).jsonObject.atPath(JsonPath("info"))

    assertThat(info).isJsonEqualTo(
      //language=JSON
      """
      {
        "title": "My API",
        "summary": "A short summary",
        "description": "The description",
        "version": "1.0.0",
        "termsOfService": "https://neckar.it/tos",
        "contact": { "name": "Neckar IT", "url": "https://neckar.it", "email": "info@neckar.it" },
        "license": { "name": "GPL-3.0", "url": "https://www.gnu.org/licenses/gpl-3.0" }
      }
      """.trimIndent()
    )
  }
}
