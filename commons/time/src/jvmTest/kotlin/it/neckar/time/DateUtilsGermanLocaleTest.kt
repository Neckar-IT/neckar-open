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
import it.neckar.open.time.DateUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.DefaultLocale
import java.time.Duration

/**
 */
@DefaultLocale("de-DE")
class DateUtilsGermanLocaleTest {
  @Test
  @Throws(Exception::class)
  fun words() {
    assertThat(DateUtils.formatDurationWords(Duration.ofHours(13).plusMinutes(10))).isEqualTo("13 Stunden 10 Minuten")
    assertThat(DateUtils.formatDurationWords(Duration.ofHours(1).plusMinutes(1).plusSeconds(1))).isEqualTo("1 Stunde 1 Minute 1 Sekunde")
  }

  @Test
  fun `zero duration uses the localized seconds string`() {
    assertThat(DateUtils.formatDurationWords(Duration.ZERO)).isEqualTo("0 Sekunden")
  }
}
