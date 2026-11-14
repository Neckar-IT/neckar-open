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
package it.neckar.logging

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

class LoggerNameTest {
  @Test
  fun testShortened() {
    assertThat(LoggerName("it.neckar.logging.MyClass").shortened()).isEqualTo(ShortenedLoggerName("i.n.l.MyClass"))
    assertThat(LoggerName("MyClass").shortened()).isEqualTo(ShortenedLoggerName("MyClass"))
    assertThat(LoggerName("").shortened()).isEqualTo(ShortenedLoggerName(""))
    assertThat(LoggerName("foo.bar").shortened()).isEqualTo(ShortenedLoggerName("f.bar"))

    // Empty segments (leading dot, doubled dots) must not crash on parts[i][0].
    assertThat(LoggerName(".foo").shortened()).isEqualTo(ShortenedLoggerName(".foo"))
    assertThat(LoggerName("a..b").shortened()).isEqualTo(ShortenedLoggerName("a..b"))
    assertThat(LoggerName("foo.").shortened()).isEqualTo(ShortenedLoggerName("f."))
  }

  @Test
  fun testMatches() {
    ShortenedLoggerName("f.bar").let {
      assertThat(it.matches(LoggerName("foo.bar"))).isTrue()
      assertThat(it.matches(LoggerName("foo2.bar"))).isTrue()
      assertThat(it.matches(LoggerName("f.bar"))).isTrue()
      assertThat(it.matches(LoggerName("f.bar2"))).isFalse()
      assertThat(it.matches(LoggerName("f.ff"))).isFalse()
    }

    ShortenedLoggerName("i.n.l.MyClass").let {
      assertThat(it.matches(LoggerName("it.neckar.logging.MyClass"))).isTrue()
      assertThat(it.matches(LoggerName("it.neckar.logging.MyClass2"))).isFalse()
      assertThat(it.matches(LoggerName("it.nobody.logging.MyClass"))).isTrue()
      assertThat(it.matches(LoggerName("it.neckar.bbb.MyClass"))).isFalse()
    }
  }
}
