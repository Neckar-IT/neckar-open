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

import com.meistercharts.algorithms.layers.LayerPaintingContext
import com.meistercharts.axis.time.DistanceDays
import com.meistercharts.axis.time.DistanceHours
import com.meistercharts.axis.time.DistanceMillis
import com.meistercharts.axis.time.DistanceMinutes
import com.meistercharts.axis.time.DistanceMonths
import com.meistercharts.axis.time.DistanceSeconds
import com.meistercharts.axis.time.DistanceYears
import com.meistercharts.axis.time.TimeTickDistance
import it.neckar.datetime.minimal.LocalDateTime
import it.neckar.datetime.minimal.fromMillisCurrentTimeZone
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.unit.other.px
import it.neckar.open.unit.si.ms

/**
 * Labels every tick from the Big Bang to a millisecond in the calendar of [CalendarLabels]:
 * "vor 66 Mio. Jahren", "44 v. Chr.", "1789", "14. Juli", "14:00", "14:00:00.100".
 */
object CalendarTickFormat : TimeAxisTickFormat {
  override fun format(tick: @ms Double, tickDistance: TimeTickDistance, i18nConfiguration: I18nConfiguration): String {
    val labels = CalendarLabels.of(i18nConfiguration)
    val dateTime = LocalDateTime.fromMillisCurrentTimeZone(tick, i18nConfiguration.timeZone)
    return when (tickDistance) {
      is DistanceYears -> labels.year(dateTime.year, tickDistance)
      is DistanceMonths -> labels.month(dateTime.date)
      is DistanceDays -> labels.dayOfMonth(dateTime.date)
      is DistanceHours, is DistanceMinutes -> labels.hoursMinutes(dateTime.time)
      is DistanceSeconds -> labels.hoursMinutesSeconds(dateTime.time)
      is DistanceMillis -> labels.hoursMinutesSecondsMillis(dateTime.time)
    }
  }

  override fun computeMaxTickLength(paintingContext: LayerPaintingContext, tickDistance: TimeTickDistance): @px Double {
    return paintingContext.gc.calculateTextWidth("vor 13,25 Mrd. Jahren")
  }
}
