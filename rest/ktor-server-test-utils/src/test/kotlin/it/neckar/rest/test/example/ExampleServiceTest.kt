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
package it.neckar.rest.test.example

import assertk.*
import assertk.assertions.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import it.neckar.ktor.client.get
import it.neckar.open.http.Url
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.test.assertData
import it.neckar.rest.test.createTestClient
import it.neckar.rest.test.isBad
import it.neckar.rest.test.isGood
import it.neckar.rest.test.launchServiceInTest
import org.junit.jupiter.api.Test

@DisableLogging
class ExampleServiceTest {
  @Test
  fun testRequestResponsePatternSuccess() = testApplication {
    launchServiceInTest(ExampleService())

    val client = createTestClient {
      //additional setup - if necessary
    }

    val foobar = ExampleService.FooBar("foo", "bar")

    client.post<ExampleService.FooBarResponse>(Url.localhostHttp + Url.rootRelative("/foobar"), ExampleService.FooBarRequest(foobar))
      .isGood() // verify *our* success response, *NOT* the HTTP status code!
      .assertData() //extract the [data] from the response!
      .isEqualTo(foobar) //verify the data, *NOT* the response object!
  }

  @Test
  fun testRequestResponsePatternFailure() = testApplication {
    launchServiceInTest(ExampleService())

    val client = createTestClient {
      //additional setup - if necessary
    }

    val foobar = ExampleService.FooBar("willFail", "bar", true)

    client.post<ExampleService.FooBarResponse>(Url.localhostHttp + Url.rootRelative("/foobar"), ExampleService.FooBarRequest(foobar))
      .isBad(ExampleService.FooBarResponse.Failed) // verify *our* success response, *NOT* the HTTP status code!
  }

  /**
   * Low-level test that uses the KTor client directly.
   * Helpful for some very basic, low-level end points
   */
  @Test
  fun testLowLevel() = testApplication {
    launchServiceInTest(ExampleService())

    val client = createTestClient {
      //additional setup - if necessary
    }

    //Low-level test using the Ktor client
    client.httpClient.get(Url.localhostHttp + Url.rootRelative("/hello/world")).let { httpResponse ->
      assertThat(httpResponse.status.isSuccess()).isTrue()
      assertThat(httpResponse.bodyAsText()).isEqualTo("Hello World!")
    }
  }
}
