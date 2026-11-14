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
import org.awaitility.Awaitility
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Duration
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 *
 */
class ThreadDeadlockDetectorTest {
  /**
   * Create two threads that deadlock each other.
   */
  @Test
  fun testBasic() {
    Assertions.assertTimeout(Duration.ofSeconds(10)) {
      val lockA: Lock = ReentrantLock()
      val lockB: Lock = ReentrantLock()
      val barrier = CyclicBarrier(2)

      //Lock the locks in different order for both threads
      val thread1 = Thread(LockBothLocksRunnable(lockA, lockB, barrier), "dead-thread1")
      val thread2 = Thread(LockBothLocksRunnable(lockB, lockA, barrier), "dead-thread2")
      val deadlockDetector = ThreadDeadlockDetector(10)
      val out = ByteArrayOutputStream()
      deadlockDetector.addListener(DefaultDeadlockListener(PrintStream(out)))
      val deadLockDetected = AtomicBoolean(false)
      deadlockDetector.addListener { deadLockDetected.set(true) }
      deadlockDetector.start()
      thread1.start()
      thread2.start()

      //Wait until the deadlock has been detected
      Awaitility
        .waitAtMost(5, TimeUnit.SECONDS)
        .untilTrue(deadLockDetected)
      deadlockDetector.stop()

      //Check the output of the dumps
      assertThat(out.toString()).contains("Deadlocked Threads:")
      assertThat(out.toString()).contains("dead-thread1,5")
      assertThat(out.toString()).contains("dead-thread2,5")
    }
  }

  /**
   * A listener that throws must not kill the timer thread - all later checks would fail silently.
   */
  @Test
  fun testDetectionSurvivesFailingListener() {
    Assertions.assertTimeout(Duration.ofSeconds(10)) {
      val lockA: Lock = ReentrantLock()
      val lockB: Lock = ReentrantLock()
      val barrier = CyclicBarrier(2)

      val thread1 = Thread(LockBothLocksRunnable(lockA, lockB, barrier), "failing-listener-dead-thread1").apply { isDaemon = true }
      val thread2 = Thread(LockBothLocksRunnable(lockB, lockA, barrier), "failing-listener-dead-thread2").apply { isDaemon = true }

      val deadlockDetector = ThreadDeadlockDetector(10)
      val detectionCount = AtomicInteger()

      //The counting listener is registered first - the throwing one must not prevent later runs
      deadlockDetector.addListener { detectionCount.incrementAndGet() }
      deadlockDetector.addListener { throw IllegalStateException("Deadlocked Thread not found") }

      deadlockDetector.start()
      thread1.start()
      thread2.start()

      try {
        Awaitility
          .waitAtMost(5, TimeUnit.SECONDS)
          .until { detectionCount.get() >= 2 }
      } finally {
        deadlockDetector.stop()
      }

      assertThat(detectionCount.get()).isGreaterThanOrEqualTo(2)
    }
  }

  /**
   * A runnable that locks both locks.
   *
   *
   *
   *  * Lock [.lock1]
   *  * Wait for the barrier [.barrier]
   *  * Lock [.lock2]
   *
   */
  class LockBothLocksRunnable(
    val lock1: Lock,
    val lock2: Lock,
    val barrier: CyclicBarrier,
  ) : Runnable {
    override fun run() {
      lock1.lock()
      try {
        barrier.await()
      } catch (e: InterruptedException) {
        throw RuntimeException(e)
      } catch (e: BrokenBarrierException) {
        throw RuntimeException(e)
      }
      lock2.lock()
    }
  }
}
