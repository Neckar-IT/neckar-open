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
package it.neckar.open.javafx

import com.sun.javafx.tk.Toolkit
import it.neckar.open.annotations.JavaFriendly
import it.neckar.open.annotations.NonBlocking
import it.neckar.open.dispose.Disposable
import it.neckar.open.time.monotonicTimeSource
import it.neckar.open.unit.si.ms
import it.neckar.open.unit.si.ns
import javafx.animation.AnimationTimer
import javafx.application.Platform
import javafx.util.Duration
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import kotlin.time.ComparableTimeMark
import kotlin.time.DurationUnit


/**
 * A class for timers executed on the JavaFX Application Thread.
 */
object JavaFxTimer {
  /**
   * Calls the given runnable on the JavaFX Application Thread after the given delay.
   */
  @Deprecated("Use delay instead", ReplaceWith("delay(delay, run)", "it.neckar.open.javafx.JavaFxTimer.delay"))
  @JvmStatic
  fun start(delay: Duration, run: Runnable): Disposable {
    return delay(delay, run)
  }

  /**
   * Calls the given lambda after the delay once
   */
  @JvmStatic
  fun delay(delay: Duration, run: () -> Unit): Disposable {
    if (delay.toMillis() == 0.0) {
      Platform.runLater(run)
      return Disposable.noop
    }

    return repeatWhile(delay) {
      run()
      Continuation.Stop
    }
  }

  /**
   * Returns the animation timer or null if the action has been scheduled immediately
   */
  fun delay(delay: kotlin.time.Duration, run: () -> Unit): Disposable {
    return delay(delay.toJavaFx(), run)
  }

  @JvmStatic
  @JavaFriendly
  fun delay(delay: kotlin.time.Duration, run: Runnable): Disposable {
    return delay(delay, run::run)
  }

  @JvmStatic
  @JavaFriendly
  fun delay(delay: Duration, run: Runnable): Disposable {
    return delay(delay, run::run)
  }

  /**
   * Repeats the action - as long as true is returned by the callable
   */
  @JvmStatic
  @JavaFriendly
  @NonBlocking
  fun repeatWhile(delay: Duration, run: Callable<Continuation>): Disposable {
    return repeatWhile(delay) {
      run.call()
    }
  }

  /**
   * Repeats the action - as long as true is returned by [run]
   */
  @NonBlocking
  fun repeatWhile(delay: Duration, run: () -> Continuation): Disposable {
    require(delay.toMillis() >= 1.0) {
      "A delay of at least 1 ms is required"
    }

    return object : AnimationTimer() {
      var lastRun: ComparableTimeMark = monotonicTimeSource.markNow()

      override fun handle(_nowNanos: @ns Long) {
        @ms val elapsedTimeSinceLastRun = lastRun.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
        if (elapsedTimeSinceLastRun < delay.toMillis()) {
          return
        }

        lastRun = monotonicTimeSource.markNow()

        when (run()) {
          Continuation.Continue -> {} //do nothing, just continue
          Continuation.Stop -> stop()
        }
      }
    }.let {
      it.start()
      Disposable {
        it.stop()
      }
    }
  }

  @JavaFriendly
  @JvmStatic
  @NonBlocking
  fun repeat(delay: Duration, run: Runnable): Disposable {
    return repeat(delay) {
      run.run()
    }
  }

  /**
   * Repeats the given lambda every [delay] (if possible).
   * Never executes the actions *before* [delay], but sometimes later
   */
  fun repeat(delay: Duration, run: () -> Unit): Disposable {
    return repeatWhile(delay) {
      run()
      Continuation.Continue
    }
  }

  /**
   * Repeats the given lambda every [delay] (if possible).
   * Never executes the actions *before* [delay], but sometimes later
   */
  fun repeat(delay: kotlin.time.Duration, run: () -> Unit): Disposable {
    return repeat(delay.toJavaFx(), run)
  }

  /**
   * Similar to SwingUtilities#invokeAndWait
   */
  @JvmStatic
  fun runAndWait(runnable: Runnable) {
    runAndWait(runnable::run)
  }

  /**
   * Executes the given lambda within the application thread
   */
  fun <T> runAndWait(runnable: () -> T): T {
    if (Platform.isFxApplicationThread()) {
      //run inline in fx thread
      return runnable()
    }

    return FutureTask<T>(Callable(runnable))
      .also {
        Platform.runLater(it)
      }.get()
  }

  /**
   * Similar to SwingUtilities#invokeAndWait
   *
   * @return the result of the [callable]
   */
  @JvmStatic
  fun <V> runAndWait(callable: Callable<V>): V {
    if (Platform.isFxApplicationThread()) {
      return callable.call()
    }

    return FutureTask(callable).also {
      Platform.runLater(it)
    }.get()
  }

  /**
   * Waits until the next paint pulse has been fired
   */
  fun waitForPaintPulse() {
    runAndWait { Toolkit.getToolkit().firePulse() }
  }

  /**
   * Waits until the condition provides true.
   * Checks every [waitDuration]
   */
  fun delayUntil(waitDuration: kotlin.time.Duration, condition: () -> Boolean, callback: () -> Unit): Disposable {
    return repeat(waitDuration) {
      if (condition()) {
        callback()
        Continuation.Stop
      } else {
        Continuation.Continue
      }
    }
  }
}


/**
 * Converts a Kotlin duration to a JavaFX duration
 */
fun kotlin.time.Duration.toJavaFx(): Duration = Duration.millis(toDouble(DurationUnit.MILLISECONDS))

enum class Continuation {
  Continue,
  Stop;

  companion object {
    @JvmStatic
    fun continueIf(shallContinue: Boolean): Continuation {
      return when (shallContinue) {
        true -> Continue
        false -> Stop
      }
    }

    fun continueIf(shallContinue: () -> Boolean): Continuation {
      return continueIf(shallContinue())
    }

    @JvmStatic
    fun stopIf(shallStop: Boolean): Continuation {
      return when (shallStop) {
        true -> Stop
        false -> Continue
      }
    }
  }
}
