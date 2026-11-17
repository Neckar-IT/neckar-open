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

import it.neckar.open.annotations.Blocking
import it.neckar.open.annotations.NonUiThread
import javax.annotation.concurrent.ThreadSafe

/**
 * Job manager that only executes the newest job for a key.
 * Old jobs are automatically discarded.
 *
 */
@ThreadSafe
public interface NewestOnlyJobsManager {
  /**
   * Schedules a new job.
   */
  public fun scheduleJob(job: Job)

  /**
   * Clears all jobs for the given key
   */
  public fun clearJobs(key: Any)

  /**
   * Represents a job.
   * Each job has a key - these are compared using #equals().
   * Only the newest job for every key is executed.
   */
  public interface Job {
    /**
     * The key for the job. Only the youngest job for a given key is executed
     */
    public val key: Any

    /**
     * Executes the job
     */
    @NonUiThread
    @Blocking
    public fun execute()
  }
}
