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

import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.unit.si.ms
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CopyOnWriteArraySet

private val logger: Logger = LoggerFactory.getLogger("it.neckar.open.concurrent.ThreadDeadlockDetector")

/**
 *
 * ThreadDeadlockDetector class.
 *
 */
public class ThreadDeadlockDetector
/**
 * It may be expensive to check for deadlocks, and it is not
 * critical to know so quickly.
 */
@JvmOverloads constructor(
  public val deadlockCheckPeriod: @ms Long = 10_000,
) {
  private val threadCheck = Timer("ThreadDeadlockDetector", true)
  private val mXBean = ManagementFactory.getThreadMXBean()
  private val listeners = CopyOnWriteArraySet<Listener>()

  public fun start() {
    threadCheck.scheduleAtFixedRate(object : TimerTask() {
      override fun run() {
        try {
          checkForDeadlocks()
        } catch (e: Exception) {
          //An exception leaving this method terminates the timer thread - every later check would be lost silently
          logger.warn("Deadlock check failed", e)
        }
      }
    }, this.deadlockCheckPeriod, this.deadlockCheckPeriod)
  }

  public fun stop() {
    threadCheck.cancel()
  }

  /**
   * A thread may terminate between the deadlock query and the lookup. Such a thread is skipped,
   * the remaining deadlocked threads are still reported.
   */
  private fun checkForDeadlocks() {
    val ids = findDeadlockedThreads() ?: return

    val threads = HashSet<Thread>()
    for (id in ids) {
      val threadInfo = mXBean.getThreadInfo(id) ?: continue
      threads.add(findMatchingThread(threadInfo) ?: continue)
    }

    if (threads.isNotEmpty()) {
      fireDeadlockDetected(threads)
    }
  }

  private fun findDeadlockedThreads(): LongArray? {
    // findDeadlockedThreads() also sees a cycle over an ownable synchronizer; the monitor-only
    // variant is the fallback for a JVM whose management bean does not support them.
    return if (mXBean.isSynchronizerUsageSupported) {
      mXBean.findDeadlockedThreads()
    } else {
      mXBean.findMonitorDeadlockedThreads()
    }
  }

  private fun fireDeadlockDetected(threads: Set<Thread>) {
    for (listener in listeners) {
      listener.deadlockDetected(threads)
    }
  }

  /**
   *
   * addListener
   *
   * @param listener a ThreadDeadlockDetector.Listener object.
   * @return a boolean.
   */
  public fun addListener(listener: Listener): Boolean {
    return listeners.add(listener)
  }

  /**
   *
   * removeListener
   *
   * @param listener a ThreadDeadlockDetector.Listener object.
   * @return a boolean.
   */
  public fun removeListener(listener: Listener): Boolean {
    return listeners.remove(listener)
  }

  /**
   * This is called whenever a problem with threads is detected.
   */
  public fun interface Listener {
    public fun deadlockDetected(deadlockedThreads: Set<Thread>)
  }

  public companion object {
    /**
     * Returns null if the thread has already terminated.
     */
    private fun findMatchingThread(threadInfo: ThreadInfo): Thread? {
      for (thread in Thread.getAllStackTraces().keys) {
        if (thread.id == threadInfo.threadId) {
          return thread
        }
      }
      return null
    }
  }
}
/**
 *
 * Constructor for ThreadDeadlockDetector.
 */
