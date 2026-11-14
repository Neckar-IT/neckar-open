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
package it.neckar.time

import assertk.*
import assertk.assertions.*
import it.neckar.open.time.formatUtcForDebug
import it.neckar.open.time.millis2Instant
import it.neckar.open.unit.si.ms
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TimeExtTest {
  val time: @ms Double = 1.5468559191231233E12

  @Test
  fun testInstant() {
    assertThat(time.formatUtcForDebug()).isEqualTo("2019-01-07T10:11:59.123Z")

    val instant = millis2Instant(time)
    val zonedDateTime = instant.atZone(ZoneId.of("Europe/Berlin"))
    assertThat(DateTimeFormatter.ISO_DATE_TIME.format(zonedDateTime)).isEqualTo("2019-01-07T11:11:59.123123291+01:00[Europe/Berlin]")
  }
}
