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

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import javax.annotation.Nonnull

/**
 * Provides methods that support concurrency.
 *
 */
public object ConcurrentUtil {
  @JvmStatic
  public fun createSingleThreadScheduledExecutor(spawnDaemonThread: Boolean, threadName: String?): ScheduledExecutorService {
    return Executors.newSingleThreadScheduledExecutor(CustomizableThreadFactory(spawnDaemonThread, threadName))
  }

  @JvmStatic
  public fun createSingleThreadExecutor(spawnDaemonThread: Boolean, threadName: String?): ExecutorService {
    return Executors.newSingleThreadExecutor(CustomizableThreadFactory(spawnDaemonThread, threadName))
  }

  private class CustomizableThreadFactory(
    private val spawnDaemonThread: Boolean,
    private val threadName: String?,
  ) : ThreadFactory {
    override fun newThread(@Nonnull r: Runnable): Thread {
      val thread = Thread(r)
      thread.isDaemon = spawnDaemonThread
      if (threadName != null) {
        thread.name = threadName
      }
      return thread
    }
  }
}
