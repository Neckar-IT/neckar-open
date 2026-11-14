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
package it.neckar.open.version

import assertk.*
import assertk.assertions.*
import com.google.common.base.Splitter
import it.neckar.open.version.Version.Companion.parse
import java.util.concurrent.Callable
import javax.annotation.Nonnull

/**
 *
 */
object VersionParsingPerformance {
  @Nonnull
  val DOT_SPLITTER: Splitter = Splitter.on(".")

  @Nonnull
  val VERSION: String = "1.2.3-suffix"

  @JvmStatic
  fun main(args: Array<String>) {
    run("String.plit") {
      val parts = VERSION.split("\\.").dropLastWhile { it.isEmpty() }.toTypedArray()
      require(parts.size == 3) { "Version <$VERSION> must contain exactly three parts delimited with '.'" }
      val build: Int
      val suffix: String?
      if (parts[2].contains("-")) {
        val firstIndex = parts[2].indexOf('-')
        val buildAsString = parts[2].substring(0, firstIndex)
        build = buildAsString.toInt()
        suffix = parts[2].substring(firstIndex + 1, parts[2].length)
      } else {
        build = parts[2].toInt()
        suffix = null
      }
      val major = parts[0].toInt()
      val minor = parts[1].toInt()
      Version(major, minor, build, suffix)
    }
    run("static Splitter") {
      val parts = DOT_SPLITTER.split(VERSION)
      val iterator: Iterator<String> = parts.iterator()
      val part0 = iterator.next()
      val part1 = iterator.next()
      val part2 = iterator.next()
      require(!iterator.hasNext()) { "In" }
      val index = part2.indexOf("-")
      if (index > -1) {
        val suffix = part2.substring(index + 1)
        val build = part2.substring(0, index).toInt()
        Version(part0.toInt(), part1.toInt(), build, suffix)
      } else {
        Version(part0.toInt(), part1.toInt(), part0.toInt())
      }
    }
    run("version.parse") { parse(VERSION) }
    run("indexOf") {
      val index0 = VERSION.indexOf(".")
      val index1 = VERSION.indexOf(".", index0 + 2)
      val indexMinus = VERSION.indexOf("-", index1 + 2)
      require(!(index0 == -1 || index1 == -1))
      val major = VERSION.substring(0, index0).toInt()
      val minor = VERSION.substring(index0 + 1, index1).toInt()
      val build = VERSION.substring(index1 + 1, indexMinus).toInt()
      val suffix = VERSION.substring(indexMinus + 1)
      Version(major, minor, build, suffix)
    }
  }

  private fun run(@Nonnull description: String, @Nonnull callable: Callable<Version>) {
    // Warmup
    for (i in 0..9999999) {
      (val major, val minor, val build, val suffix) = callable.call()
      assertThat(major.toLong()).isEqualTo(1)
      assertThat(minor.toLong()).isEqualTo(2)
      assertThat(build.toLong()).isEqualTo(3)
      assertThat(suffix).isEqualTo("suffix")
    }

    // Do the work
    val start = System.currentTimeMillis()
    for (i in 0..9999999) {
      (val major, val minor, val build, val suffix) = callable.call()
      assertThat(major.toLong()).isEqualTo(1)
      assertThat(minor.toLong()).isEqualTo(2)
      assertThat(build.toLong()).isEqualTo(3)
      assertThat(suffix).isEqualTo("suffix")
    }
    val end = System.currentTimeMillis()
    println("$description took ${end - start}")
  }
}
