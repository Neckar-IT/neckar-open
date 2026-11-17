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
package it.neckar.open.javafx

import it.neckar.open.time.DateUtils
import javafx.beans.binding.Bindings
import javafx.beans.binding.StringBinding
import javafx.beans.value.ObservableValue
import java.time.Duration
import java.time.LocalTime
import java.time.Period
import java.time.temporal.Temporal
import java.util.concurrent.Callable

/**
 * Extension methods for Date and time related stuff
 */

/**
 * Format in HH:mm format
 */
fun ObservableValue<LocalTime?>.asHHmm(): StringBinding {
  return Bindings.createStringBinding(Callable {
    this.value?.asHHmm().orEmpty()
  }, this)
}

fun LocalTime.asHHmm(): String {
  return DateUtils.formatHHmm(this)
}


fun ObservableValue<Duration?>.asWords(): StringBinding {
  return Bindings.createStringBinding(Callable {
    this.value?.asWords().orEmpty()
  }, this)
}


fun Duration.asWords(): String {
  return DateUtils.formatDurationWords(this)
}

fun Duration.asHHmm(): String {
  return DateUtils.formatDurationHHmm(this)
}

fun Temporal.formatLocalDateAndOrTime(): String {
  return DateUtils.formatLocalDateAndOrTime(this)
}


fun ObservableValue<Period?>.asWeeksAndDays(): StringBinding {
  return Bindings.createStringBinding(Callable {
    this.value?.asWeeksAndDays().orEmpty()
  }, this)
}

fun Period.asWeeksAndDays(): String {
  return DateUtils.asWeeksAndDays(this)
}




