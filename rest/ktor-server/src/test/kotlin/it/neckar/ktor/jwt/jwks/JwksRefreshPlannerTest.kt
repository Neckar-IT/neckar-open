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
package it.neckar.ktor.jwt.jwks

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import it.neckar.open.time.VirtualNowProvider
import it.neckar.open.time.nowProvider
import it.neckar.open.time.resetNowProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class JwksRefreshPlannerTest {
  private lateinit var virtualNow: VirtualNowProvider

  private val planner = JwksRefreshPlanner(window = 30.seconds, maxRefreshesPerMinute = 4)

  @BeforeEach
  fun fixTime() {
    virtualNow = VirtualNowProvider(1_800_000_000_000.0)
    nowProvider = virtualNow
  }

  @AfterEach
  fun releaseTime() {
    resetNowProvider()
  }

  private fun advance(by: kotlin.time.Duration) {
    virtualNow.virtualNow += by.inWholeMilliseconds.toDouble()
  }

  @Test
  fun `plans the first refresh and then nothing until the window elapsed`() {
    assertThat(planner.planRefreshFor(JwkKeyId("a"))).isTrue()
    assertThat(planner.planRefreshFor(JwkKeyId("b"))).isFalse()

    advance(29.seconds)
    assertThat(planner.planRefreshFor(JwkKeyId("c"))).isFalse()

    advance(2.seconds)
    assertThat(planner.planRefreshFor(JwkKeyId("d"))).isTrue()
  }

  @Test
  fun `a thousand requests with a thousand distinct kids inside one minute plan two refreshes`() {
    var planned = 0

    //One request every 60 ms, so the whole flood fits into the minute the cap is measured over.
    repeat(1000) { index ->
      if (planner.planRefreshFor(JwkKeyId("flood-$index"))) {
        planned++
      }
      advance(60.milliseconds)
    }

    //The window is the binding limit here: two 30-second windows fit into the minute, the cap allows four.
    assertThat(planned).isEqualTo(2)
  }

  @Test
  fun `one refresh per window is never blocked by the cap`() {
    var planned = 0

    //One request every 31 seconds: fewer than the cap allows per minute, so only the window decides.
    repeat(20) {
      if (planner.planRefreshFor(JwkKeyId("kid-$it"))) {
        planned++
      }
      advance(31.seconds)
    }

    assertThat(planned).isEqualTo(20)
  }

  @Test
  fun `the cap alone bounds a flood inside one minute`() {
    val tight = JwksRefreshPlanner(window = 1.seconds, maxRefreshesPerMinute = 4)
    var planned = 0

    repeat(60) {
      if (tight.planRefreshFor(JwkKeyId("kid-$it"))) {
        planned++
      }
      advance(1.seconds)
    }

    assertThat(planned).isEqualTo(4)
  }

  @Test
  fun `a kid a successful fetch did not deliver plans nothing more`() {
    assertThat(planner.planRefreshFor(JwkKeyId("stranger"))).isTrue()
    planner.onFetchSucceeded(setOf(JwkKeyId("ours")))

    assertThat(planner.knownAbsentKeyIds).contains(JwkKeyId("stranger"))

    advance(1.minutes)
    assertThat(planner.planRefreshFor(JwkKeyId("stranger"))).isFalse()
  }

  @Test
  fun `a kid the fetch delivered is not remembered as absent`() {
    assertThat(planner.planRefreshFor(JwkKeyId("rotated"))).isTrue()
    planner.onFetchSucceeded(setOf(JwkKeyId("rotated")))

    assertThat(planner.knownAbsentKeyIds).isEmpty()
  }

  /**
   * The bound this asserts is the one on the negative memory, so the kids have to arrive across more
   * than one fetch: a single [JwksRefreshPlanner.onFetchSucceeded] can only carry over what the
   * asked-since-fetch memory holds, which is bounded by the same number and would keep the
   * eviction under test from ever running.
   */
  @Test
  fun `the negative memory is bounded, so a flood of distinct kids cannot grow it`() {
    val flooded = JwksRefreshPlanner(window = 1.seconds, maxRefreshesPerMinute = Int.MAX_VALUE)
    val batchSize = JwksRefreshPlanner.MaxRememberedKeyIds / 2
    val batches = (0 until 4).map { batch -> (0 until batchSize).map { JwkKeyId("flood-$batch-$it") } }

    batches.forEach { batch ->
      batch.forEach {
        flooded.planRefreshFor(it)
        advance(1.seconds)
      }
      flooded.onFetchSucceeded(emptySet())
    }

    assertThat(flooded.knownAbsentKeyIds).hasSize(JwksRefreshPlanner.MaxRememberedKeyIds)
    //Oldest first: the kids that arrived most recently are the ones worth remembering.
    assertThat(flooded.knownAbsentKeyIds).doesNotContain(batches.first().first())
    assertThat(flooded.knownAbsentKeyIds).contains(batches.last().last())
  }

  /**
   * Kids already established as absent must not re-enter the asked-since-fetch memory: they have an
   * answer, and re-entering would push out the kids that are still waiting for one.
   *
   * Enough distinct absent kids to fill that memory, so the displacement can happen at all — the
   * property is invisible with a single kid asked repeatedly, which occupies one entry either way.
   */
  @Test
  fun `kids already known to be absent do not displace the ones still waiting for an answer`() {
    val unbounded = JwksRefreshPlanner(window = 1.seconds, maxRefreshesPerMinute = Int.MAX_VALUE)
    val strangers = (0 until JwksRefreshPlanner.MaxRememberedKeyIds).map { JwkKeyId("stranger-$it") }

    strangers.forEach {
      unbounded.planRefreshFor(it)
      advance(1.seconds)
    }
    unbounded.onFetchSucceeded(setOf(JwkKeyId("ours")))
    assertThat(unbounded.knownAbsentKeyIds).hasSize(JwksRefreshPlanner.MaxRememberedKeyIds)

    //One kid nobody has answered yet, and then every answered one again. Re-entering would fill the
    //memory behind this one and push it out — it is the oldest entry from here on.
    advance(1.seconds)
    val waiting = JwkKeyId("waiting")
    assertThat(unbounded.planRefreshFor(waiting)).isTrue()

    strangers.forEach {
      advance(1.seconds)
      assertThat(unbounded.planRefreshFor(it)).isFalse()
    }

    unbounded.onFetchSucceeded(setOf(JwkKeyId("ours")))

    assertThat(unbounded.knownAbsentKeyIds).contains(waiting)
  }

  @Test
  fun `the regular cycle forgets what was established as absent`() {
    assertThat(planner.planRefreshFor(JwkKeyId("stranger"))).isTrue()
    planner.onFetchSucceeded(setOf(JwkKeyId("ours")))

    planner.forgetAbsentKeyIds()

    advance(1.minutes)
    assertThat(planner.planRefreshFor(JwkKeyId("stranger"))).isTrue()
  }
}
