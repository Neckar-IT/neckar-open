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
package it.neckar.ktor.client.heartbeat

import assertk.*
import assertk.assertions.*
import assertk.assertions.support.*
import it.neckar.heartbeat.HeartbeatState

// Extension function to assert that a HeartbeatState is Alive
fun Assert<HeartbeatState>.isAlive() = given { actual ->
  if (actual is HeartbeatState.Alive) return@given
  expected("to be Alive but was ${actual::class.simpleName}")
}

// Extension function to assert that a HeartbeatState is Dead.ConnectionFailure
fun Assert<HeartbeatState>.isConnectionFailure() = given { actual ->
  if (actual is HeartbeatState.Dead.ConnectionFailure) return@given
  expected("to be ConnectionFailure but was ${actual::class.simpleName}")
}

// Extension function to assert that a HeartbeatState is Dead.VersionMismatch
fun Assert<HeartbeatState>.isVersionMismatch() = given { actual ->
  if (actual is HeartbeatState.Dead.VersionMismatch) return@given
  expected("to be VersionMismatch but was ${actual::class.simpleName}")
}

// Extension function to assert that a HeartbeatState is Dead.ErrorResponse
fun Assert<HeartbeatState>.isErrorResponse(statusCode: Int? = null, message: String? = null) = given { actual ->
  if (actual is HeartbeatState.Dead.ErrorResponse) {
    statusCode?.let { assertThat(actual.statusCode, "statusCode").isEqualTo(it) }
    message?.let { assertThat(actual.message, "message").isEqualTo(it) }
    return@given
  }
  expected("to be ErrorResponse but was ${actual::class.simpleName}")
}

// Extension function to assert that a HeartbeatState is Dead.ExceptionOccurred
fun Assert<HeartbeatState>.isExceptionOccurred(exception: Throwable) = given { actual ->
  if (actual is HeartbeatState.Dead.ExceptionOccurred) {
    assertThat(actual.exception).isEqualTo(exception)
    return@given
  }
  expected("to be ExceptionOccurred but was ${actual::class.simpleName}")
}
