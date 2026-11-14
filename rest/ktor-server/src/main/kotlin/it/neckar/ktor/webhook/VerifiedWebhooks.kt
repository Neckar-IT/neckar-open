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
package it.neckar.ktor.webhook

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import it.neckar.ktor.TransparentRouteSelector
import it.neckar.ktor.proxy.forwardedClientAddress
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.time.now

/**
 * Guards the webhook routes defined in [build] with [verifier]: a delivery it rejects gets a 401 and a warning in the
 * log, and no handler below runs. A handler reads the verified body with [verifiedWebhookBody].
 *
 * ```
 * verifiedWebhooks(StandardWebhooksVerifier(secret)) {
 *   post("/api/events") { dispatch(call.verifiedWebhookBody().decodeToString()) }
 * }
 * ```
 */
fun Route.verifiedWebhooks(verifier: WebhookVerifier, build: Route.() -> Unit): Route {
  // A new selector per call: Ktor reuses the child of an equal selector, and a second plugin install on it throws.
  val guarded = createChild(WebhookRouteSelector())
  guarded.install(WebhookVerificationPlugin) { this.verifier = verifier }
  guarded.build()
  return guarded
}

/**
 * The body [verifiedWebhooks] read and verified; the plugin consumed the request stream, so the handler cannot receive it again.
 */
fun ApplicationCall.verifiedWebhookBody(): ByteArray =
  attributes.getOrNull(VerifiedBodyKey) ?: error("${request.uri} is no route inside verifiedWebhooks { }")

private val VerifiedBodyKey: AttributeKey<ByteArray> = AttributeKey("VerifiedWebhookBody")

private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.webhook.VerifiedWebhooks")

private class WebhookVerificationConfig {
  lateinit var verifier: WebhookVerifier
}

private val WebhookVerificationPlugin = createRouteScopedPlugin("WebhookVerification", ::WebhookVerificationConfig) {
  val verifier: WebhookVerifier = pluginConfig.verifier

  onCall { call ->
    if (call.isHandled) return@onCall
    val body: ByteArray = call.receive<ByteArray>()
    when (val verdict: WebhookVerdict = verifier.verify(call.request.headers::get, body, receivedAt = now())) {
      WebhookVerdict.Accepted -> call.attributes.put(VerifiedBodyKey, body)
      is WebhookVerdict.Rejected -> {
        logger.warn("Rejected webhook delivery to ${call.request.uri} from ${call.request.forwardedClientAddress()}: $verdict")
        call.respond(HttpStatusCode.Unauthorized)
      }
    }
  }
}

private class WebhookRouteSelector : RouteSelector(), TransparentRouteSelector {
  override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
    RouteSelectorEvaluation.Transparent

  override fun toString(): String = "(verified webhooks)"
}
