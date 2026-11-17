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
package it.neckar.open.observable

import assertk.*
import assertk.assertions.*
import it.neckar.open.dispose.Disposable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 *
 */
class ObservableCoroutinesTest {

  @Test
  fun `asFlow emits a change made after the collector started, not the current value`() = runTest {
    val observable = ObservableString("initial")
    val first = async { observable.asFlow().first() }
    runCurrent()

    observable.value = "changed"

    assertThat(first.await()).isEqualTo("changed")
  }

  @Disabled("test blocks")
  @Test
  fun testFlow() = runTest {
    val fooBar = FooBar()
    val flow = fooBar.asFlow()

    val received = CompletableDeferred<Any?>()

    val job = launch(Dispatchers.Default + CoroutineName("LaunchFlowCollector")) {
      println("Start collecting flow")
      val scope = CoroutineScope(Dispatchers.Default)

      flow
        .collect {
          received.complete(it)
          println("## Flow collected: $it. Will call cancel")
          cancel() // stop after first val
          println("## cancel called")
        }
    }

    assertThat(received.isActive).isTrue()

    runBlocking(Dispatchers.Default) {
      println("Setting foo to new value")
      fooBar.foo = "the-new-value"
      println("done setting foo to new value")
    }


    println("Waiting for flow to be collected")
    advanceUntilIdle()

    val value = received.await()
    assertThat(value).isEqualTo("the-new-value")

    job.cancel()
  }

  private class FooBar : ObservableProperties {
    val fooProperty = ObservableObject("foo")
    var foo: String by fooProperty

    val barProperty = ObservableObject("bar")
    var bar: String by barProperty

    val observablePropertiesSupport = ObservablePropertiesSupport(
      listOf(
        fooProperty,
        barProperty,
      )
    )

    override fun consumeAllPropertiesChanges(action: ConsumeChangesAction<Any?>): Disposable {
      return observablePropertiesSupport.consumeAllPropertiesChanges(action)
    }
  }

  @Disabled("test blocks")
  @Test
  fun `callback sends value through flow`() = runTest {
    val emitter = CallbackEmitter()
    val flow = listenToCallback(emitter)

    // Wir starten eine Coroutine, die den Wert sendet – leicht verzögert, damit der Flow bereit ist
    launch {
      delay(10) // kleine Verzögerung, damit Flow .first() erreicht
      emitter.emit("Hello from real callback!")
    }

    val result = flow.first()
    assertThat(result).isEqualTo("Hello from real callback!")
  }
}

interface MyListener {
  fun onValue(value: String)
}

class CallbackEmitter {
  private var listener: MyListener? = null

  fun setListener(listener: MyListener?) {
    this@CallbackEmitter.listener = listener
  }

  fun emit(value: String) {
    listener?.onValue(value)
  }
}

fun listenToCallback(emitter: CallbackEmitter): Flow<String> = callbackFlow {
  val listener = object : MyListener {
    override fun onValue(value: String) {
      trySend(value)
    }
  }

  emitter.setListener(listener)
  awaitClose { emitter.setListener(null) }
}
