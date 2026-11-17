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
package it.neckar.ktor.tracking

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import it.neckar.rest.test.*
import it.neckar.open.tenancy.Tenant
import it.neckar.open.tenancy.TenantDescriptor
import it.neckar.rest.jwt.JwtTokenClaim
import it.neckar.rest.jwt.JwtTokenUsageType
import it.neckar.rest.jwt.RefreshTokenChainId
import it.neckar.ktor.jwt.withClaim
import it.neckar.ktor.jwt.withTenantDescriptor
import it.neckar.open.tenancy.Slug
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class OtelIdentityPluginTest {

  private val jwtSecret = "test-secret-for-otel-identity"
  private val jwtIssuer = "test-issuer"
  private val jwtAudience = "test-audience"
  private val tenantId = Uuid.parse("a1b45679-e0d3-42be-9fb7-fd40cc12ba0e")
  private val tenantSlug = Slug("test-tenant")

  private fun createTestToken(loginName: String): String {
    return JWT.create()
      .withSubject(loginName)
      .withIssuer(jwtIssuer)
      .withAudience(jwtAudience)
      .withClaim(JwtTokenClaim.TokenUsageType, JwtTokenUsageType.Access)
      .withClaim(JwtTokenClaim.RefreshTokenChainId, RefreshTokenChainId(tenantId))
      .withTenantDescriptor(TenantDescriptor(Tenant.Id(tenantId), tenantSlug))
      .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
      .sign(Algorithm.HMAC256(jwtSecret))
  }

  @Test
  fun `sets span attributes from JWT identity`() {
    val spanExporter = InMemorySpanExporter.create()
    val tracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
      .build()
    val openTelemetry = OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .build()

    testApplication {
      application {
        install(KtorServerTelemetry) {
          setOpenTelemetry(openTelemetry)
        }

        install(Authentication) {
          jwt {
            verifier(
              JWT.require(Algorithm.HMAC256(jwtSecret))
                .withIssuer(jwtIssuer)
                .withAudience(jwtAudience)
                .build()
            )
            validate { credential ->
              JWTPrincipal(credential.payload)
            }
          }
        }

        install(OtelIdentityPlugin)

        routing {
          authenticate {
            get("/api/test") {
              call.respondText("ok")
            }
          }
        }
      }

      val token = createTestToken("demo")
      val response = client.get("/api/test") {
        header(HttpHeaders.Authorization, "Bearer $token")
      }
      assertThat(response).isOk()
    }

    val spans = spanExporter.finishedSpanItems
      .filter { it.kind == SpanKind.SERVER }
    assertThat(spans).hasSize(1)

    val span = spans.single()
    val attributes = span.attributes.asMap()
    assertThat(attributes[io.opentelemetry.api.common.AttributeKey.stringKey("enduser.id")]).isEqualTo("demo")
    assertThat(attributes[io.opentelemetry.api.common.AttributeKey.stringKey("tenant.id")]).isEqualTo(tenantId.toString())
  }

  @Test
  fun `does not set attributes for unauthenticated requests`() {
    val spanExporter = InMemorySpanExporter.create()
    val tracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
      .build()
    val openTelemetry = OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .build()

    testApplication {
      application {
        install(KtorServerTelemetry) {
          setOpenTelemetry(openTelemetry)
        }

        install(OtelIdentityPlugin)

        routing {
          get("/api/public") {
            call.respondText("public")
          }
        }
      }

      val response = client.get("/api/public")
      assertThat(response).isOk()
    }

    val spans = spanExporter.finishedSpanItems
      .filter { it.kind == SpanKind.SERVER }
    assertThat(spans).hasSize(1)

    val span = spans.single()
    val attributes = span.attributes.asMap()
    assertThat(attributes[io.opentelemetry.api.common.AttributeKey.stringKey("enduser.id")]).isEqualTo(null)
    assertThat(attributes[io.opentelemetry.api.common.AttributeKey.stringKey("tenant.id")]).isEqualTo(null)
  }
}
