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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.open.time.now
import org.junit.jupiter.api.Test

class VerifiedWebhooksTest {

  @Test
  fun `hands a verified delivery and its body to the handler`() = testApplication {
    val received: MutableList<String> = mutableListOf()
    installWebhookRoute(StandardWebhooksVerifier(Secret), received)

    val response: HttpResponse = client.postSigned(Payload, Secret)

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    assertThat(received).containsExactly(Payload)
  }

  @Test
  fun `answers 401 and skips the handler for an unsigned delivery`() = testApplication {
    val received: MutableList<String> = mutableListOf()
    installWebhookRoute(StandardWebhooksVerifier(Secret), received)

    val response: HttpResponse = client.post("/hook") { setBody(Payload) }

    assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(received).isEmpty()
  }

  @Test
  fun `leaves a route outside the guard unverified`() = testApplication {
    routing {
      verifiedWebhooks(RejectEveryWebhook) {
        post("/hook") { call.respond(HttpStatusCode.OK) }
      }
      post("/open") { call.respond(HttpStatusCode.OK) }
    }

    assertThat(client.post("/open") { setBody(Payload) }.status).isEqualTo(HttpStatusCode.OK)
  }

  @Test
  fun `gives each guard of one parent its own verifier`() = testApplication {
    routing {
      verifiedWebhooks(StandardWebhooksVerifier(Secret)) {
        post("/hook") { call.respond(HttpStatusCode.OK) }
      }
      verifiedWebhooks(RejectEveryWebhook) {
        post("/closed") { call.respond(HttpStatusCode.OK) }
      }
    }

    assertThat(client.postSigned(Payload, Secret).status).isEqualTo(HttpStatusCode.OK)
    assertThat(client.post("/closed") { setBody(Payload) }.status).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test
  fun `leaves a call another plugin already answered untouched`() = testApplication {
    var verifications = 0
    install(createApplicationPlugin("AnswersFirst") { onCall { call -> call.respond(HttpStatusCode.TooManyRequests) } })
    routing {
      verifiedWebhooks({ _, _, _ -> verifications++; WebhookVerdict.Accepted }) {
        post("/hook") { call.respond(HttpStatusCode.OK) }
      }
    }

    assertThat(client.post("/hook") { setBody(Payload) }.status).isEqualTo(HttpStatusCode.TooManyRequests)
    assertThat(verifications).isEqualTo(0)
  }

  @Test
  fun `fails a handler outside the guard that asks for the verified body`() = testApplication {
    routing {
      post("/open") { call.respond(call.verifiedWebhookBody().size.toString()) }
    }

    assertThat(client.post("/open") { setBody(Payload) }.status).isEqualTo(HttpStatusCode.InternalServerError)
  }

  private fun ApplicationTestBuilder.installWebhookRoute(verifier: WebhookVerifier, received: MutableList<String>) {
    routing {
      verifiedWebhooks(verifier) {
        post("/hook") {
          received.add(call.verifiedWebhookBody().decodeToString())
          call.respond(HttpStatusCode.OK)
        }
      }
    }
  }

  private suspend fun HttpClient.postSigned(payload: String, secret: StandardWebhooksSecret): HttpResponse = post("/hook") {
    val webhookTimestamp: String = now().epochSeconds.toString()
    header(StandardWebhooksVerifier.HeaderWebhookId, "msg_test")
    header(StandardWebhooksVerifier.HeaderWebhookTimestamp, webhookTimestamp)
    header(StandardWebhooksVerifier.HeaderWebhookSignature, secret.computeSignatureEntry(WebhookId("msg_test"), webhookTimestamp, payload.toByteArray()))
    setBody(payload)
  }

  private companion object {
    const val Payload: String = """{"type": "test", "umlaut": "ä"}"""
    val Secret: StandardWebhooksSecret = StandardWebhooksSecret("whsec_" + "A".repeat(43) + "=")
  }
}
