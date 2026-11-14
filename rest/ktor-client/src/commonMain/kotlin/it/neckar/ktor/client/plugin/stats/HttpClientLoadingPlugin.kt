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
package it.neckar.ktor.client.plugin.stats

import it.neckar.open.time.nowMillis
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.util.*
import io.ktor.utils.io.*

/**
 * Configuration class for [HttpClientLoadingPlugin].
 *
 * @property loadingCallback the [LoadingCallback] to be called on send and receive requests.
 */
class HttpClientLoadingPluginConfig {
  /**
   * The callback that is notified
   */
  var loadingCallback: LoadingCallback? = null
}


/**
 * A plugin that intercepts HTTP requests and notifies a [LoadingCallback] when they are sent and completed.
 *
 * @property loadingCallback the [LoadingCallback] to be called on send and receive requests.
 */
@KtorDsl
class HttpClientLoadingPlugin private constructor(
  val loadingCallback: LoadingCallback,
) {
  companion object Plugin : HttpClientPlugin<HttpClientLoadingPluginConfig, HttpClientLoadingPlugin> {
    /**
     * The context objects for the requests that have been sent.
     * The context is the only object that is the same for before and receive events
     */
    private val sentRequestContexts2Descriptor: MutableMap<HttpRequestBuilder, PendingRequestDescriptor> = mutableMapOf() //TODO synchronize(?)

    override val key: AttributeKey<HttpClientLoadingPlugin> = AttributeKey("HttpClientLoadingPlugin")

    override fun prepare(block: HttpClientLoadingPluginConfig.() -> Unit): HttpClientLoadingPlugin {
      val config = HttpClientLoadingPluginConfig().apply(block)

      val loadingCallback = requireNotNull(config.loadingCallback) {
        "No loadingCallback set"
      }
      return HttpClientLoadingPlugin(loadingCallback)
    }

    override fun install(plugin: HttpClientLoadingPlugin, scope: HttpClient) {
      scope.sendPipeline.intercept(HttpSendPipeline.Before) {
        val context = this@intercept.context
        val pendingRequestDescriptor = PendingRequestDescriptor(context.url.build(), context.method, nowMillis())
        plugin.loadingCallback.requestSent(pendingRequestDescriptor)

        sentRequestContexts2Descriptor[context] = pendingRequestDescriptor
      }

      scope.sendPipeline.intercept(HttpSendPipeline.Receive) {
        val context = this@intercept.context

        val descriptor = sentRequestContexts2Descriptor.remove(context) ?: throw IllegalStateException("Received request with context $context which cannot be found in the list of started requests.")

        plugin.loadingCallback.requestCompleted(descriptor.receivedAt(nowMillis()))
      }
    }
  }
}

/**
 * A callback interface that is called when an HTTP request is sent and completed.
 */
interface LoadingCallback {
  /**
   * Called when an HTTP request has been sent.
   *
   * @param request the intercepted request.
   */
  fun requestSent(request: PendingRequestDescriptor)

  /**
   * Called when an HTTP request has completed.
   *
   * @param request the intercepted request.
   */
  fun requestCompleted(request: CompletedRequestDescriptor)
}

