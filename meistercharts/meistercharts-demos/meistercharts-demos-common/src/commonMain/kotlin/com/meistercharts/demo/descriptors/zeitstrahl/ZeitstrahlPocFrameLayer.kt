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

import com.meistercharts.algorithms.layers.AbstractLayer
import com.meistercharts.algorithms.layers.LayerPaintingContext
import com.meistercharts.algorithms.layers.LayerType
import com.meistercharts.algorithms.layers.axis.time.CalendarOffsetFormat
import com.meistercharts.algorithms.layers.axis.time.TimeAxisLayer
import com.meistercharts.axis.time.TimeTickDistance
import com.meistercharts.canvas.fill
import com.meistercharts.canvas.saved
import com.meistercharts.canvas.strokeStyle
import com.meistercharts.font.FontDescriptorFragment
import com.meistercharts.font.FontSize
import com.meistercharts.font.FontWeight
import it.neckar.geometry.Direction
import kotlin.math.roundToLong

/**
 * The background, the row of the path with the date of the window center, the column lines and a grid line per tick of
 * [timeAxisLayer] down to the time axis. [ZeitstrahlPocLanesLayer] writes the path into the left of the path row.
 */
class ZeitstrahlPocFrameLayer(
  private val viewport: ZeitstrahlPocViewport,
  private val timeAxisLayer: TimeAxisLayer,
) : AbstractLayer() {
  override val type: LayerType = LayerType.Content

  override fun layout(paintingContext: LayerPaintingContext) {
    super.layout(paintingContext)
    viewport.applyPendingWindow()
  }

  override fun paint(paintingContext: LayerPaintingContext) {
    val gc = paintingContext.gc
    val pathBottom = ZeitstrahlPocLayout.PathHeight
    val gridBottom = gc.height - ZeitstrahlPocLayout.StripHeight - ZeitstrahlPocLayout.TimeAxisHeight
    val axisVariables = timeAxisLayer.paintingVariables()

    gc.fill(SignalColors.ground)
    gc.fillRect(0.0, 0.0, gc.width, gc.height)

    gc.font(FontDescriptorFragment(size = FontSize(14.0), weight = FontWeight.Bold))
    gc.fill(SignalColors.ink)
    // At the ends of the range the time zone offset moves the center out of the dates of LocalDate.
    val centerMillis = (viewport.centerSeconds * 1000.0).coerceIn(TimeTickDistance.MinSupportedMillis, TimeTickDistance.MaxSupportedMillis)
    val centerLabel = CalendarOffsetFormat.format(centerMillis, axisVariables.offsetTickDistance, paintingContext.i18nConfiguration)
    gc.fillText(centerLabel, gc.width - 16.0, pathBottom / 2 - 8.0, Direction.CenterRight)
    gc.font(FontDescriptorFragment(size = FontSize(11.0), weight = FontWeight.Normal))
    gc.fill(SignalColors.muted)
    gc.fillText("Fenster ${duration(viewport.visibleSeconds)} · Zoomstufe ${viewport.zoomLevel}", gc.width - 16.0, pathBottom / 2 + 9.0, Direction.CenterRight)

    gc.lineWidth = 1.0
    gc.strokeStyle(SignalColors.line)
    gc.strokeLine(0.0, pathBottom, gc.width, pathBottom)
    gc.strokeLine(ZeitstrahlPocLayout.TreeWidth, pathBottom, ZeitstrahlPocLayout.TreeWidth, gridBottom)

    gc.saved {
      gc.clip(ZeitstrahlPocLayout.TreeWidth, pathBottom, viewport.windowWidth, gridBottom - pathBottom)
      gc.strokeStyle(SignalColors.gridMajor)
      val paintGridLine: (Double) -> Unit = { millis ->
        if (millis.isFinite()) {
          val x = viewport.timeViewport.time2windowX(millis)
          gc.strokeLine(x, pathBottom, x, gridBottom)
        }
      }
      axisVariables.tickLabels.values.fastForEach(paintGridLine)
      axisVariables.offsetTickLabels.values.fastForEach(paintGridLine)
    }
  }

  /** "3.190 Jahre", "45 Tage", "6 Stunden", "12 Sekunden" */
  private fun duration(seconds: Double): String = when {
    seconds >= 1e9 * SecondsPerYear -> "${oneDecimal(seconds / SecondsPerYear / 1e9)} Mrd. Jahre"
    seconds >= 1e6 * SecondsPerYear -> "${oneDecimal(seconds / SecondsPerYear / 1e6)} Mio. Jahre"
    seconds >= 2 * SecondsPerYear -> "${grouped((seconds / SecondsPerYear).roundToLong())} Jahre"
    seconds >= 2 * SecondsPerDay -> "${grouped((seconds / SecondsPerDay).roundToLong())} Tage"
    seconds >= 2 * SecondsPerHour -> "${(seconds / SecondsPerHour).roundToLong()} Stunden"
    else -> "${oneDecimal(seconds)} Sekunden"
  }

  /** "66.000.000" */
  private fun grouped(value: Long): String = value.toString().reversed().chunked(3).joinToString(".").reversed()

  /** "13,8", "66" */
  private fun oneDecimal(value: Double): String {
    val tenths = (value * 10).roundToLong()
    val fraction = tenths % 10
    return if (fraction == 0L) grouped(tenths / 10) else "${grouped(tenths / 10)},$fraction"
  }

  private companion object {
    const val SecondsPerHour: Double = 3_600.0
    const val SecondsPerDay: Double = 24 * SecondsPerHour
    const val SecondsPerYear: Double = ZeitstrahlPocViewport.SecondsPerYear
  }
}
