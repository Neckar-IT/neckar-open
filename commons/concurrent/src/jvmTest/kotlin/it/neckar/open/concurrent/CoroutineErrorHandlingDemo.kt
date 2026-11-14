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
package it.neckar.open.concurrent

import assertk.*
import assertk.assertions.*
import kotlinx.coroutines.*

/**
 * Proves that *all* coroutines are canceled if one coroutine throws an exception.
 * Does *not* work in unit test because of the test context
 */
fun main(): Unit = runBlocking {
  println("1")
  delay(100)
  println("2")
  delay(0)
  println("3")
  delay(-100000)
  println("4")


  val exceptions = mutableListOf<Throwable>()

  val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { coroutineContext, throwable ->
    println("Handling exception $throwable")
    exceptions.add(throwable)
  })

  var job1Done = false

  val job1 = coroutineScope.launch {
    println("in job1")
    while (isActive) {
      delay(1000)
      println("Job1 done waiting")
    }

    println("job1 done")
    job1Done = true
  }

  var exceptionThrown = false

  val job2 = coroutineScope.launch(Dispatchers.IO) {
    println("in job2")
    delay(100)
    println("Job2: delay")
    exceptionThrown = true
    throw IllegalArgumentException("Uups")
  }

  assertThat(job1.isActive).isTrue()
  assertThat(job2.isActive).isTrue()
  assertThat(exceptionThrown).isFalse()
  assertThat(job1Done).isFalse()


  delay(150)


  assertThat(exceptionThrown).isTrue()
  assertThat(job1Done).isFalse()

  assertThat(job1.isActive).isTrue()
  assertThat(job2.isActive).isFalse()

  println("---")

  job1.join()

  assertThat(exceptions).hasSize(1)
}
