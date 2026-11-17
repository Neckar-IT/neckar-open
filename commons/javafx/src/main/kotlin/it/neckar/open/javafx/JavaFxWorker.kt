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

import it.neckar.open.annotations.NonUiThread
import it.neckar.open.annotations.UiThread
import it.neckar.open.kotlin.lang.requireNotNull
import javafx.application.Platform
import javafx.concurrent.Task
import java.util.concurrent.ExecutionException
import javax.annotation.Nonnull

/**
 * Similar to a simple SwingWorker.
 * Must not return "null". Instead, use [java.util.Optional]
 *
 */
abstract class JavaFxWorker<T> : Task<T>() {
  @Throws(Exception::class)
  override fun call(): T? {
    return doInBackground()
  }

  @Throws(Exception::class)
  protected abstract fun doInBackground(): T

  @UiThread
  final override fun succeeded() {
    super.succeeded()
  }

  @UiThread
  final override fun running() {
    super.running()
  }

  @UiThread
  final override fun cancelled() {
    super.cancelled()
  }

  @UiThread
  final override fun failed() {
    super.failed()
  }

  @NonUiThread
  override fun done() {
    super.done()
    Platform.runLater {
      cleanup()
      try {
        success(get())
      } catch (e: InterruptedException) {
        failure(e)
      } catch (e: ExecutionException) {
        failure(e.cause ?: e)
      } catch (e: Exception) {
        failure(e)
      }
    }
  }

  /**
   * Is called before #done(T);
   * This method is called even if [.doInBackground] has thrown an exception
   */
  @UiThread
  protected open fun cleanup() {
  }

  /**
   * Is called the value that is returned by [.doInBackground].
   * Is only called if no exception has been thrown in [.doInBackground].
   */
  @UiThread
  protected open fun success(t: T) {
  }

  @UiThread
  protected open fun failure(e: Throwable) {
    throw e
  }

  /**
   * Schedules for execution
   *
   * @return the thread this worker runs its background stuff on. Safe to ignore when only
   * the execution side-effect is needed.
   */
  @Nonnull
  @IgnorableReturnValue
  fun execute(): Thread {
    val thread = Thread(this, "JavaFXWorker-Thread for <" + javaClass.name + ">")
    thread.start()
    return thread
  }

  companion object {
    /**
     * Creates a very simple fx worker
     */
    fun <T> create(backgroundAction: () -> T): JavaFxWorker<T> {
      return object : JavaFxWorker<T>() {
        override fun doInBackground(): T {
          return backgroundAction()
        }
      }
    }

    fun <T> build(configuration: Builder<T>.() -> Unit): Builder<T> {
      return Builder<T>().also {
        it.configuration()
      }
    }

    operator fun <T> invoke(configuration: Builder<T>.() -> Unit) {
      build(configuration)
        .build()
        .execute()
    }
  }

  /**
   * A builder for a javafx worker
   */
  class Builder<T> {
    var backgroundAction: (() -> T)? = null

    var onCleanup: () -> Unit = {}

    var onSuccess: (t: T?) -> Unit = {}
    var onFailure: (e: Throwable) -> Unit = {}

    fun build(): JavaFxWorker<T> {
      val backgroundActionCopy = backgroundAction.requireNotNull {
        "No backgroundAction has been set!"
      }

      return object : JavaFxWorker<T>() {
        override fun doInBackground(): T {
          return backgroundActionCopy.invoke()
        }

        override fun cleanup() {
          super.cleanup()
          onCleanup()
        }

        override fun success(t: T) {
          super.success(t)
          onSuccess(t)
        }

        override fun failure(e: Throwable) {
          super.failure(e)
          onFailure(e)
        }
      }
    }
  }
}
