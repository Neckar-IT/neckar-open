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
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.Ignore
import kotlin.test.Test


/**
 * Throwing a JavaScript string inside a coroutine does not stop the job on Kotlin/JS, so the test is ignored.
 *
 * workaround: dependency=libs:kotlin:2.4.20 observed-on=2026-10-07 upstream=https://youtrack.jetbrains.com/issue/KT-72174
 */
class CoroutinesExceptionJsTest {
  @Ignore
  @Test
  fun testErrorHandler(): TestResult = runTest {
    println("starting testErrorHandler|")
    val errors = mutableListOf<Throwable>()

    println("1a|")

    val appScope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
      println("called error for $throwable")
      errors.add(throwable)
    })

    println("1b|")

    assertThat(errors).isEmpty()

    println("1c|")

    appScope.launch(testScheduler) {
      throw RuntimeException("Uuups")
    }.join()

    println("2|")
    assertThat(errors).hasSize(1)
    assertThat(errors.first()).isInstanceOf(RuntimeException::class)

    appScope.launch(testScheduler) {
      throw Error("An error")
    }.join()

    println("3|")
    assertThat(errors).hasSize(2)
    assertThat(errors.first()).isInstanceOf(RuntimeException::class)
    assertThat(errors[1]).isInstanceOf(Error::class)

    println("3.5|")

    val job = appScope.launch(testScheduler) {
      println("in job|")
      throw js(""" "Empty!" """) //In JS everything can be thrown, even a String!
      println("after throwing a string|")
    }

    println("3.6|")
    job.join()

    println("4|")
    assertThat(errors).hasSize(3)
    assertThat(errors.first()).isInstanceOf(RuntimeException::class)
    assertThat(errors[1]).isInstanceOf(Error::class)
    assertThat(errors[2]).isInstanceOf(String::class)

    println("5|")
  }

  @Ignore
  @Test
  fun testThrowString() = runTest {
    val job = launch {
      println("throwing a JS string")
      throw js(""" "Empty!" """) //In JS everything can be thrown, even a String!
      println("not executed....")
    }

    println("waiting for job")
    job.join()
  }
}
