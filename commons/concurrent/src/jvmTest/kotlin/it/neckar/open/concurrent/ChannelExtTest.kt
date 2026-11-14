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
package it.neckar.open.concurrent

import assertk.*
import assertk.assertions.*
import it.neckar.open.test.utils.isFalse
import it.neckar.open.test.utils.isTrue
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class ChannelExtTest {
  @Test
  fun testCanReceive() = runTest {
    val channel = Channel<String>()

    assertThat(channel.isEmpty).isTrue()
    assertThat(channel.isClosedForReceive).isFalse()

    val done = AtomicBoolean(false)
    val job = launch(coroutineContext) {
      while (isActive && channel.canReceive()) {
        delay(10)
      }

      assertThat(done).isFalse()
      done.set(true)
    }

    channel.close()
    assertThat(channel.canReceive()).isFalse()

    job.join()
    assertThat(done).isTrue()
    assertThat(channel.canReceive()).isFalse()
  }

  @Test
  suspend fun testCanReceive2() {
    val channel = Channel<String>(1)

    assertThat(channel.isEmpty).isTrue()
    assertThat(channel.isClosedForReceive).isFalse()
    assertThat(channel.canReceive()).isTrue()

    channel.send("foobar1")

    assertThat(channel.isEmpty).isFalse()
    assertThat(channel.isClosedForReceive).isFalse()
    assertThat(channel.canReceive()).isTrue()

    assertThat(channel.receive()).isEqualTo("foobar1")

    assertThat(channel.isEmpty).isTrue()
    assertThat(channel.isClosedForReceive).isFalse()
    assertThat(channel.canReceive()).isTrue()

    channel.send("foobar2")
    channel.close()

    assertThat(channel.isEmpty).isFalse()
    assertThat(channel.isClosedForReceive).isFalse()
    assertThat(channel.canReceive()).isTrue()

    assertThat(channel.receive()).isEqualTo("foobar2")

    assertThat(channel.isEmpty).isFalse() //Because isClosedForReceive is false
    assertThat(channel.isClosedForReceive).isTrue()
    assertThat(channel.canReceive()).isFalse()
  }
}
