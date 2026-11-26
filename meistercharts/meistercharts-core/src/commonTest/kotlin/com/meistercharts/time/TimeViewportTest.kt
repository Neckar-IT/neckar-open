/*
 * Copyright 2023 Neckar IT GmbH, Mössingen, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meistercharts.time

import assertk.*
import assertk.assertions.*
import it.neckar.datetime.minimal.LocalDateTime
import it.neckar.open.unit.si.ms
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test

class TimeViewportTest {
  /** 1789-07-14T14:00:00Z */
  private val bastilleDayAfternoon: @ms Double = LocalDateTime.of(1789, 7, 14, 14, 0, 0).toMillis(offset = 0.0)

  /** 2026-11-20T00:00:00Z */
  private val today: @ms Double = LocalDateTime.of(2026, 11, 20, 0, 0, 0).toMillis(offset = 0.0)

  @Test
  fun positionRelativeToTheCenter() {
    val viewport = TimeViewport()
    // 1789-07-14T12:00:00Z in the center, one day across the window
    viewport.set(centerMillis = -5.6949264e12, millisPerPixel = 57_600.0, windowWidth = 1_500.0)
    // 1789-07-14T06:00:00Z
    assertThat(viewport.time2windowX(-5.6949264e12 - 6 * 3_600_000.0)).isCloseTo(375.0, 1e-9)
    assertThat(viewport.window2timeX(375.0)).isCloseTo(-5.6949264e12 - 6 * 3_600_000.0, 1e-3)
  }

  @Test
  fun zoomFromTheWholeSpanToOneSecondAndBackKeepsAnEntryWithinOnePixel() {
    val viewport = TimeViewport()
    viewport.updateWindowWidth(1_500.0)
    viewport.showWindow(TimeViewport.BigBangMillis, today)
    @ms val initialMillisPerPixel = viewport.millisPerPixel
    val entryX = viewport.time2windowX(bastilleDayAfternoon)

    val steps = 60
    val factor = (TimeViewport.MinSpan / 1_500.0 / initialMillisPerPixel).pow(1.0 / steps)
    repeat(steps) {
      viewport.zoomAround(viewport.time2windowX(bastilleDayAfternoon), factor)
    }
    assertThat(viewport.visibleTo - viewport.visibleFrom).isCloseTo(TimeViewport.MinSpan, 1e-3)
    assertThat(viewport.visibleFrom).isLessThanOrEqualTo(bastilleDayAfternoon)
    assertThat(viewport.visibleTo).isGreaterThanOrEqualTo(bastilleDayAfternoon)

    repeat(steps) {
      viewport.zoomAround(viewport.time2windowX(bastilleDayAfternoon), 1 / factor)
    }
    assertThat(viewport.millisPerPixel / initialMillisPerPixel).isCloseTo(1.0, 1e-6)
    assertThat(abs(viewport.time2windowX(bastilleDayAfternoon) - entryX)).isLessThanOrEqualTo(1.0)
  }

  @Test
  fun zoomIsClampedToTheSupportedRange() {
    val viewport = TimeViewport()
    viewport.updateWindowWidth(1_000.0)
    repeat(200) { viewport.zoomAround(500.0, 0.1) }
    assertThat(viewport.millisPerPixel).isEqualTo(TimeViewport.MinSpan / 1_000.0)
    repeat(200) { viewport.zoomAround(500.0, 10.0) }
    assertThat(viewport.millisPerPixel).isEqualTo(TimeViewport.MaxSpan / 1_000.0)
  }

  @Test
  fun wholeSpanBesideAFixedColumn() {
    val viewport = TimeViewport()
    viewport.updateWindowWidth(960.0, visibleStartX = 260.0)
    viewport.showWindow(TimeViewport.BigBangMillis, today)

    assertThat(viewport.time2windowX(TimeViewport.BigBangMillis)).isCloseTo(260.0, 1e-6)
    assertThat(viewport.time2windowX(today)).isCloseTo(960.0, 1e-6)

    viewport.panBy(1e9)
    assertThat(viewport.visibleFrom).isCloseTo(it.neckar.datetime.minimal.LocalDate.MinMillis, 1e6)
  }
}
