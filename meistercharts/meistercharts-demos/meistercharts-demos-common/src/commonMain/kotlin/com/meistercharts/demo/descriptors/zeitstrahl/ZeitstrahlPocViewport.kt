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
package com.meistercharts.demo.descriptors.zeitstrahl

import com.meistercharts.axis.time.YearNumbering
import com.meistercharts.time.TimeViewport
import it.neckar.datetime.minimal.DayOfMonth
import it.neckar.datetime.minimal.LocalDate
import it.neckar.datetime.minimal.Month
import it.neckar.datetime.minimal.TimeConstants
import it.neckar.datetime.minimal.Year
import kotlin.math.floor
import kotlin.math.log2

/**
 * The lanes region right of [ZeitstrahlPocLayout.TreeWidth] of [timeViewport], in seconds like `zeitstrahl-poc-server`.
 *
 * [timeViewport] covers the whole canvas, so `TimeAxisLayer` paints canvas x; every x here counts from the left of the lanes region.
 */
class ZeitstrahlPocViewport(
  /** Seconds since 1970-01-01T00:00:00Z of the present. */
  val presentSeconds: Double,
) {
  val timeViewport: TimeViewport = TimeViewport()

  /** The window [showWindow] got before the first width, applied by [applyPendingWindow]. */
  private var pendingWindow: Pair<Double, Double>? = BigBangSeconds to presentSeconds

  val windowWidth: Double
    get() = timeViewport.visibleWidth

  val secondsPerPixel: Double
    get() = timeViewport.millisPerPixel / MillisPerSecond

  /** The time at the center of the lanes region. */
  val centerSeconds: Double
    get() = secondsAt(windowWidth / 2)

  /** The zoom level of the detail levels and of the lane query; the widest span of `TimeViewport` reaches 49 to 51 and stays at the coarsest level. */
  val zoomLevel: ZoomLevel
    get() = ZoomLevel(floor(log2(secondsPerPixel)).toInt().coerceIn(ZoomLevel.MinValue, ZoomLevel.MaxValue))

  val visibleFrom: Double
    get() = timeViewport.visibleFrom / MillisPerSecond

  val visibleTo: Double
    get() = timeViewport.visibleTo / MillisPerSecond

  val visibleSeconds: Double
    get() = secondsPerPixel * windowWidth

  fun xOf(seconds: Double): Double = timeViewport.time2windowX(seconds * MillisPerSecond) - timeViewport.visibleStartX

  fun secondsAt(x: Double): Double = timeViewport.window2timeX(x + timeViewport.visibleStartX) / MillisPerSecond

  /** Half-open: a span that ends where the window starts lies outside, like the neighbour section of a periodization. */
  fun overlaps(start: Double, end: Double): Boolean = end > visibleFrom && start < visibleTo

  /** The pixels of [start] to [end] inside the window. */
  fun visibleXRange(start: Double, end: Double): ClosedFloatingPointRange<Double> = maxOf(xOf(start), 0.0)..minOf(xOf(end), windowWidth)

  /** Shows [fromSeconds] to [toSeconds] across the lanes region, once the width is known. */
  fun showWindow(fromSeconds: Double, toSeconds: Double) {
    pendingWindow = fromSeconds to toSeconds
    applyPendingWindow()
  }

  /** Applies the window of [showWindow] as soon as the lanes region has a width. */
  fun applyPendingWindow() {
    val (fromSeconds, toSeconds) = pendingWindow ?: return
    if (windowWidth <= 0.0) {
      return
    }
    timeViewport.showWindow(fromSeconds * MillisPerSecond, toSeconds * MillisPerSecond)
    pendingWindow = null
  }

  companion object {
    private const val MillisPerSecond: Double = TimeConstants.MillisPerSecond

    /** 13.787 billion years before 1970 */
    const val BigBangSeconds: Double = TimeViewport.BigBangMillis / MillisPerSecond

    private val beforePresentReferenceSeconds: Double = secondsOf(YearNumbering.Calendar.BeforePresentReferenceYear, 1, 1)

    /** The mean year of the gregorian calendar. */
    const val SecondsPerYear: Double = 365.2425 * 86_400.0

    /** The start of a day of the proleptic gregorian calendar in astronomical year counting. */
    fun secondsOf(year: Long, month: Int, day: Int): Double = LocalDate(Year(year), Month(month), DayOfMonth(day)).toEpochDay() * 86_400.0

    fun secondsOfYearsBeforePresent(years: Double): Double = beforePresentReferenceSeconds - years * SecondsPerYear

    /** The astronomical year at [seconds]. */
    fun yearAt(seconds: Double): Year = LocalDate.fromMillis(seconds * MillisPerSecond, offset = 0.0).year
  }
}
