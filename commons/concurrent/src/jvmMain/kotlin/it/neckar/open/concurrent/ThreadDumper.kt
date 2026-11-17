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

import java.io.StringWriter
import java.io.Writer
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.util.Arrays

/**
 * Dumps threads
 *
 */
public object ThreadDumper {

  //Fallback if the other method does not work
  @JvmStatic
  public val threadInfos: Array<ThreadInfo>
    get() {
      val threadMXBean = ManagementFactory.getThreadMXBean()

      try {
        return sort(threadMXBean.dumpAllThreads(false, false))
      } catch (ignored: Exception) {
      }

      val threadIds = threadMXBean.allThreadIds
      return sort(threadMXBean.getThreadInfo(threadIds, Integer.MAX_VALUE))
    }

  @JvmStatic
  public fun dumpThreads(threadInfos: Array<ThreadInfo>): String {
    val writer = StringWriter()
    dumpThreads(threadInfos, writer)
    return writer.toString()
  }

  /**
   * Dumps the threads to the given [writer]. Returns the stack trace of the EDT as a
   * convenience when available; callers that only care about the writer output may
   * safely ignore the return value.
   */
  @IgnorableReturnValue
  @JvmStatic
  public fun dumpThreads(threadInfos: Array<ThreadInfo>, writer: Writer): Array<StackTraceElement>? {
    var edtStack: Array<StackTraceElement>? = null

    for (info in threadInfos) {
      if (info.threadName.startsWith("AWT-EventQueue-")) {
        edtStack = info.stackTrace
      }
      dumpThreadInfo(info, writer)
    }

    return edtStack
  }

  @JvmStatic
  public fun dumpThreads(writer: Writer): Array<StackTraceElement>? {
    return dumpThreads(threadInfos, writer)
  }

  private fun sort(threads: Array<ThreadInfo>): Array<ThreadInfo> {
    Arrays.sort(threads, fun(o1: ThreadInfo, o2: ThreadInfo): Int {
      val t1 = o1.threadName
      val t2 = o2.threadName
      if (t1.startsWith("AWT-EventQueue")) {
        return -1
      }
      if (t2.startsWith("AWT-EventQueue")) {
        return 1
      }
      val r1 = o1.threadState == Thread.State.RUNNABLE
      val r2 = o2.threadState == Thread.State.RUNNABLE
      if (r1 && !r2) {
        return -1
      }
      if (r2 && !r1) {
        return 1
      }
      return 0
    })

    return threads
  }

  @JvmStatic
  public fun dumpThreadInfo(info: ThreadInfo, writer: Writer) {
    dumpCallStack(info, writer, info.stackTrace)
  }

  private fun dumpCallStack(info: ThreadInfo, writer: Writer, stackTraceElements: Array<StackTraceElement>) {
    val sb = StringBuilder("\"").append(info.threadName).append("\"")
    sb.append(" prio=0 tid=0x0 nid=0x0 ").append(getReadableState(info.threadState)).append("\n")
    sb.append("     java.lang.Thread.State: ").append(info.threadState).append("\n")
    if (info.lockName != null) {
      sb.append(" on ").append(info.lockName)
    }
    if (info.lockOwnerName != null) {
      sb.append(" owned by \"").append(info.lockOwnerName).append("\" Id=").append(info.lockOwnerId)
    }
    if (info.isSuspended) {
      sb.append(" (suspended)")
    }
    if (info.isInNative) {
      sb.append(" (in native)")
    }

    writer.write(sb.toString() + "\n")
    for (element in stackTraceElements) {
      writer.write("\tat $element\n")
    }
    writer.write("\n")
  }

  private fun getReadableState(state: Thread.State): String {
    when (state) {
      Thread.State.BLOCKED                             -> return "blocked"
      Thread.State.TIMED_WAITING, Thread.State.WAITING -> return "waiting on condition"
      Thread.State.RUNNABLE                            -> return "runnable"
      Thread.State.NEW                                 -> return "new"
      Thread.State.TERMINATED                          -> return "terminated"
    }
  }
}
/**
 * Creates a string containing all thread dumps
 */
