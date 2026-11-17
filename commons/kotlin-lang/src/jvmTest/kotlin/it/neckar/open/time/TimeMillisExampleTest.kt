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
package it.neckar.open.time

import assertk.*
import assertk.assertions.*
import it.neckar.open.collections.fastForEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 *
 */
class TimeMillisExampleTest {
  @Test
  fun testIt() {
    assertThat(TimeMillisExample.Timestamp20200521150041500.formatUtcForDebug()).isEqualTo("2020-05-21T15:00:41.500Z")

    TimeMillisExample.entries.fastForEach {
      assertThat(it.formatUtcForDebug()).isNotEmpty()
    }
  }

  @Test
  fun testInstant2Minis() {
    val instant = OffsetDateTime.of(2026, 9, 30, 17, 38, 12, 123, ZoneOffset.UTC)
    assertThat(instant.toInstant().toEpochMilli().toDouble()).isEqualTo(1.790789892E12)
  }
}
