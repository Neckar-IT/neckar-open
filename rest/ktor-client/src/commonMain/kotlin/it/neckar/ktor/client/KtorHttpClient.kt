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
package it.neckar.ktor.client

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import it.neckar.logging.LoggerFactory
import it.neckar.logging.debug

/**
 * Creates a http client.
 *
 * ATTENTION: Sets expectSuccess to true
 */
fun createHttpClient(engine: HttpClientEngine?, clientName: String, config: HttpClientConfig<*>.() -> Unit = {}): HttpClient {
  //A caller that has no engine of its own passes null: the engine then comes off the classpath.
  if (engine == null) {
    return createHttpClient(clientName, config)
  }

  return HttpClient(engine) {
    defaultConfig(clientName, config)
  }
}

fun <T : HttpClientEngineConfig> createHttpClient(engine: HttpClientEngineFactory<T>, clientName: String, configBlock: HttpClientConfig<T>.() -> Unit = {}): HttpClient {
  return HttpClient(engine) {
    defaultConfig(clientName, configBlock)
  }
}

/**
 * Default configuration that is called for every client
 */
private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.defaultConfig(clientName: String, config: HttpClientConfig<T>.() -> Unit) {
  //Call expect success first to allow overriding in the config block
  expectSuccess = true

  config()

  //Apply default plugins.
  //Plugins are only applied once, the second time they are ignored

  install(ContentNegotiation) {
    json()
  }

  install(Logging) {
    logger = object : Logger {
      val internalLogger: it.neckar.logging.Logger = LoggerFactory.getLogger("ktor.http.client.$clientName")

      override fun log(message: String) {
        internalLogger.debug { message }
      }
    }
  }
}

/**
 * Creates an http client with the default configuration applied as well as the given configuration.
 *
 * See `setupTestClient` for setting up a client for tests.
 */
fun createHttpClient(clientName: String, config: HttpClientConfig<*>.() -> Unit = {}): HttpClient {
  //No engine is handed over: Ktor picks the one on the classpath, and jvmMain's HttpClientJvm.FACTORY is private.
  return HttpClient {
    defaultConfig(clientName, config)
  }
}
