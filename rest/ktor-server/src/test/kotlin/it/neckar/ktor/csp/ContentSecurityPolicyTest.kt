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
package it.neckar.ktor.csp

import assertk.*
import assertk.assertions.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.open.http.Url
import it.neckar.open.test.utils.DisableLogging
import org.junit.jupiter.api.Test

@DisableLogging
class ContentSecurityPolicyTest {
  private fun Application.setupRoutes() {
    routing {
      get("/api/widget/books") { call.respondText("books") }
    }
  }

  /**
   * The baseline a service gets without configuring anything, pinned in full: `frame-ancestors`,
   * `base-uri` and `form-action` do not fall back to `default-src`, so a service that never names
   * them is unprotected against framing, base-tag injection and form hijacking. `object-src` does
   * fall back — to `'self'`, which still allows same-origin plugin content — so it is forbidden
   * outright, spelled as the empty source list.
   */
  @Test
  fun `applies the baseline every service starts with`() = testApplication {
    application {
      installContentSecurityPolicy {
        mode = CspMode.Enforce
      }
      setupRoutes()
    }

    val policy = client.get("/api/widget/books").headers["Content-Security-Policy"]

    assertThat(policy).isNotNull()
      .isEqualTo("default-src 'self'; frame-ancestors 'self'; base-uri 'self'; form-action 'self'; object-src")
  }

  /**
   * A default is a starting point, not a fixture: a service that must be framable can say so.
   */
  @Test
  fun `lets a service replace a defaulted directive`() = testApplication {
    application {
      installContentSecurityPolicy {
        mode = CspMode.Enforce
        directive(CspDirective.FrameAncestors, CspSource.Origin("https://embedder.example.com"))
      }
      setupRoutes()
    }

    val policy = client.get("/api/widget/books").headers["Content-Security-Policy"]

    assertThat(policy).isNotNull().contains("frame-ancestors https://embedder.example.com")
  }

  @Test
  fun `reports without enforcing by default`() = testApplication {
    application {
      installContentSecurityPolicy()
      setupRoutes()
    }

    val response = client.get("/api/widget/books")

    assertThat(response.headers["Content-Security-Policy-Report-Only"]).isNotNull()
    assertThat(response.headers["Content-Security-Policy"]).isNull()
  }

  /**
   * A stored nonce is a reused nonce — whoever saw the document once knows the value an injected
   * `<script>` needs to carry. A per-response nonce is only worth anything if the response it
   * belongs to is not served twice.
   */
  @Test
  fun `marks nonce-bearing html as non-cacheable`() = testApplication {
    application {
      installContentSecurityPolicy()
      routing {
        get("/api/widget/quote-html") { call.respondNonceHtml("<html lang=\"en\"><body></body></html>") }
      }
    }

    val response = client.get("/api/widget/quote-html")

    assertThat(response.headers[HttpHeaders.CacheControl]).isNotNull().contains("no-store")
    assertThat(response.headers[HttpHeaders.ContentType]).isNotNull().contains("text/html")
  }

  @Test
  fun `generates a fresh nonce per request`() = testApplication {
    application {
      installContentSecurityPolicy()
      routing {
        get("/api/widget/quote-html") { call.respondText(cspNonce().value) }
      }
    }

    val first = client.get("/api/widget/quote-html").bodyAsText()
    val second = client.get("/api/widget/quote-html").bodyAsText()

    assertThat(first).isNotEqualTo(second)
  }

  @Test
  fun `announces exactly the nonce the handler received`() = testApplication {
    application {
      installContentSecurityPolicy {
        mode = CspMode.Enforce
        directive(CspDirective.ScriptSrc, CspSource.Self, CspSource.Nonce)
      }
      routing {
        get("/api/widget/quote-html") { call.respondText(cspNonce().value) }
      }
    }

    val response = client.get("/api/widget/quote-html")
    val nonceHandedToTheHandler = response.bodyAsText()

    assertThat(response.headers["Content-Security-Policy"])
      .isNotNull()
      .contains("script-src 'self' 'nonce-$nonceHandedToTheHandler'")
  }

  /**
   * A third-party page a service merely hosts — an API reference UI, say — cannot be made to fit
   * the service's own policy, and widening it would hand the whole service what one page needs.
   */
  @Test
  fun `serves no policy below an exempt path prefix`() = testApplication {
    application {
      installContentSecurityPolicy {
        exemptPathPrefixes = listOf(Url.rootRelative("/api/widget/doc"))
      }
      routing {
        get("/api/widget/books") { call.respondText("books") }
        get("/api/widget/doc") { call.respondText("api reference index") }
        get("/api/widget/doc/api") { call.respondText("api reference") }
      }
    }

    assertThat(client.get("/api/widget/doc").headers["Content-Security-Policy-Report-Only"]).isNull()
    assertThat(client.get("/api/widget/doc/api").headers["Content-Security-Policy-Report-Only"]).isNull()
    assertThat(client.get("/api/widget/books").headers["Content-Security-Policy-Report-Only"]).isNotNull()
  }

  /**
   * The prefix matches on segment boundaries: exempting `/api/widget/doc` must not strip the policy
   * from `/api/widget/doctor`.
   */
  @Test
  fun `keeps the policy on a sibling path that merely extends the exempt prefix`() = testApplication {
    application {
      installContentSecurityPolicy {
        exemptPathPrefixes = listOf(Url.rootRelative("/api/widget/doc"))
      }
      routing {
        get("/api/widget/doctor") { call.respondText("not the doc tree") }
      }
    }

    assertThat(client.get("/api/widget/doctor").headers["Content-Security-Policy-Report-Only"]).isNotNull()
  }
}
