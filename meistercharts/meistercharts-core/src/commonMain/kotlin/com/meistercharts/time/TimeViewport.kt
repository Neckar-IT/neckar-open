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

import it.neckar.datetime.minimal.LocalDate
import it.neckar.open.unit.other.px
import it.neckar.open.unit.si.ms
import kotlin.math.max

/**
 * The visible time window, computed relative to the window center.
 *
 * A time `t` lies at `windowWidth / 2 + (t - centerMillis) / millisPerPixel`; around the center `Double` keeps
 * its full resolution, also when the span from the Big Bang to the present fits into the window.
 */
class TimeViewport {
  var centerMillis: @ms Double = 0.0
    private set

  /**
   * The zoom level: the time span of one pixel.
   */
  var millisPerPixel: @ms Double = 1.0
    private set

  var windowWidth: @px Double = 0.0
    private set

  /**
   * The x where the visible time starts; a fixed column covers the window left of it.
   */
  var visibleStartX: @px Double = 0.0
    private set

  /**
   * The width that shows time, right of [visibleStartX].
   */
  val visibleWidth: @px Double
    get() = (windowWidth - visibleStartX).coerceAtLeast(0.0)

  /**
   * The time at [visibleStartX].
   */
  val visibleFrom: @ms Double
    get() = window2timeX(visibleStartX)

  /**
   * The time at the right edge of the window.
   */
  val visibleTo: @ms Double
    get() = window2timeX(windowWidth)

  fun time2windowX(time: @ms Double): @px Double {
    return windowWidth / 2 + (time - centerMillis) / millisPerPixel
  }

  fun window2timeX(x: @px Double): @ms Double {
    return centerMillis + (x - windowWidth / 2) * millisPerPixel
  }

  /**
   * Sets every value as given, without clamping; for a viewport that follows another coordinate system.
   */
  fun set(centerMillis: @ms Double, millisPerPixel: @ms Double, windowWidth: @px Double) {
    require(millisPerPixel > 0.0 && millisPerPixel.isFinite()) { "millisPerPixel must be positive and finite but was <$millisPerPixel>" }
    require(centerMillis.isFinite()) { "centerMillis must be finite but was <$centerMillis>" }
    this.centerMillis = centerMillis
    this.millisPerPixel = millisPerPixel
    this.windowWidth = windowWidth
  }

  /**
   * Keeps the time below the center when the width changes.
   */
  fun updateWindowWidth(windowWidth: @px Double, visibleStartX: @px Double = 0.0) {
    require(windowWidth >= 0.0) { "windowWidth must be >= 0 but was <$windowWidth>" }
    require(visibleStartX >= 0.0) { "visibleStartX must be >= 0 but was <$visibleStartX>" }
    this.windowWidth = windowWidth
    this.visibleStartX = visibleStartX
    clamp()
  }

  /**
   * Shows [from] to [to] from [visibleStartX] to the right edge of the window.
   */
  fun showWindow(from: @ms Double, to: @ms Double) {
    require(from < to) { "from <$from> must be smaller than to <$to>" }
    require(visibleWidth > 0.0) { "No visible width: windowWidth <$windowWidth>, visibleStartX <$visibleStartX>" }
    millisPerPixel = (to - from) / visibleWidth
    centerMillis = from + (windowWidth / 2 - visibleStartX) * millisPerPixel
    clamp()
  }

  /**
   * Zooms by [factor] so that the time below [x] stays below [x]; a factor above 1 zooms out.
   */
  fun zoomAround(x: @px Double, factor: Double) {
    require(factor > 0.0 && factor.isFinite()) { "factor must be positive and finite but was <$factor>" }
    @ms val timeAtX = window2timeX(x)
    millisPerPixel = (millisPerPixel * factor).coerceIn(minMillisPerPixel(), maxMillisPerPixel())
    centerMillis = timeAtX - (x - windowWidth / 2) * millisPerPixel
    clamp()
  }

  /**
   * Moves the content by [deltaX] pixels; a positive value shows earlier times.
   */
  fun panBy(deltaX: @px Double) {
    centerMillis -= deltaX * millisPerPixel
    clamp()
  }


  private fun minMillisPerPixel(): @ms Double {
    return if (visibleWidth > 0.0) MinSpan / visibleWidth else MinSpan
  }

  private fun maxMillisPerPixel(): @ms Double {
    return if (visibleWidth > 0.0) MaxSpan / visibleWidth else MaxSpan
  }

  /**
   * Keeps the time from [visibleStartX] to the right edge of the window within `LocalDate.MinMillis..LocalDate.MaxMillis`.
   */
  private fun clamp() {
    millisPerPixel = millisPerPixel.coerceIn(minMillisPerPixel(), maxMillisPerPixel())
    @ms val earliestCenter = LocalDate.MinMillis + (windowWidth / 2 - visibleStartX) * millisPerPixel
    @ms val latestCenter = LocalDate.MaxMillis - windowWidth / 2 * millisPerPixel
    centerMillis = centerMillis.coerceIn(earliestCenter, max(earliestCenter, latestCenter))
  }

  companion object {
    /**
     * The narrowest visible span: one second.
     */
    const val MinSpan: @ms Double = 1_000.0

    /**
     * The widest visible span: the whole range of `LocalDate`, 30 billion years.
     */
    val MaxSpan: @ms Double = LocalDate.MaxMillis - LocalDate.MinMillis

    /**
     * 13.787 billion years before 1970, the age of the universe.
     */
    const val BigBangMillis: @ms Double = -4.35075697224e20
  }
}
