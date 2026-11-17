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
package it.neckar.ktor

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.common.featureflags.FeatureFlags
import it.neckar.rest.test.*
import org.junit.jupiter.api.Test
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import it.neckar.open.test.utils.DisableLogging

/**
 * Tests for [installProductCors]
 */
@DisableLogging
class DeploymentCorsExtensionsTest {

  @Test
  fun `CORS allows production domain`() = testApplication {
    application {
      installProductCors(TestProductService, TestProductDomain)

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    val response = client.get("/test") {
      header(HttpHeaders.Origin, "https://test-product.neckar.it")
    }

    assertThat(response).isOk()
    assertThat(response.headers[HttpHeaders.AccessControlAllowOrigin]).isEqualTo("https://test-product.neckar.it")
  }

  @Test
  fun `CORS allows dev domain`() = testApplication {
    application {
      installProductCors(TestProductService, TestProductDomain)

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    val response = client.get("/test") {
      header(HttpHeaders.Origin, "https://dev.test-product.neckar.it")
    }

    assertThat(response).isOk()
    assertThat(response.headers[HttpHeaders.AccessControlAllowOrigin]).isEqualTo("https://dev.test-product.neckar.it")
  }

  @Test
  fun `CORS allows demo domain`() = testApplication {
    application {
      installProductCors(TestProductService, TestProductDomain)

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    val response = client.get("/test") {
      header(HttpHeaders.Origin, "https://demo.test-product.neckar.it")
    }

    assertThat(response).isOk()
    assertThat(response.headers[HttpHeaders.AccessControlAllowOrigin]).isEqualTo("https://demo.test-product.neckar.it")
  }

  @Test
  fun `CORS allows localhost domain`() = testApplication {
    application {
      installProductCors(TestProductService, TestProductDomain)

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    val response = client.get("/test") {
      header(HttpHeaders.Origin, "http://test-product.localdev")
    }

    assertThat(response).isOk()
    assertThat(response.headers[HttpHeaders.AccessControlAllowOrigin]).isEqualTo("http://test-product.localdev")
  }

  @Test
  fun `CORS allows Vite dev server port`() = testApplication {
    application {
      installProductCors(TestProductService, TestProductDomain)

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    val response = client.get("/test") {
      header(HttpHeaders.Origin, "http://localhost:5173")
    }

    assertThat(response).isOk()
    assertThat(response.headers[HttpHeaders.AccessControlAllowOrigin]).isEqualTo("http://localhost:5173")
  }

  @Test
  fun `CORS allows service port`() = testApplication {
    application {
      installProductCors(TestProductService, TestProductDomain)

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    val response = client.get("/test") {
      header(HttpHeaders.Origin, "http://localhost:8080")
    }

    assertThat(response).isOk()
    assertThat(response.headers[HttpHeaders.AccessControlAllowOrigin]).isEqualTo("http://localhost:8080")
  }

  @Test
  fun `CORS allows mea portal`() = testApplication {
    application {
      installProductCors(TestProductService, TestProductDomain)

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    val response = client.get("/test") {
      header(HttpHeaders.Origin, "https://mea.neckar.it")
    }

    assertThat(response).isOk()
    assertThat(response.headers[HttpHeaders.AccessControlAllowOrigin]).isEqualTo("https://mea.neckar.it")
  }

  @Test
  fun `CORS allows IntelliJ built-in server`() = testApplication {
    application {
      installProductCors(TestProductService, TestProductDomain)

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    val response = client.get("/test") {
      header(HttpHeaders.Origin, "http://localhost:63342")
    }

    assertThat(response).isOk()
    assertThat(response.headers[HttpHeaders.AccessControlAllowOrigin]).isEqualTo("http://localhost:63342")
  }

  @Test
  fun `CORS preflight returns allowed methods and headers`() = testApplication {
    application {
      installProductCors(TestProductService, TestProductDomain)

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    val response = client.options("/test") {
      header(HttpHeaders.Origin, "https://test-product.neckar.it")
      header(HttpHeaders.AccessControlRequestMethod, "POST")
      header(HttpHeaders.AccessControlRequestHeaders, "Authorization, ${FeatureFlags.HeaderKey}")
    }

    assertThat(response).isOk()
    // Ktor CORS only lists non-simple methods (GET, POST, HEAD are simple and omitted)
    val allowMethods = response.headers[HttpHeaders.AccessControlAllowMethods]
    assertThat(allowMethods).isNotNull()
    assertThat(allowMethods!!).contains("PUT")
    assertThat(allowMethods).contains("DELETE")
    assertThat(allowMethods).contains("PATCH")

    val allowHeaders = response.headers[HttpHeaders.AccessControlAllowHeaders]
    assertThat(allowHeaders).isNotNull()
    assertThat(allowHeaders!!).contains("Authorization")
    assertThat(allowHeaders).contains(FeatureFlags.HeaderKey)
  }

  @Test
  fun `CORS additionalConfig is applied`() = testApplication {
    application {
      installProductCors(TestProductService, TestProductDomain) {
        allowHost("custom-host.example.com", listOf("https"))
      }

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    val response = client.get("/test") {
      header(HttpHeaders.Origin, "https://custom-host.example.com")
    }

    assertThat(response).isOk()
    assertThat(response.headers[HttpHeaders.AccessControlAllowOrigin]).isEqualTo("https://custom-host.example.com")
  }

  @Test
  fun `CORS rejects unknown origin`() = testApplication {
    application {
      installProductCors(TestProductService, TestProductDomain)

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    val response = client.get("/test") {
      header(HttpHeaders.Origin, "https://evil.example.com")
    }

    // CORS should not include Access-Control-Allow-Origin for unknown origins
    assertThat(response.headers[HttpHeaders.AccessControlAllowOrigin]).isEqualTo(null)
  }

  @Test
  fun `CORS works without UI descriptor`() = testApplication {
    application {
      installProductCors(TestProductNoUiService, TestProductNoUiDomain)

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    // Production domain should still work
    val response = client.get("/test") {
      header(HttpHeaders.Origin, "https://no-ui-product.neckar.it")
    }

    assertThat(response).isOk()
    assertThat(response.headers[HttpHeaders.AccessControlAllowOrigin]).isEqualTo("https://no-ui-product.neckar.it")
  }

  /**
   * The public MeisterCharts data server names no domain and lets `anyHost()` decide.
   */
  @Test
  fun `CORS without domains takes the origins from the additional config`() = testApplication {
    application {
      installProductCors(service = null) { anyHost() }

      routing {
        get("/test") {
          call.respondText("OK")
        }
      }
    }

    val response = client.get("/test") {
      header(HttpHeaders.Origin, "https://example.com")
    }

    assertThat(response).isOk()
    assertThat(response.headers[HttpHeaders.AccessControlAllowOrigin]).isEqualTo("https://example.com")
  }

  @Test
  fun `CORS without domains and without additional config fails`() = testApplication {
    application {
      assertFailure { installProductCors(service = null) }.messageContains("needs a DomainDescriptor")
    }
    startApplication()
  }
}
