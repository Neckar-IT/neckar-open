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
package it.neckar.problem

import it.neckar.open.exceptions.ErrorCode
import it.neckar.open.serialization.roundTrip
import it.neckar.open.tracing.TraceId
import org.junit.jupiter.api.Test

/**
 *
 */
class ApplicationErrorTest {
  @Test
  fun testJsonUnexpected() {
    roundTrip<ApplicationError>(
      UnexpectedApplicationError(
        fallbackMessage = "the message",
        instanceUri = "the instance uri",
        exceptionType = "the exception type",
        traceId = TraceId("0af7651916cd43dd8448eb211c80319c"),
      )
    ) {
      //language=JSON
      """
        {
          "type" : "UnexpectedError",
          "fallbackMessage" : "the message",
          "exceptionType" : "the exception type",
          "instanceUri" : "the instance uri",
          "traceId" : "0af7651916cd43dd8448eb211c80319c"
        }
      """.trimIndent()
    }
  }

  @Test
  fun testJsonExpected() {
    roundTrip<ApplicationError>(
      ExpectedApplicationError(
        fallbackMessage = "the fallbackMessage",
        instanceUri = "the instance uri",
        errorCode = ErrorCode.parse("AS-123"),
        attributes = mapOf("key" to "value"),
      )
    ) {
      //language=JSON
      """
        {
          "type" : "ExpectedError",
          "errorCode" : "AS-123",
          "fallbackMessage" : "the fallbackMessage",
          "instanceUri" : "the instance uri",
          "attributes" : {
            "key" : "value"
          },
          "traceId" : null
        }
      """.trimIndent()
    }
  }
}
