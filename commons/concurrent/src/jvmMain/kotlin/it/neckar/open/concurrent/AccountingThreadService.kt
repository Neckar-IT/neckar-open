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

import it.neckar.open.exceptions.CanceledException
import it.neckar.open.unit.si.ms
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.Timer

/**
 * Keeps references to the threads that have been created
 *
 */
public class AccountingThreadService(
  /**
   * The thread factory that is used to create the threads
   */
  private val threadFactory: ThreadFactory
) : ThreadService {
  private val threads = Collections.synchronizedList(ArrayList<WeakReference<Thread>>())
  private val executorServices = Collections.synchronizedList(ArrayList<WeakReference<ExecutorService>>())
  private val swingTimers = Collections.synchronizedList(ArrayList<WeakReference<Timer>>())
  private val timers = Collections.synchronizedList(ArrayList<WeakReference<java.util.Timer>>())

  private val isShutdown = AtomicBoolean()

  override fun newThread(r: Runnable): Thread {
    ensureNotShutdown()

    val thread = threadFactory.newThread(r)
    threads.add(WeakReference(thread))
    return thread
  }

  override fun newFixedThreadPool(nThreads: Int, name: String): ExecutorService {
    ensureNotShutdown()

    val executorService = Executors.newFixedThreadPool(nThreads, NamedThreadFactory(this, name))
    executorServices.add(WeakReference(executorService))
    return executorService
  }

  override fun newCachedThreadPool(name: String): ExecutorService {
    ensureNotShutdown()

    val executorService = Executors.newCachedThreadPool(NamedThreadFactory(this, name))
    executorServices.add(WeakReference(executorService))
    return executorService
  }

  override fun newScheduledThreadPool(nThreads: Int, name: String): ScheduledExecutorService {
    ensureNotShutdown()

    val executor = ScheduledThreadPoolExecutor(nThreads, NamedThreadFactory(this, name))
    executorServices.add(WeakReference(executor))
    return executor
  }

  override fun newTimer(name: String): java.util.Timer {
    ensureNotShutdown()
    val timer = java.util.Timer()
    timers.add(WeakReference(timer))
    return timer
  }

  override fun newSwingTimer(@ms delay: Int): Timer {
    ensureNotShutdown()
    val swingTimer = Timer(delay, null)
    swingTimers.add(WeakReference(swingTimer))
    return swingTimer
  }

  public fun getThreads(): List<WeakReference<Thread>> {
    return Collections.unmodifiableList(threads)
  }

  public fun getExecutorServices(): List<WeakReference<ExecutorService>> {
    return Collections.unmodifiableList(executorServices)
  }

  private fun ensureNotShutdown() {
    if (isShutdown.get()) {
      throw CanceledException()
    }
  }

  public fun shutdown() {
    isShutdown.set(true)

    //interrupt all threads
    for (threadRef in threads) {
      val thread = threadRef.get()
      thread?.interrupt()
    }

    for (executorService in executorServices) {
      val service = executorService.get()
      service?.shutdownNow()
    }

    //close all timers
    for (timerReference in swingTimers) {
      val timer = timerReference.get()
      timer?.stop()
    }

    for (timerReference in timers) {
      val timer = timerReference.get()
      timer?.cancel()
    }
  }

  @Throws(InterruptedException::class)
  public fun awaitTermination(timeout: Int, timeUnit: TimeUnit): Boolean {
    var success = true
    for (executorService in executorServices) {
      val service = executorService.get()
      if (service != null) {
        if (!service.awaitTermination(timeout.toLong(), timeUnit)) {
          success = false
        }
      }
    }
    return success
  }
}
