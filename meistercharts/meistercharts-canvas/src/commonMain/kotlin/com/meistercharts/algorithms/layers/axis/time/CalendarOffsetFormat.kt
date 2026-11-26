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
import it.neckar.open.unit.si.ms

/**
 * Labels the offset area in the calendar of [CalendarLabels]: "Juli 1789", "14. Juli 1789", "14. Juli 1789 14:00".
 */
object CalendarOffsetFormat : TimeAxisOffsetFormat {
  override fun format(offsetTick: @ms Double, offsetTickDistance: TimeTickDistance, i18nConfiguration: I18nConfiguration): String {
    val labels = CalendarLabels.of(i18nConfiguration)
    val dateTime = LocalDateTime.fromMillisCurrentTimeZone(offsetTick, i18nConfiguration.timeZone)
    return when (offsetTickDistance) {
      is DistanceYears -> labels.year(dateTime.year, offsetTickDistance)
      is DistanceMonths -> labels.month(dateTime.date)
      is DistanceDays -> labels.date(dateTime.date)
      is DistanceHours, is DistanceMinutes -> "${labels.date(dateTime.date)} ${labels.hoursMinutes(dateTime.time)}"
      is DistanceSeconds, is DistanceMillis -> "${labels.date(dateTime.date)} ${labels.hoursMinutesSeconds(dateTime.time)}"
    }
  }
}
