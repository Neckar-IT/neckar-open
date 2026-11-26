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
package com.meistercharts.algorithms.layers.axis.time

import assertk.*
import assertk.assertions.*
import com.meistercharts.axis.time.TimeTickDistance
import com.meistercharts.axis.time.YearNumbering
import com.meistercharts.canvas.i18nSupport
import com.meistercharts.canvas.layout.buffer.TickLabelsBuffer
import com.meistercharts.canvas.mock.MockCanvasRenderingContext
import com.meistercharts.canvas.mock.MockLayerPaintingContext
import com.meistercharts.time.TimeViewport
import it.neckar.datetime.minimal.DayOfMonth
import it.neckar.datetime.minimal.LocalDate
import it.neckar.datetime.minimal.LocalDateTime
import it.neckar.datetime.minimal.LocalTime
import it.neckar.datetime.minimal.Month
import it.neckar.datetime.minimal.TimeZone
import it.neckar.datetime.minimal.Year
import it.neckar.geometry.Size
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.unit.si.ms
import org.junit.jupiter.api.Test

/**
 * The labels [TimeAxisLayer] computes with [CalendarTickFormat] and [YearNumbering.Calendar] for each window of the
 * label table of the time axis, in a window of 1,500 px.
 */
class TimeAxisLayerCalendarLabelsTest {
  private val german: I18nConfiguration = I18nConfiguration.GermanyUTC
  private val english: I18nConfiguration = I18nConfiguration(TimeZone.UTC, it.neckar.open.i18n.Locale.US)

  @Test
  fun bigBangToToday() {
    val labels = labels(TimeViewport.BigBangMillis, millisOf(2026, 11, 20))
    assertThat(labels.first()).isEqualTo("vor 13 Mrd. Jahren")
    assertThat(labels).contains("vor 12 Mrd. Jahren")
    assertThat(labels.last()).isEqualTo("vor 1 Mrd. Jahren")
    assertThat(labels).hasSize(12)
    // The ticks at an offset tick have their label in the offset area.
    assertThat(offsetLabels(TimeViewport.BigBangMillis, millisOf(2026, 11, 20))).containsExactly("vor 10 Mrd. Jahren", "heute")
  }

  @Test
  fun dinosaursDieOut() {
    val labels = labels(millisOf(1950 - 74_000_000L, 1, 1), millisOf(1950 - 58_000_000L, 1, 1))
    assertThat(labels).containsExactly(
      "vor 74 Mio. Jahren", "vor 72 Mio. Jahren", "vor 70 Mio. Jahren", "vor 68 Mio. Jahren", "vor 66 Mio. Jahren",
      "vor 64 Mio. Jahren", "vor 62 Mio. Jahren", "vor 60 Mio. Jahren", "vor 58 Mio. Jahren",
    )
    assertThat(labels(millisOf(1950 - 74_000_000L, 1, 1), millisOf(1950 - 58_000_000L, 1, 1), english)).contains("66 million years ago")
  }

  @Test
  fun neolithicRevolution() {
    val labels = labels(millisOf(-11_999, 1, 1), millisOf(-7_999, 1, 1))
    assertThat(labels.first()).isEqualTo("12.000 v. Chr.")
    assertThat(labels).contains("10.000 v. Chr.")
    assertThat(labels.last()).isEqualTo("8.000 v. Chr.")
    assertThat(labels).hasSize(9)
  }

  @Test
  fun lastCenturyBeforeChrist() {
    val labels = labels(millisOf(-99, 1, 1), millisOf(1, 1, 1))
    assertThat(labels.first()).isEqualTo("100 v. Chr.")
    assertThat(labels).contains("50 v. Chr.")
    assertThat(labels.last()).isEqualTo("10 v. Chr.")
    assertThat(labels).hasSize(10)
  }

  @Test
  fun eighteenthCentury() {
    val labels = labels(millisOf(1700, 1, 1), millisOf(1800, 1, 1))
    assertThat(labels.first()).isEqualTo("1700")
    assertThat(labels).contains("1710")
    assertThat(labels.last()).isEqualTo("1800")
    assertThat(labels).hasSize(11)
  }

  @Test
  fun daysOfJuly1789() {
    val labels = labels(millisOf(1789, 7, 10), millisOf(1789, 7, 20))
    assertThat(labels).contains("14. Juli")
    assertThat(labels).contains("15. Juli")
    assertThat(labels).hasSize(11)
    assertThat(labels(millisOf(1789, 7, 10), millisOf(1789, 7, 20), english)).contains("July 14")
  }

  @Test
  fun hoursOfBastilleDay() {
    val labels = labels(millisOf(1789, 7, 14) + 8 * 3_600_000.0, millisOf(1789, 7, 14) + 20 * 3_600_000.0)
    assertThat(labels).contains("14:00")
    assertThat(labels).contains("15:00")
    assertThat(labels).hasSize(13)
  }

  @Test
  fun oneSecondOfBastilleDay() {
    @ms val start = millisOf(1789, 7, 14) + 14 * 3_600_000.0
    val labels = labels(start, start + 1_000.0)
    assertThat(labels).contains("14:00:00.100")
    assertThat(labels).contains("14:00:00.200")
    assertThat(labels).hasSize(9)
    assertThat(offsetLabels(start, start + 1_000.0)).containsExactly("14. Juli 1789 14:00:00", "14. Juli 1789 14:00:01")
  }

  @Test
  fun historicalYearsWithoutYearZero() {
    assertThat(CalendarLabels.German.historicalYear(Year(-43))).isEqualTo("44 v. Chr.")
    assertThat(CalendarLabels.English.historicalYear(Year(-43))).isEqualTo("44 BC")
    assertThat(CalendarLabels.German.historicalYear(Year(0))).isEqualTo("1 v. Chr.")
    assertThat(CalendarLabels.German.historicalYear(Year(10_000))).isEqualTo("10.000")
  }

  @Test
  fun offsetLabels() {
    val bastilleDay = LocalDateTime(LocalDate(1789, 7, 14), LocalTime(14, 0, 0)).toMillis(offset = 0.0)
    assertThat(CalendarOffsetFormat.format(bastilleDay, TimeTickDistance.forOffsets(3_600_000.0 * 12), german)).isEqualTo("14. Juli 1789")
    assertThat(CalendarOffsetFormat.format(bastilleDay, TimeTickDistance.forOffsets(60_000.0 * 10), english)).isEqualTo("July 14, 1789 14:00")
  }

  /**
   * The tick labels [TimeAxisLayer] paints between [start] and [end]; a tick at an offset tick has no label of its own.
   */
  private fun labels(start: @ms Double, end: @ms Double, i18nConfiguration: I18nConfiguration = german): List<String> {
    return layout(start, end, i18nConfiguration).tickLabels.labelsBetween(start, end)
  }

  /**
   * The labels of the offset area [TimeAxisLayer] paints between [start] and [end].
   */
  private fun offsetLabels(start: @ms Double, end: @ms Double, i18nConfiguration: I18nConfiguration = german): List<String> {
    return layout(start, end, i18nConfiguration).offsetTickLabels.labelsBetween(start, end)
  }

  private fun layout(start: @ms Double, end: @ms Double, i18nConfiguration: I18nConfiguration): TimeAxisPaintingVariables {
    val viewport = TimeViewport()
    viewport.updateWindowWidth(WindowWidth)
    viewport.showWindow(start, end)

    val layer = TimeAxisLayer {
      viewportSource = TimeAxisViewportSource.Explicit(viewport)
      absoluteTimestampTickFormat = CalendarTickFormat
      offsetTickFormat = CalendarOffsetFormat
      yearNumbering = YearNumbering.Calendar
    }
    val gc = MockCanvasRenderingContext()
    gc.canvasSize = Size.of(WindowWidth, 600.0)
    val paintingContext = MockLayerPaintingContext().copy(gc = gc)
    paintingContext.chartSupport.i18nSupport.selectedConfiguration = i18nConfiguration
    paintingContext.chartSupport.rootChartState.windowSize = gc.canvasSize
    paintingContext.chartSupport.rootChartState.contentAreaSize = gc.canvasSize
    layer.layout(paintingContext)
    return layer.paintingVariables()
  }

  private fun TickLabelsBuffer.labelsBetween(start: @ms Double, end: @ms Double): List<String> {
    return (0 until size).filter { valueAt(it) in start..end }.map { formattedAt(it) }
  }

  private fun millisOf(year: Long, month: Int, day: Int): @ms Double {
    return LocalDate(Year(year), Month(month), DayOfMonth(day)).toEpochDay() * 86_400_000.0
  }

  private fun millisOf(year: Int, month: Int, day: Int): @ms Double {
    return millisOf(year.toLong(), month, day)
  }

  private companion object {
    const val WindowWidth: Double = 1_500.0
  }
}
