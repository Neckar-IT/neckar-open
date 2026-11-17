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

import assertk.*
import assertk.assertions.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.mockk
import it.neckar.rest.test.*
import it.neckar.ktor.installCompression
import it.neckar.ktor.installJson
import it.neckar.open.exceptions.DataIntegrityException
import it.neckar.open.exceptions.refTo
import it.neckar.problem.ApplicationError
import it.neckar.problem.UnexpectedApplicationError
import it.neckar.rest.InvalidQueryParameterException
import it.neckar.uuid.NativeUuid
import it.neckar.uuid.ValueClassUuid
import kotlin.jvm.JvmInline
import org.junit.jupiter.api.Test
import it.neckar.open.test.utils.DisableLogging

/**
 * The split of `respondUnexpectedProblem` between a 5xx and a 4xx body.
 */
@DisableLogging
class StatusPageRoutingTest {
  @Test
  fun testExceptionHandling() = testApplication {
    val logger: org.slf4j.Logger = mockk(relaxed = true)

    application {
      installStatusPages(logger = logger, config = {})
      installCompression()
      installJson { }

      routing {
        get("/runtimeException") {
          throw RuntimeException("mongodb://demo:secret@localhost:27017")
        }
        get("/invalidQueryParameter") {
          throw InvalidQueryParameterException("Invalid value [abc] for query parameter [limit]")
        }
        get("/dataIntegrity") {
          throw DataIntegrityException.missingReference(
            source = SourceEntity::class refTo TestId(uuidA),
            relationship = "child",
            target = TargetEntity::class refTo TestId(uuidB),
          )
        }
      }
    }

    startApplication()

    val client = createClient {
      install(ContentNegotiation) {
        json()
      }
    }

    client.get("/runtimeException") {
      accept(ContentType.Application.Json)
    }.let { response ->
      assertThat(response).isInternalServerError()
      assertThat(response.bodyAsText()).doesNotContain("secret")

      val applicationError = response.body<ApplicationError>()
      assertThat(applicationError).isInstanceOf(UnexpectedApplicationError::class).all {
        prop(UnexpectedApplicationError::exceptionType).isEqualTo("java.lang.RuntimeException")
        prop(UnexpectedApplicationError::fallbackMessage).isNull()
      }
    }

    client.get("/invalidQueryParameter") {
      accept(ContentType.Application.Json)
    }.let { response ->
      assertThat(response).isBadRequest()

      val applicationError = response.body<ApplicationError>()
      assertThat(applicationError.fallbackMessage).isEqualTo("Invalid value [abc] for query parameter [limit]")
    }

    client.get("/dataIntegrity") {
      accept(ContentType.Application.Json)
    }.let { response ->
      assertThat(response).isInternalServerError()

      val applicationError = response.body<ApplicationError>()
      assertThat(applicationError).isInstanceOf(UnexpectedApplicationError::class)
      assertThat(applicationError.fallbackMessage).isNull()
    }
  }

  private class SourceEntity
  private class TargetEntity

  @JvmInline
  private value class TestId(override val uuid: NativeUuid) : ValueClassUuid {
    override fun toString(): String = uuid.toHexDashString()
  }

  companion object {
    private val uuidA = NativeUuid.parse("11111111-1111-1111-1111-111111111111")
    private val uuidB = NativeUuid.parse("22222222-2222-2222-2222-222222222222")
  }
}
