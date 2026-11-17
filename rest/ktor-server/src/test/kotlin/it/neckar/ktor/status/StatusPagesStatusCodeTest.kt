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
package it.neckar.ktor.status

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.CannotTransformContentToTypeException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.mockk
import it.neckar.ktor.archive.archiveFilterFromQuery
import it.neckar.ktor.getLongOrFail
import it.neckar.ktor.installJson
import it.neckar.ktor.rest.requireIdMatchesPath
import it.neckar.ktor.uuid
import it.neckar.open.http.UrlParameterName
import it.neckar.open.test.utils.DisableLogging
import it.neckar.problem.ApplicationError
import it.neckar.problem.UnexpectedApplicationError
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

/**
 * A formally invalid request answers 400, an exception inside the server 500 (ADL 0075).
 */
@DisableLogging
class StatusPagesStatusCodeTest {
  @Test
  fun `a failed require in the server answers 500`() = testApplication {
    installStatusPagesApplication()

    val response = jsonClient().get("/api/require")
    assertThat(response.status).isEqualTo(HttpStatusCode.InternalServerError)
    assertThat(response.body<ApplicationError>()).isInstanceOf(UnexpectedApplicationError::class)
  }

  @Test
  fun `an untransformable request body answers 400`() = testApplication {
    installStatusPagesApplication()

    val response = jsonClient().get("/api/content-transformation")
    assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    assertThat(response.body<ApplicationError>()).isInstanceOf(UnexpectedApplicationError::class)
  }

  @Test
  fun `a body id that differs from the path id answers 400`() = testApplication {
    installStatusPagesApplication()

    val response = jsonClient().get("/api/id-mismatch")
    assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    assertThat(response.body<ApplicationError>()).isInstanceOf(UnexpectedApplicationError::class)
  }

  @Test
  fun `an unknown archive filter answers 400`() = testApplication {
    installStatusPagesApplication()

    val response = jsonClient().get("/api/archive-filter?archiveFilter=Nonsense")
    assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    assertThat(response.body<ApplicationError>()).isInstanceOf(UnexpectedApplicationError::class)
  }

  @Test
  fun `a known archive filter is read`() = testApplication {
    installStatusPagesApplication()

    val response = jsonClient().get("/api/archive-filter?archiveFilter=Archived")
    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    assertThat(response.bodyAsText()).isEqualTo("Archived")
  }

  @Test
  fun `an unparseable uuid parameter answers 400`() = testApplication {
    installStatusPagesApplication()

    val response = jsonClient().get("/api/uuid/not-a-uuid")
    assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    assertThat(response.body<ApplicationError>()).isInstanceOf(UnexpectedApplicationError::class)
  }

  @Test
  fun `an unparseable long parameter answers 400`() = testApplication {
    installStatusPagesApplication()

    val response = jsonClient().get("/api/long/twelve")
    assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    assertThat(response.body<ApplicationError>()).isInstanceOf(UnexpectedApplicationError::class)
  }

  @Test
  fun `a missing uuid parameter answers 400`() = testApplication {
    installStatusPagesApplication()

    val response = jsonClient().get("/api/uuid")
    assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    assertThat(response.body<ApplicationError>()).isInstanceOf(UnexpectedApplicationError::class)
  }

  /**
   * Ktor's receive wraps the decoding failure in a BadRequestException; without that the
   * IllegalArgumentException handler would answer 500.
   */
  @Test
  fun `an unreadable json body answers 400`() = testApplication {
    installStatusPagesApplication()

    val response = jsonClient().post("/api/body") {
      contentType(ContentType.Application.Json)
      setBody("""{"name": """)
    }
    assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    assertThat(response.body<ApplicationError>()).isInstanceOf(UnexpectedApplicationError::class)
  }

  private fun ApplicationTestBuilder.installStatusPagesApplication() {
    application {
      installStatusPages(logger = mockk(relaxed = true), config = {})
      installJson { }

      routing {
        get("/api/require") {
          require(false) { "an invariant of the service" }
        }
        get("/api/content-transformation") {
          throw CannotTransformContentToTypeException(typeOf<Map<String, String>>())
        }
        get("/api/id-mismatch") {
          requireIdMatchesPath(entityName = "Book", bodyId = "book-a", pathId = "book-b")
        }
        get("/api/uuid/{uuid}") {
          call.respondText(call.parameters.uuid().toString())
        }
        get("/api/long/{count}") {
          call.respondText(call.parameters.getLongOrFail(UrlParameterName("count")).toString())
        }
        get("/api/uuid") {
          call.respondText(call.parameters.uuid().toString())
        }
        post("/api/body") {
          call.respondText(call.receive<Map<String, String>>().toString())
        }
        get("/api/archive-filter") {
          call.respondText(call.request.queryParameters.archiveFilterFromQuery().name)
        }
      }
    }
  }

  private fun ApplicationTestBuilder.jsonClient() = createClient {
    install(ContentNegotiation) {
      json()
    }
  }
}
