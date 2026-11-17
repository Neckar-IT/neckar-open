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
package it.neckar.rest.test.openapi

import assertk.*
import assertk.assertions.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.openapi.OpenApiException
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.test.createTestClient
import it.neckar.rest.test.example.ExampleService
import it.neckar.rest.test.launchServiceInTest
import org.junit.jupiter.api.Test

@DisableLogging
class OpenApiServiceTestExtTest {
  @Test
  fun `testOpenApi accepts the JSON the service actually serves`() = testApplication {
    val service = ExampleService()
    launchServiceInTest(service)

    val client = createTestClient()

    val servedJson = client.getOpenApiJson(service.serviceDescriptor, OpenApiMetaData.Category.Default)

    client.testOpenApi(service, OpenApiMetaData.Category.Default) { servedJson }
  }

  @Test
  fun `testOpenApi fails for a different JSON`() = testApplication {
    val service = ExampleService()
    launchServiceInTest(service)

    val client = createTestClient()

    assertFailure {
      client.testOpenApi(service, OpenApiMetaData.Category.Default) { """{"openapi": "3.1.0"}""" }
    }.isInstanceOf(AssertionError::class)
  }

  @Test
  fun `generateOpenApiJsonFiles fails for a route without a doc block`() = testApplication {
    application {
      routing {
        get("/without-doc") {
        }
      }
    }

    assertFailure {
      generateOpenApiJsonFiles(ExampleService())
    }.isInstanceOf(OpenApiException::class).messageContains("/without-doc/(method:GET)")
  }
}
