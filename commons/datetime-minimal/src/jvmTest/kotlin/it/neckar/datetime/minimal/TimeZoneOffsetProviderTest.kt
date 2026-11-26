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
package it.neckar.datetime.minimal

import assertk.*
import assertk.assertions.*
import it.neckar.open.time.formatUtcForDebug
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

class TimeZoneOffsetProviderTest {

  @Test
  fun testOffsets() {
    val timeZoneOffsetProvider = DefaultTimeZoneOffsetProvider()
    assertThat(timeZoneOffsetProvider.offset(1607867526127.0, TimeZone("Europe/Berlin"))).isEqualTo(1.hours.toDouble(DurationUnit.MILLISECONDS))
    assertThat(timeZoneOffsetProvider.offset(1607867526127.0, TimeZone("Africa/Asmara"))).isEqualTo(3.hours.toDouble(DurationUnit.MILLISECONDS))
    assertThat(timeZoneOffsetProvider.offset(1607867526127.0, TimeZone("America/Aruba"))).isEqualTo((-4).hours.toDouble(DurationUnit.MILLISECONDS))
    assertThat(timeZoneOffsetProvider.offset(1607867526127.0, TimeZone("Indian/Christmas"))).isEqualTo(7.hours.toDouble(DurationUnit.MILLISECONDS))
    assertThat(timeZoneOffsetProvider.offset(1607867526127.0, TimeZone("Pacific/Guadalcanal"))).isEqualTo(11.hours.toDouble(DurationUnit.MILLISECONDS))
    assertThat(timeZoneOffsetProvider.offset(1607867526127.0, TimeZone("Africa/Algiers"))).isEqualTo(1.hours.toDouble(DurationUnit.MILLISECONDS))
    assertThat(timeZoneOffsetProvider.offset(1607867526127.0, TimeZone("America/Adak"))).isEqualTo((-10).hours.toDouble(DurationUnit.MILLISECONDS))
    assertThat(timeZoneOffsetProvider.offset(1607867526127.0, TimeZone("Europe/Tallinn"))).isEqualTo(2.hours.toDouble(DurationUnit.MILLISECONDS))
  }

  @Test
  fun testTimestampAlignment() {
    assertThat(1607864400000.0.formatUtcForDebug()).isEqualTo("2020-12-13T13:00:00.000Z")
    assertThat(DefaultCachedTimeZoneOffsetProvider.alignTimestampToTimeZones(1607864400000.0).formatUtcForDebug()).isEqualTo("2020-12-13T13:00:00.000Z")

    assertThat(1607864401000.0.formatUtcForDebug()).isEqualTo("2020-12-13T13:00:01.000Z")
    assertThat(DefaultCachedTimeZoneOffsetProvider.alignTimestampToTimeZones(1607864401000.0).formatUtcForDebug()).isEqualTo("2020-12-13T13:00:00.000Z")

    assertThat(1607865299000.0.formatUtcForDebug()).isEqualTo("2020-12-13T13:14:59.000Z")
    assertThat(DefaultCachedTimeZoneOffsetProvider.alignTimestampToTimeZones(1607865299000.0).formatUtcForDebug()).isEqualTo("2020-12-13T13:00:00.000Z")

    assertThat(1607865300000.0.formatUtcForDebug()).isEqualTo("2020-12-13T13:15:00.000Z")
    assertThat(DefaultCachedTimeZoneOffsetProvider.alignTimestampToTimeZones(1607865300000.0).formatUtcForDebug()).isEqualTo("2020-12-13T13:15:00.000Z")

    assertThat(1607865301000.0.formatUtcForDebug()).isEqualTo("2020-12-13T13:15:01.000Z")
    assertThat(DefaultCachedTimeZoneOffsetProvider.alignTimestampToTimeZones(1607865301000.0).formatUtcForDebug()).isEqualTo("2020-12-13T13:15:00.000Z")

    assertThat(1607866199000.0.formatUtcForDebug()).isEqualTo("2020-12-13T13:29:59.000Z")
    assertThat(DefaultCachedTimeZoneOffsetProvider.alignTimestampToTimeZones(1607866199000.0).formatUtcForDebug()).isEqualTo("2020-12-13T13:15:00.000Z")

    assertThat(1607866200000.0.formatUtcForDebug()).isEqualTo("2020-12-13T13:30:00.000Z")
    assertThat(DefaultCachedTimeZoneOffsetProvider.alignTimestampToTimeZones(1607866200000.0).formatUtcForDebug()).isEqualTo("2020-12-13T13:30:00.000Z")

    assertThat(1607866201000.0.formatUtcForDebug()).isEqualTo("2020-12-13T13:30:01.000Z")
    assertThat(DefaultCachedTimeZoneOffsetProvider.alignTimestampToTimeZones(1607866201000.0).formatUtcForDebug()).isEqualTo("2020-12-13T13:30:00.000Z")

    assertThat(1607867099000.0.formatUtcForDebug()).isEqualTo("2020-12-13T13:44:59.000Z")
    assertThat(DefaultCachedTimeZoneOffsetProvider.alignTimestampToTimeZones(1607867099000.0).formatUtcForDebug()).isEqualTo("2020-12-13T13:30:00.000Z")

    assertThat(1607867100000.0.formatUtcForDebug()).isEqualTo("2020-12-13T13:45:00.000Z")
    assertThat(DefaultCachedTimeZoneOffsetProvider.alignTimestampToTimeZones(1607867100000.0).formatUtcForDebug()).isEqualTo("2020-12-13T13:45:00.000Z")

    assertThat(1607867101000.0.formatUtcForDebug()).isEqualTo("2020-12-13T13:45:01.000Z")
    assertThat(DefaultCachedTimeZoneOffsetProvider.alignTimestampToTimeZones(1607867101000.0).formatUtcForDebug()).isEqualTo("2020-12-13T13:45:00.000Z")

    assertThat(1607867999000.0.formatUtcForDebug()).isEqualTo("2020-12-13T13:59:59.000Z")
    assertThat(DefaultCachedTimeZoneOffsetProvider.alignTimestampToTimeZones(1607867999000.0).formatUtcForDebug()).isEqualTo("2020-12-13T13:45:00.000Z")
  }
}
