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

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.uri
import io.ktor.server.response.*
import io.ktor.util.cio.ChannelWriteException
import it.neckar.ktor.tracking.currentTraceId
import it.neckar.open.authorization.MissingCapabilityException
import it.neckar.open.exceptions.ApplicationException
import it.neckar.open.exceptions.DataIntegrityException
import it.neckar.open.exceptions.PermissionDeniedException
import it.neckar.open.exceptions.exceptionTypeName
import it.neckar.problem.ApplicationError
import it.neckar.problem.ExpectedApplicationError
import it.neckar.problem.UnexpectedApplicationError
import it.neckar.problem.toExpectedProblem
import it.neckar.rest.InvalidQueryParameterException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.json.JsonEncodingException
import org.slf4j.Logger


/**
 * Installs the status pages - including the additional configuration
 */
// Every handler here answers an exception: a malformed request, a missing credential, a missing route.
@Suppress("Status4xxIsAProtocolError")
fun Application.installStatusPages(logger: Logger, config: StatusPagesConfig.() -> Unit = {}) {
  install(StatusPages) {
    //Handling well-known exceptions, defined by us
    exception<it.neckar.open.exceptions.NotFoundException> { call, cause ->
      logger.info("Not found", cause)
      // A 404 is reserved for missing routes and binary endpoints without a target.
      // NotFoundException is legacy: an endpoint answers a missing entity with bad/NotFound.
      call.respondUnexpectedProblem(HttpStatusCode.BadRequest, cause)
    }
    // Capability-specific 403 — registered BEFORE the generic PermissionDeniedException
    // handler so Ktor's most-specific-first dispatch picks it up. Carries the missing
    // capability key in `attributes.missingCapability` so the frontend can render a
    // precise "you need permission X" message.
    exception<MissingCapabilityException> { call, cause ->
      logger.info("Missing capability: ${cause.missingCapability}")

      call.respond<ApplicationError>(
        HttpStatusCode.Forbidden,
        ExpectedApplicationError(
          errorCode = MissingCapabilityException.Code,
          fallbackMessage = cause.message,
          attributes = mapOf(MissingCapabilityException.MissingCapabilityAttribute to cause.missingCapability.value),
          traceId = currentTraceId(),
        ),
      )
    }
    exception<PermissionDeniedException> { call, cause ->
      logger.info("[${cause::class.qualifiedName}]: ${cause.message}")
      call.respondUnexpectedProblem(HttpStatusCode.Forbidden, cause)
    }

    // Persistent data violates a domain invariant — log at ERROR so it surfaces in
    // Grafana/Sentry, then respond with 500. The caller cannot recover; operations
    // must investigate the broken document. The structured message is built by
    // DataIntegrityException itself.
    exception<DataIntegrityException> { call, cause ->
      logger.error("DataIntegrityException: ${cause.message}", cause)
      call.respondUnexpectedProblem(HttpStatusCode.InternalServerError, cause)
    }

    //Application exceptions (should be avoided - use [it.neckar.rest.RestResponse.BadResponse] instead!)
    exception<ApplicationException> { call, cause ->
      logger.info("Application Exception: [${cause.errorCode}] ${cause.message}")
      call.respond<ApplicationError>(HttpStatusCode.BadRequest, cause.toExpectedProblem(currentTraceId()))
    }

    //Handle exceptions that are thrown by Ktor itself
    exception<BadRequestException> { call, cause ->
      logger.warn("Bad Request: [${cause::class.qualifiedName}]: ${cause.message}", cause)
      call.respondUnexpectedProblem(HttpStatusCode.BadRequest, cause)
    }

    // A request body Ktor cannot read: no converter for the expected type, an unsupported content
    // type, a payload over the size limit. Without this handler the Throwable fallback answers 500.
    exception<ContentTransformationException> { call, cause ->
      logger.warn("Content transformation failed: ${cause.message}", cause)
      call.respondUnexpectedProblem(HttpStatusCode.BadRequest, cause)
    }

    // A failed require() is a programming error of the service; a formally invalid request has its own handler.
    exception<IllegalArgumentException> { call, cause ->
      logger.error("Illegal argument: ${cause.message}", cause)
      call.respondUnexpectedProblem(HttpStatusCode.InternalServerError, cause)
    }

    exception<InvalidQueryParameterException> { call, cause ->
      logger.info("Invalid query parameter: ${cause.message}")

      call.respondUnexpectedProblem(HttpStatusCode.BadRequest, cause)
    }

    @OptIn(ExperimentalSerializationApi::class)
    exception<JsonDecodingException> { call, cause ->
      val pathInfo = cause.path?.let { " at path $it" }.orEmpty()
      val offsetInfo = if (cause.offset >= 0) " (offset ${cause.offset})" else ""
      logger.warn("JSON Decoding Error: ${cause.shortMessage}$pathInfo$offsetInfo", cause)

      // shortMessage rather than the exception message: kotlinx.serialization appends the raw
      // request body to the latter.
      call.respond<ApplicationError>(
        HttpStatusCode.BadRequest,
        UnexpectedApplicationError(
          fallbackMessage = "${cause.shortMessage}$pathInfo$offsetInfo",
          exceptionType = cause.exceptionTypeName(),
          traceId = currentTraceId(),
        ),
      )
    }

    @OptIn(ExperimentalSerializationApi::class)
    exception<JsonEncodingException> { call, cause ->
      logger.error("JSON Encoding Error: ${cause.shortMessage}", cause)

      call.respondUnexpectedProblem(HttpStatusCode.InternalServerError, cause)
    }

    exception<SerializationException> { call, cause ->
      logger.warn("Serialization Problem: ${cause.message}", cause)

      call.respondUnexpectedProblem(HttpStatusCode.BadRequest, cause)
    }

    // The client went away mid-response — a closed tab on an event stream. Nothing to answer on a
    // dead connection, and a 500 for it buries the failures that are ours.
    exception<ChannelWriteException> { call, _ ->
      logger.debug("Client left while responding to ${call.request.uri}")
    }

    config()

    //Fallback for all other exceptions, ensure these are handled properly - and return the [Problem] object
    exception<Throwable> { call, cause ->
      logger.error("Internal Server Error", cause)
      call.respondUnexpectedProblem(HttpStatusCode.InternalServerError, cause)
    }
  }
}

