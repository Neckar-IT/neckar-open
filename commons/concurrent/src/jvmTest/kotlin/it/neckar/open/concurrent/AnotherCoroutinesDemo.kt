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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.system.measureTimeMillis

fun main(): Unit = runBlocking(CoroutineName("MyCoroutineName") + Dispatchers.Default) {
  println("Starting demo in ${Thread.currentThread().name}")


  val eventFlow = scheduleEventFlow()


  val collectJob1 = launch {
    eventFlow.onEach {
      println("1 Received $it in ${Thread.currentThread().name}")
    }.collect()
  }

  val collecting = mutableListOf<String>()

  val collectJob2 = eventFlow.onEach {
    println("2 Received $it in ${Thread.currentThread().name}")
    collecting.add(it)
  }.launchIn(this)


  delay(400)
  collectJob1.cancel()

  delay(999)
  println("Stopping emitter")

  collectJob2.cancel()
  println("Collected ${collecting.size} elements")

  measureTimeMillis {
    collectJob1.cancelAndJoin()
  }.also { println("Stopped collectJob after $it ms") }


  //
  //
  // Cleanup!
  //
  //

  measureTimeMillis {
    emitterJob.cancelAndJoin()
  }.also { println("Stopped emitterJob after $it ms") }

  println("Done!")
}


lateinit var emitterJob: Job

/**
 * Simulates a event source
 */
fun CoroutineScope.scheduleEventFlow(): SharedFlow<String> {
  val mutableSharedFlow = MutableSharedFlow<String>(2)

  emitterJob = launch(Dispatchers.IO) {
    var counter = 0
    while (isActive) {
      val value = "Entry $counter"
      println("Emitting $value")
      mutableSharedFlow.emit(value)
      counter++
      delay(100)
    }
  }

  return mutableSharedFlow.asSharedFlow()
}


fun createFlow(): Flow<String> {
  return flow {
    println("in string")

    var counter = 0

    while (currentCoroutineContext().isActive) {
      this.emit("NewValue $counter")
      counter++
      delay(100)
    }
  }
}

