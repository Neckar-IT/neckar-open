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
import assertk.*
import assertk.assertions.*
import it.neckar.react.common.form.parseTimePickerValue
import it.neckar.react.common.form.toTimePickerString
import kotlin.js.Date
import kotlin.test.Test

class DateAndTimeInputFieldsTest {
  @Test
  fun testToTimePickerStringKeepsAlignedMinutes() {
    //A stored time that is already on a step boundary must render unchanged - not jump a full step.
    assertThat(Date(2020, 0, 1, 10, 0).toTimePickerString(15)).isEqualTo("10:00")
    assertThat(Date(2020, 0, 1, 10, 15).toTimePickerString(15)).isEqualTo("10:15")
    assertThat(Date(2020, 0, 1, 10, 30).toTimePickerString(15)).isEqualTo("10:30")
    assertThat(Date(2020, 0, 1, 10, 45).toTimePickerString(15)).isEqualTo("10:45")
  }

  @Test
  fun testToTimePickerStringRoundsUpToStep() {
    //Non-aligned minutes still round up to the next step boundary.
    assertThat(Date(2020, 0, 1, 10, 7).toTimePickerString(15)).isEqualTo("10:15")
    assertThat(Date(2020, 0, 1, 10, 1).toTimePickerString(15)).isEqualTo("10:15")
  }

  @Test
  fun testToTimePickerStringNoStep() {
    assertThat(Date(2020, 0, 1, 10, 7).toTimePickerString()).isEqualTo("10:07")
  }

  @Test
  fun testParseTimePickerValue() {
    assertThat("10:30".parseTimePickerValue()).isEqualTo(Date(0, 0, 0, 10, 30).getTime())
    assertThat("00:00".parseTimePickerValue()).isEqualTo(Date(0, 0, 0, 0, 0).getTime())
    //A time input with a sub-minute step appends seconds - they are ignored.
    assertThat("10:30:00".parseTimePickerValue()).isEqualTo(Date(0, 0, 0, 10, 30).getTime())
  }

  @Test
  fun testParseTimePickerValueRejectsIncompleteInput() {
    //Clearing the field must clear the state instead of throwing.
    assertThat("".parseTimePickerValue()).isNull()
    assertThat("   ".parseTimePickerValue()).isNull()
    assertThat("10".parseTimePickerValue()).isNull()
    assertThat("10:".parseTimePickerValue()).isNull()
    assertThat(":30".parseTimePickerValue()).isNull()
    assertThat("hh:mm".parseTimePickerValue()).isNull()
  }
}
