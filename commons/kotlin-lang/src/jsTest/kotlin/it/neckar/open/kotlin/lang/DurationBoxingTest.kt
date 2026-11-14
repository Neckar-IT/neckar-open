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
package it.neckar.open.kotlin.lang

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class DurationBoxingTest {
  @Ignore
  @Test
  fun testBoxing() {
    val duration = 1.seconds
    println("#DurationBoxingTest.testBoxing")
    println(duration.inWholeMilliseconds)
    val result = js("typeof duration")
    println("the JS type of duration is $result")

    console.log("123")
    js("console.log('Duration:',duration)")
    console.log("456")

    println("a ${js("typeof duration")}")
    println("b ${js("Object.getOwnPropertyNames(duration)")}")
    println("c ${js("JSON.stringify(duration)")}")
  }

  @Ignore
  @Test
  fun testLong() {
    val myLong = 177777L
    println("#DurationBoxingTest.testLong")
    println(myLong)
    println("a ${js("typeof myLong")}")
    println("b ${js("Object.getOwnPropertyNames(myLong)")}")
    println("c ${js("JSON.stringify(myLong)")}")
  }
}
