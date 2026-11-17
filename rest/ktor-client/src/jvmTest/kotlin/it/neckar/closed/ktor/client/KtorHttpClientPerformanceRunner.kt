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
package it.neckar.closed.ktor.client

import assertk.*
import assertk.assertions.*
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.apache.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.neckar.commons.logback.LogbackConfigurer
import it.neckar.ktor.client.createHttpClient
import it.neckar.open.kotlin.lang.fastFor
import kotlinx.coroutines.*
import org.slf4j.event.Level
import kotlin.system.measureTimeMillis

fun main(): Unit = runBlocking {
  LogbackConfigurer.setLoggerLevel("io.ktor.client", Level.INFO)
  LogbackConfigurer.setLoggerLevel("ktor.http.client", Level.INFO)

  println()
  println("####################################################################################")
  println("CIO")
  println("####################################################################################")
  runTestConnections("https://www.heise.de", CIO) {
    engine {
      //this.maxConnectionsCount = 100
    }
  }.let {
    println("##############################################")
    printResult(it)
  }

  //withoutCloseTime: 10737
  //withCloseTime: 11727

  println()
  println("####################################################################################")
  println("OkHttp")
  println("####################################################################################")
  runTestConnections("https://www.heise.de", OkHttp) {
    engine {
      config {
      }
    }
  }.let {
    println("##############################################")
    printResult(it)
  }

  //withoutCloseTime: 1833
  //withCloseTime: 1614

  println()
  println("####################################################################################")
  println("Apache")
  println("####################################################################################")
  runTestConnections("https://www.heise.de", Apache) {
    engine {
      customizeClient {
        setMaxConnTotal(100) // Example configuration, adjust as needed
        setMaxConnPerRoute(100)
        //// Enable HTTP/2
        //versionPolicy = org.apache.hc.core5.http.HttpVersionPolicy.FORCE_HTTP_2
      }
    }
  }.let {
    println("##############################################")
    printResult(it)
  }

  //withoutCloseTime: 4894
  //withCloseTime: 4574
}

internal fun printResult(result: MyResult) {
  println("withoutCloseTime: ${result.withoutCloseTime}")
  println("withCloseTime: ${result.withCloseTime}")
  println("httpVersions: ${result.httpVersions}")

}

private suspend fun <T : HttpClientEngineConfig> runTestConnections(url: String, engine: HttpClientEngineFactory<T>, config: HttpClientConfig<out T>.() -> Unit = {}): MyResult {
  val client = createHttpClient(engine, "test", config)
  //assertThat(client.engine::class.qualifiedName.orEmpty()).contains("Apache")

  val count = 50

  val httpVersions = mutableSetOf<HttpProtocolVersion>()

  val withoutCloseTime = measureTimeMillis {
    count.fastFor {
      val response = client.get(url)
      httpVersions.add(response.version)
      assertThat(response).isInstanceOf(HttpResponse::class)
      print(".")
    }
  }

  println()

  val withCloseTime = measureTimeMillis {
    count.fastFor {
      val response = client.get(url)
      assertThat(response).isInstanceOf(HttpResponse::class)
      response.readBytes() //read the body to close the connection(?)
      print(".")
    }
  }

  println()

  return MyResult(httpVersions, withoutCloseTime, withCloseTime)
}

internal data class MyResult(val httpVersions: MutableSet<HttpProtocolVersion>, val withoutCloseTime: Long, val withCloseTime: Long) {

}


