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

import it.neckar.projects.common.ServiceName
import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.neckar.commons.logback.LogbackConfigurer
import it.neckar.commons.logback.debug
import it.neckar.ktor.jwt.JwtSigningKey
import it.neckar.ktor.jwt.AuthProviderName
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.ktor.jwt.addJwtHeaderAuth
import it.neckar.ktor.jwt.addJwtCookieAuth
import it.neckar.ktor.jwt.jwtPrincipal
import it.neckar.ktor.jwt.jwtToken
import it.neckar.ktor.jwt.withAudience
import it.neckar.ktor.jwt.withClaim
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.exceptions.PermissionDeniedException
import it.neckar.open.http.Url
import it.neckar.projects.common.Port
import it.neckar.projects.common.ServiceDescriptor
import it.neckar.rest.jwt.JwtAudience
import it.neckar.rest.jwt.JwtTokenClaim
import it.neckar.rest.jwt.JwtTokenUsageType
import it.neckar.uuid.pseudoRandom
import org.slf4j.event.Level
import java.time.Instant
import kotlin.uuid.Uuid

private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.KtorExampleServer")

fun main() {
  LogbackConfigurer.setRootLoggerLevel(Level.DEBUG)

  logger.info("Starting Demo server")

  launchService(
    service = DemoService(),
    port = Port(9999),
  )
}

class DemoService(
  /**
   * Optional port override. If provided, this port will be used instead of the one defined in the service descriptor.
   * Useful for tests where the server runs on a random port.
   */
  portOverride: Port? = null,
  ) : KtorService {
  private val cookieAuthName = AuthProviderName("cookieAuth")

  private val audience = JwtAudience("DemoAudience")
  private val jwsKey = JwtSigningKey.HMAC256("the demo secret")

  override val serviceDescriptor: ServiceDescriptor = object : ServiceDescriptor {
    override val serviceName: ServiceName = ServiceName("DemoService")

    override
    val apiPrefix: String = ""

    override
    val port: Port = Port(9999)
  }

  override val baseUrl: Url.Absolute = serviceDescriptor.baseUrl(portOverride)

  override fun Application.configure() {
    logger.info("Starting Demo Service")

    installPlugins()
    install(StatusPages) {
      exception<PermissionDeniedException> { call, cause ->
        logger.info("Permission denied", cause)

        val message: String = cause.message.orEmpty()
        call.respond(HttpStatusCode.Forbidden, message)
      }
    }

    metaRouting(service = serviceDescriptor, baseUrl = Url.pattern("meta"), authenticationRequirement = AuthenticationRequirement.None)
    metricsRouting(baseUrl = Url.pattern(""))

    routing {
      get {
        call.respondText("Demo Service up and running!")
      }

      route("") {
        staticResources("resources-test", "assets", index = "test.html")
      }

      get("login") {
        val now = Instant.now()
        val token = JWT.create()
          .withJWTId(Uuid.pseudoRandom().toHexDashString())
          .withIssuedAt(now)
          .withIssuer("auth.pv.neckar.it")
          .withSubject("Subject for Demo Server")
          .withAudience(audience)
          .withClaim("DemoClaim", "Demo-Clain-Value")
          .withClaim(JwtTokenClaim.TokenUsageType, JwtTokenUsageType.Access)
          .withNotBefore(now)
          .withExpiresAt(now.plusSeconds(60 * 10))
          .sign(jwsKey)

        logger.debug("Generated token: $token")

        call.respond(token)
      }

      authenticate {
        get("with-jwt-auth") {
          val jwtPrincipal = call.jwtPrincipal()
          val authorizationHeader = call.request.authorizationHeader()
          val jwtToken = call.jwtToken()

          call.respondText(buildString {
            appendLine("with jwt authentication for audience: ${jwtPrincipal.payload.audience}")
            appendLine("authorization header: $authorizationHeader")
            appendLine("jwtToken: $jwtToken")
          })
        }
      }

      authenticate(cookieAuthName.value) {
        get("with-jwt-cookie-auth") {
          val jwsToken = call.jwtPrincipal()
          call.respondText(buildString {
            appendLine("with jwt cooke authentication: for audience: ${jwsToken.payload.audience}")
          })
        }
      }

      authenticate {
        authenticate(cookieAuthName.value) {
          get("with-both-auth") {
            val jwsToken = call.jwtPrincipal()
            call.respondText("with both: for audience: ${jwsToken.payload.audience}")
          }
        }
      }
    }

    logger.info("Demo Service started")
    logger.debug { "Port: ${port()}" }
    logger.debug {
      dumpRoutes()
    }
  }

  /**
   * Module for initial server settings like headers
   * authentication and json support
   * */
  fun Application.installPlugins() {
    installCallId()
    installCallLogging()
    installCompression()
    install(DefaultHeaders) {
    }

    install(Authentication) {
      addJwtHeaderAuth(audience = audience, tokenUsageType = JwtTokenUsageType.Access, jwtSigningKeyProvider = { jwsKey })
      addJwtCookieAuth(cookieAuthName.value, audience = audience, tokenUsageType = JwtTokenUsageType.Access) { jwsKey }
    }

    installIgnoreTailingSlash()
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.DemoService")

    val serviceDescriptor: ServiceDescriptor = object : ServiceDescriptor {
      override val serviceName: ServiceName = ServiceName("DemoService")

      override
      val apiPrefix: String = ""

      override
      val port: Port = Port(9999)
    }
  }
}
