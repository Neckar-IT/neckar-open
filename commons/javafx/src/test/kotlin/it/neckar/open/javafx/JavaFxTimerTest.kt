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
@file:Suppress("RETURN_VALUE_NOT_USED")

package it.neckar.open.javafx

import assertk.*
import assertk.assertions.*
import it.neckar.open.concurrent.ThreadDeadlockDetector
import it.neckar.open.javafx.test.JavaFxTest
import it.neckar.open.test.utils.WithVirtualTime
import it.neckar.open.test.utils.isFalse
import it.neckar.open.test.utils.isTrue
import it.neckar.open.time.VirtualNowProvider
import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import jfxtras.util.PlatformUtil
import org.awaitility.core.ConditionTimeoutException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

@JavaFxTest
class JavaFxTimerTest {

  private lateinit var threadDeadlockDetector: ThreadDeadlockDetector

  @BeforeEach
  fun setUp() {
    threadDeadlockDetector = ThreadDeadlockDetector(1_000).also { it.start() }
  }

  @AfterEach
  fun tearDown() {
    threadDeadlockDetector.stop()
  }

  @WithVirtualTime(5000.0)
  @Test
  fun testSimple(nowProvider: VirtualNowProvider) {
    assertThat(nowProvider).isInstanceOf(VirtualNowProvider::class)

    assertThat(Platform.isFxApplicationThread()).isFalse()

    var called = false
    JavaFxTimer.delay(10.milliseconds) {
      assertThat(called).isFalse()
      called = true
    }

    nowProvider.advanceBy(20.0)
    assertThat(nowProvider.nowMillis()).isEqualTo(5020.0)

    try {
      JavaFxTimer.waitForPaintPulse()
      FxUtils.waitFor {
        called
      }
      assertThat(called).isTrue()
    } catch (e: ConditionTimeoutException) {
      println("ConditionTimeoutException caught: ${e.message}")
      Thread.getAllStackTraces().forEach { thread, stackTrace ->
        println("--------- ${thread.id} - ${thread.name}-------------------")
        stackTrace.forEach {
          println(it.toString())
        }
      }

      throw e
    }
  }

  @WithVirtualTime(5000.0)
  @Test
  fun testDelayNotImmediately(nowProvider: VirtualNowProvider) {
    try {
      assertThat(Platform.isFxApplicationThread()).isFalse()

      val called = AtomicBoolean(false)

      JavaFxTimer.delay(10.milliseconds) {
        assertThat(called).isFalse()
        called.set(true)
      }

      assertThat(called).isFalse()
      JavaFxTimer.waitForPaintPulse()
      assertThat(called).isFalse()

      nowProvider.advanceBy(9.0)

      assertThat(called).isFalse()
      JavaFxTimer.waitForPaintPulse()
      assertThat(called).isFalse()

      nowProvider.advanceBy(1.0)
      assertThat(nowProvider.nowMillis()).isEqualTo(5010.0)

      JavaFxTimer.waitForPaintPulse()
      FxUtils.waitFor { called.get() }
      assertThat(called).isTrue()
    } catch (e: ConditionTimeoutException) {
      println("---------- ConditionTimeoutException: ${e.message} ----------")
      Thread.dumpStack()

      Thread.getAllStackTraces().forEach {
        println("---- ${it.key?.name} --------")
        println(it.value.joinToString("\n"))
      }
      throw e
    }
  }

  @Test
  fun testDelay() {
    assertThat(Platform.isFxApplicationThread()).isFalse()

    var called = false

    JavaFxTimer.delay(0.milliseconds) {
      assertThat(called).isFalse()
      called = true
    }

    //wait to ensure the event has been processed
    JavaFxTimer.waitForPaintPulse()

    FxUtils.waitFor {
      called
    }
  }

  /**
   * Shows that a timeline with only one key frame at ZERO does *not* run at all
   */
  @Test
  fun testEmptyTimeline() {
    val eventHandler: EventHandler<ActionEvent> = EventHandler { throw IllegalStateException("Should not be called") }
    val keyFrame = KeyFrame(javafx.util.Duration.ZERO, eventHandler)
    val timeline = Timeline(keyFrame)

    assertThat(timeline.status).isEqualTo(Animation.Status.STOPPED)
    timeline.play()
    assertThat(timeline.status).isEqualTo(Animation.Status.STOPPED)

    PlatformUtil.runAndWait {
      //wait to ensure the event has been processed
    }

    assertThat(timeline.status).isEqualTo(Animation.Status.STOPPED)
  }

  @Test
  fun testEmptyTimeLine() {
    val timeline = Timeline()

    assertThat(timeline.status).isEqualTo(Animation.Status.STOPPED)
    timeline.play()
    assertThat(timeline.status).isEqualTo(Animation.Status.STOPPED)
  }
}
