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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.*
import io.ktor.server.application.*
import it.neckar.open.health.FatalReason
import it.neckar.open.health.ServiceState
import it.neckar.open.health.ServiceStateHolder
import it.neckar.open.test.utils.DisableLogging
import it.neckar.projects.common.Port
import it.neckar.projects.common.ServiceDescriptor
import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.Test
import java.net.ServerSocket

/**
 * What a failing startup sequence does to the process.
 *
 * The distinction this pins down is the reason the server binds before the sequence runs at all: a
 * database that is not up yet must end the process so the container restarts, while a migration that
 * needs a human must not — it would restart into the same wall, and liveness answers `200` for
 * `Fatal` on purpose.
 */
@DisableLogging
class StartupSequenceFailureTest {

  @Test
  fun `a failure without a recorded reason ends the process`() {
    val holder = ServiceStateHolder().apply { transitionTo(ServiceState.Verifying) }

    assertFailure {
      serviceOnFreePort().launchWithStartupSequence(holder) {
        //What a service start against a database that is not accepting connections yet looks like
        error("connection refused")
      }
    }.isInstanceOf(IllegalStateException::class).messageContains("connection refused")

    //Nothing turned this into a permanent state — the next container start is the recovery
    assertThat(holder.state).isEqualTo(ServiceState.Verifying)
  }

  /** A `withTimeout` inside the sequence throws a CancellationException without anything cancelling the start. */
  @Test
  fun `a CancellationException from the sequence itself ends the process`() {
    val holder = ServiceStateHolder().apply { transitionTo(ServiceState.Verifying) }

    assertFailure {
      serviceOnFreePort().launchWithStartupSequence(holder) {
        throw CancellationException("timed out inside the sequence")
      }
    }.isInstanceOf(IllegalStateException::class).messageContains("threw a CancellationException")
  }

  private fun serviceOnFreePort(): KtorService {
    val port = freePort()

    return object : KtorService {
      override val serviceDescriptor: ServiceDescriptor = ServiceDescriptor("startup-test-service", apiPrefix = "startup-test", port)
      override val portOverride: Port = port

      override fun Application.configure() {
        //No routes: the failure happens before any request could be served
      }
    }
  }

  /**
   * A port nobody listens on. Taking and releasing it leaves a window in which another process could
   * grab it — acceptable for a test, and the alternative (port 0) hides which port was bound.
   */
  private fun freePort(): Port = ServerSocket(0).use { Port(it.localPort) }
}
