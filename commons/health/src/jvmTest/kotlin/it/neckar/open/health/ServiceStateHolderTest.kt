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
package it.neckar.open.health

import assertk.*
import assertk.assertions.*
import it.neckar.open.time.now
import it.neckar.rest.health.DependencyName
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class ServiceStateHolderTest {
  @Test
  fun `legal transitions succeed`() {
    val holder = ServiceStateHolder()
    holder.transitionTo(ServiceState.Verifying)
    holder.transitionTo(ServiceState.Ready)
    holder.transitionTo(ServiceState.Degraded)
    holder.transitionTo(ServiceState.Ready)
    assertThat(holder.state).isEqualTo(ServiceState.Ready)
  }

  @Test
  fun `illegal transition is rejected`() {
    val holder = ServiceStateHolder()
    assertFailure { holder.transitionTo(ServiceState.Ready) }.isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  fun `transition out of fatal is rejected`() {
    val holder = ServiceStateHolder()
    holder.transitionTo(ServiceState.Fatal(FatalReason.Unspecified, "boom"))
    assertFailure { holder.transitionTo(ServiceState.Ready) }.isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  fun `fatal is reachable from any non-fatal state`() {
    val holder = ServiceStateHolder()
    holder.transitionTo(ServiceState.Verifying)
    holder.transitionTo(ServiceState.Fatal(FatalReason.SchemaIncompatible, "x"))
    assertThat(holder.state).isInstanceOf(ServiceState.Fatal::class)
  }

  @Test
  fun `transition to current state is a no-op without callback`() {
    var callbacks = 0
    val holder = ServiceStateHolder(onStateChanged = { callbacks++ })
    holder.transitionTo(ServiceState.Starting)
    assertThat(callbacks).isEqualTo(0)
  }

  @Test
  fun `callback fires only on actual change`() {
    val observed = mutableListOf<ServiceState>()
    val holder = ServiceStateHolder(onStateChanged = { observed.add(it) })
    holder.transitionTo(ServiceState.Verifying)
    holder.transitionTo(ServiceState.Ready)
    assertThat(observed).isEqualTo(listOf<ServiceState>(ServiceState.Verifying, ServiceState.Ready))
  }

  @Test
  fun `updateDependency records per-dependency state`() {
    val holder = ServiceStateHolder()
    holder.updateDependency(DependencyName.MongoDb, DependencyClassification.Required, DependencyStatus.Up())
    holder.updateDependency(DependencyName.S3, DependencyClassification.Optional, DependencyStatus.Down(RuntimeException("x"), now()))
    assertThat(holder.dependencies).hasSize(2)
  }

  @Test
  fun `recordUncaughtException keeps the latest exception, counts up and keeps the first timestamp`() {
    val holder = ServiceStateHolder()

    holder.recordUncaughtException(RuntimeException("first"))
    val first = holder.uncaughtExceptions
    assertThat(first?.count).isEqualTo(1)
    assertThat(first?.latest?.message).isEqualTo("first")

    holder.recordUncaughtException(NullPointerException("second"))
    val second = holder.uncaughtExceptions
    assertThat(second?.count).isEqualTo(2)
    assertThat(second?.latest?.message).isEqualTo("second")
    assertThat(second?.since).isEqualTo(first?.since)
  }

  /**
   * The fields of the holder are independent: an uncaught exception is an application error and must
   * not touch the lifecycle state or the dependencies.
   */
  @Test
  fun `recordUncaughtException changes neither the lifecycle state nor the dependencies`() {
    val holder = ServiceStateHolder().apply {
      transitionTo(ServiceState.Verifying)
      transitionTo(ServiceState.Ready)
      updateDependency(DependencyName.MongoDb, DependencyClassification.Required, DependencyStatus.Up())
    }

    holder.recordUncaughtException(RuntimeException("boom"))

    assertThat(holder.state).isEqualTo(ServiceState.Ready)
    assertThat(holder.dependencies).hasSize(1)
  }

  /**
   * `installHealthLayer` adopts what the collector recorded during the startup sequence, before the
   * holder was connected to it.
   */
  @Test
  fun `recordUncaughtExceptions adopts an earlier snapshot`() {
    val earlier = now()
    val holder = ServiceStateHolder()

    holder.recordUncaughtExceptions(UncaughtExceptions(RuntimeException("during startup"), count = 2, since = earlier))

    assertThat(holder.uncaughtExceptions?.count).isEqualTo(2)
    assertThat(holder.uncaughtExceptions?.since).isEqualTo(earlier)
    assertThat(holder.uncaughtExceptions?.latest?.message).isEqualTo("during startup")
  }

  @Test
  fun `recordUncaughtExceptions adds to what the holder recorded itself`() {
    val holder = ServiceStateHolder()
    holder.recordUncaughtException(NullPointerException("after install"))
    val ownSince = holder.uncaughtExceptions?.since

    holder.recordUncaughtExceptions(UncaughtExceptions(RuntimeException("during startup"), count = 2, since = ownSince!!.minus(1.seconds)))

    assertThat(holder.uncaughtExceptions?.count).isEqualTo(3)
    assertThat(holder.uncaughtExceptions?.since).isEqualTo(ownSince.minus(1.seconds))
    assertThat(holder.uncaughtExceptions?.latest?.message).isEqualTo("after install")
  }

  @Test
  fun `clearUncaughtExceptions clears the recorded exceptions`() {
    val holder = ServiceStateHolder()
    holder.recordUncaughtException(RuntimeException("boom"))

    holder.clearUncaughtExceptions()

    assertThat(holder.uncaughtExceptions).isNull()
  }

  /**
   * The compare-and-set exists because the dependency monitor reconciles `Ready ↔ Degraded` from the
   * periodic poll and from every `/health/ready` at once: a plain `transitionTo` threw out of the
   * request handler when the state had moved on in between.
   */
  @Test
  fun `transitionToIfCurrentIs moves only while the state is the expected one`() {
    val holder = ServiceStateHolder()
    holder.transitionTo(ServiceState.Verifying)
    holder.transitionTo(ServiceState.Ready)

    assertThat(holder.transitionToIfCurrentIs(ServiceState.Ready, ServiceState.Degraded)).isTrue()
    assertThat(holder.state).isEqualTo(ServiceState.Degraded)

    assertThat(holder.transitionToIfCurrentIs(ServiceState.Ready, ServiceState.Degraded)).isFalse()
    assertThat(holder.state).isEqualTo(ServiceState.Degraded)
  }

  @Test
  fun `transitionToIfCurrentIs answers false for a state that moved on, instead of throwing`() {
    val holder = ServiceStateHolder()
    holder.transitionTo(ServiceState.Fatal(FatalReason.Unspecified, "gone"))

    //Ready -> Degraded is a legal transition; from Fatal it is not, and a plain transitionTo throws.
    assertThat(holder.transitionToIfCurrentIs(ServiceState.Ready, ServiceState.Degraded)).isFalse()
    assertThat(holder.state).isInstanceOf(ServiceState.Fatal::class)
  }

  @Test
  fun `transitionToIfCurrentIs announces nothing when it is already where it wanted to be`() {
    val announced = mutableListOf<ServiceState>()
    val holder = ServiceStateHolder(onStateChanged = { announced += it })
    holder.transitionTo(ServiceState.Verifying)
    holder.transitionTo(ServiceState.Ready)
    announced.clear()

    assertThat(holder.transitionToIfCurrentIs(ServiceState.Ready, ServiceState.Ready)).isFalse()
    assertThat(announced).isEmpty()
  }
}
