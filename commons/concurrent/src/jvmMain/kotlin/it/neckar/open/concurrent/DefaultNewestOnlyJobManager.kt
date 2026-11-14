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

import it.neckar.open.annotations.NonUiThread
import it.neckar.open.annotations.UiThread
import it.neckar.open.kotlin.lang.fastFor
import java.util.concurrent.BlockingDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.logging.Level
import java.util.logging.Logger
import javax.annotation.PostConstruct
import javax.annotation.concurrent.ThreadSafe
import kotlin.math.max

/**
 * Default implementation for newest only jobs manager
 */
@ThreadSafe
public class DefaultNewestOnlyJobManager @JvmOverloads constructor(
  private val executorService: ExecutorService,
  private val workerCount: Int,
  /**
   * The max amount of jobs that are held
   */
  private val maxJobsCount: Int = 50
) : NewestOnlyJobsManager {

  /**
   * Contains the tile identifiers that shall be calculated
   */
  private val jobs: BlockingDeque<NewestOnlyJobsManager.Job>

  /**
   * Returns the next job from the queue
   */
  //Clean up all other jobs with the given key to avoid duplicate actions
  private val nextJob: NewestOnlyJobsManager.Job
    @Throws(InterruptedException::class)
    get() {
      val job = jobs.takeFirst()
      clearJobs(job.key)
      return job
    }

  init {
    this.jobs = LinkedBlockingDeque(maxJobsCount)

    require(workerCount >= 1) { "Need at least one worker but was <$workerCount>" }
  }

  /**
   * Starts the workers
   */
  @PostConstruct
  public fun startWorkers() {
    //Start the worker threads
    workerCount.fastFor {
      executorService.execute(Worker())
    }
  }

  /**
   * A new job is scheduled
   */
  @UiThread
  @NonUiThread
  override fun scheduleJob(job: NewestOnlyJobsManager.Job) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("schedule job for key <" + job.key + ">. Old size <" + jobs.size + ">")
    }
    //Remove old jobs with the same key
    clearJobs(job.key)

    //Schedule for calculation
    while (!jobs.offerFirst(job)) {
      //Cleanup if necessary
      ensureMaxJobsSize()
    }
  }

  override fun clearJobs(key: Any) {
    jobs.removeIf { job -> job.key == key }
  }

  /**
   * Deletes old jobs to ensure the max job count is respected
   */
  private fun ensureMaxJobsSize() {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Cleaning up old jobs <" + jobs.size + ">")
    }
    while (jobs.size >= maxJobsCount) {
      jobs.pollLast()
    }
  }

  /**
   * Worker that always gets the first job and solves it
   */
  private inner class Worker : Runnable {
    override fun run() {
      while (!Thread.currentThread().isInterrupted) {
        try {
          nextJob.execute()
        } catch (_: InterruptedException) {
          return
        } catch (e: Throwable) {
          Thread.currentThread().uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e)
        }
      }
    }
  }

  public companion object {
    private val LOG = Logger.getLogger(DefaultNewestOnlyJobManager::class.java.name)

    /**
     * Creates a new instance with a worker count depending on the available processors count.
     *
     *
     * Creates a manager with (processors-count - 1) workers.
     */
    public fun create(): DefaultNewestOnlyJobManager {
      //Calculate the amount of background painting threads
      val cores = Runtime.getRuntime().availableProcessors()
      val workerCount = max(1, cores - 1)

      return DefaultNewestOnlyJobManager(Executors.newFixedThreadPool(workerCount, NamedThreadFactory("GlobalOptionalJobManager")), workerCount)
    }
  }
}
