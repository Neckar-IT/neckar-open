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
package it.neckar.open.concurrent.repeating

import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.logging.debug
import it.neckar.open.async.PlatformReadWriteLock
import it.neckar.open.async.read
import it.neckar.open.async.write
import it.neckar.open.dispose.Disposable
import it.neckar.open.time.now
import it.neckar.open.unit.other.GuardedBy
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant


/**
 * Thrown by [stopRepeating] to end a repeating job from inside its own block.
 *
 * A control exception, not a failure: [Repeating] ends the loop quietly, without invoking the error
 * handler and without recording a failed cycle.
 */
public class StopRepeating internal constructor() : RuntimeException("stop repeating")

/**
 * Ends the repeating job from inside its block — for a loop whose end condition only the work itself
 * knows: the process output reached EOF, the queue was closed, the retry budget is used up.
 *
 * ```
 * Repeating.immediately {
 *   val line = reader.readLine() ?: stopRepeating()
 *   logger.info(line)
 * }
 * ```
 *
 * Returns [Nothing], so it can stand on the right of an elvis operator.
 */
public fun stopRepeating(): Nothing = throw StopRepeating()

/**
 * Runs a job over and over, on the rhythm of its [CycleSchedule] — every few seconds
 * ([Repeating.every]) or at a wall-clock time (`Repeating.dailyAt`, JVM only).
 *
 * The one place background work is started, so every such job gets the same four properties instead
 * of re-implementing them in a hand-rolled `while (true) { … delay(…) }`:
 *
 * - **The loop survives its own failures.** A cycle that throws is logged and reported; the next one
 *   starts on schedule. Neither the error handler nor the cycle listener can end the loop.
 * - **A cycle cannot hang forever** when [RepeatingJobDescriptor.cycleTimeout] is set: it is cancelled
 *   and the next one starts on schedule.
 * - **A worker that died is revived** by the supervisor — as long as its scope is still alive.
 * - **Every cycle is observable** through [RepeatingJobDescriptor.cycleListener]; `it.neckar.open.health`
 *   connects it to the `BackgroundJobRegistry` with a single `trackedAs(…)` call.
 *
 * - **Repeated failures thin out** when an [RepeatingJobDescriptor.errorBackoff] is set, instead of
 *   retrying at full rate for as long as the outage lasts.
 * - **What the job did is readable** from [statistics] and [state], without registering an observer.
 *
 * The timeout, the listener and the backoff are opt-in; a job that only wants the supervisor and the
 * error handling sets none of them. A loop that ends on a condition only its block knows calls
 * [stopRepeating].
 *
 * Implements [Disposable] so callers can use the standard lifecycle pattern. [dispose] delegates to
 * [cancel]; a cancelled job can be [start]ed again.
 */
public class Repeating(
  /**
   * The job that is repeated
   */
  public val jobDescriptor: RepeatingJobDescriptor,
) : Disposable {

  /**
   * The lock for the job
   */
  private val lock = PlatformReadWriteLock()

  /**
   * The current job - which might be null or canceled
   */
  @GuardedBy("lock")
  internal var currentJob: Job? = null

  @GuardedBy("lock")
  internal var supervisorJob: Job? = null

  /**
   * Belongs to the supervisor launched by the latest [start]. Held per launch so a [cancel] followed
   * by a new [start] cannot make the old supervisor report the new launch's state as its end reason.
   */
  @GuardedBy("lock")
  private var supervisorRun: SupervisorRun? = null

  /**
   * Set once the block ended the loop through [stopRepeating]. Keeps the supervisor from reviving a
   * job that declared itself finished — it exists for a worker that died unexpectedly, which is the
   * opposite case.
   */
  @GuardedBy("lock")
  private var stoppedItself: Boolean = false

  /**
   * Carries the out-of-band triggers of [triggerNow] to the waiting worker.
   *
   * Conflated on purpose: while a cycle runs, further triggers collapse into one follow-up cycle.
   * A second cycle per trigger would buy nothing — the follow-up starts after the current one and
   * therefore already sees whatever the later triggers were about.
   */
  private val triggers = Channel<Unit>(Channel.CONFLATED)

  @GuardedBy("lock")
  private var currentStatistics: RepeatingStatistics = RepeatingStatistics()

  /**
   * What this job has done so far — a consistent snapshot, safe to read from any thread.
   */
  public val statistics: RepeatingStatistics
    get() = lock.read { currentStatistics }

  /**
   * Where this job stands: [RepeatingState.Idle] before the first [start] and after [cancel],
   * [RepeatingState.Running] while it repeats, [RepeatingState.StoppedItself] once its block called
   * [stopRepeating].
   */
  public val state: RepeatingState
    get() = lock.read {
      when {
        stoppedItself -> RepeatingState.StoppedItself
        supervisorJob?.isActive == true -> RepeatingState.Running
        else -> RepeatingState.Idle
      }
    }

  /**
   * Starts the supervisor job that starts the worker job.
   *
   * Returns a [Disposable] that can be used to stop the repeating job. Ignorable: a job that lives as
   * long as the scope it was started in needs no handle of its own.
   */
  @IgnorableReturnValue
  public fun start(
    /**
     * The coroutineScope scope in which the job is executed
     */
    coroutineScope: CoroutineScope,

    /**
     * The scope in which the supervisor job is executed.
     *
     * Defaults to [coroutineScope]: the supervisor exists to revive the worker, so outliving it
     * serves no purpose — a supervisor in a scope of its own would keep running (and keep restarting
     * an already-dead worker) after the caller shut its scope down, and would never be collected.
     */
    supervisorScope: CoroutineScope = coroutineScope,

    /**
     * The delay between each execution of the supervisor job
     */
    supervisorJobDelay: Duration = 10.seconds,
  ): Disposable {
    logger.debug { "Starting supervisor job for [${jobDescriptor.schedule.description}]" }

    lock.write {
      if (supervisorJob?.isActive == true) {
        //Idempotent on purpose: a library must not punish a caller for a second start - two callers
        //racing to start the same job is a normal thing to happen, and throwing would make the loser
        //fail for something that is already true.
        logger.debug { "Repeating job [${jobDescriptor.schedule.description}] is already started" }
        return this
      }
      stoppedItself = false
      //A trigger describes a moment, not an intention for the future. One that arrived while the job
      //was stopped would otherwise fire the first cycle right away - on a daily schedule that means
      //03:00 becomes "whenever the service restarted".
      discardPendingTriggers()

      //Outside notifyCycleListener on purpose: a listener that rejects the configuration fails this start
      jobDescriptor.cycleListener?.onJobStarted(jobDescriptor)

      val run = SupervisorRun()
      supervisorRun = run
      //Atomic: a supervisor launched into a cancelled scope still runs its finally and reports the end
      supervisorJob = supervisorScope.launch(start = CoroutineStart.ATOMIC) {
        var endReason: RepeatingEndReason? = null
        try {
          while (isActive) {
            //Reviving the worker is only possible while its scope lives. In a cancelled scope every
            //restart would produce an already-cancelled job, and the supervisor would thrash against
            //it for the lifetime of the process.
            if (coroutineScope.isActive.not()) {
              logger.info("Job scope is gone - stopping the supervisor of [${jobDescriptor.schedule.description}]")
              break
            }

            val finished = lock.write {
              val currentJob = currentJob

              if (stoppedItself) {
                logger.debug { "Repeating job [${jobDescriptor.schedule.description}] stopped itself - nothing left to supervise" }
                return@write true
              }

              if (currentJob == null) {
                startJob(coroutineScope)
              } else if (currentJob.isActive.not()) {
                logger.warn("Repeating job [${jobDescriptor.schedule.description}] is no longer active - restarting it")
                startJob(coroutineScope)
              } else {
                logger.debug { "Job is already running: [$currentJob]" }
              }

              false
            }

            if (finished) {
              endReason = RepeatingEndReason.StoppedItself
              break
            }

            delay(supervisorJobDelay)
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Throwable) {
          //The catch sits outside the loop, so the supervisor is over. Say so: without it the reader
          //sees an error and assumes supervision continued, and a worker that dies later is never
          //revived without a second log line to explain why.
          logger.error("Supervisor of the repeating job [${jobDescriptor.schedule.description}] failed - the job is no longer supervised and will not be restarted", e)
          endReason = RepeatingEndReason.SupervisorFailed(e)
        } finally {
          val reason = endReason ?: if (run.cancelledManually) RepeatingEndReason.Cancelled else RepeatingEndReason.ScopeCancelled
          notifyCycleListener { onJobEnded(reason) }
        }
      }
    }

    return this
  }

  /**
   * Starts the worker job itself
   */
  internal fun startJob(coroutineScope: CoroutineScope) {
    logger.debug { "Starting repeating job [${jobDescriptor.schedule.description}]" }

    lock.write {
      currentJob = coroutineScope.launch {
        //The moment the last cycle stood for; null before the first cycle of this worker
        var cycleAt: Instant? = null
        var nextCycleStart = NextCycleStart.Scheduled

        while (isActive) {
          cycleAt = when (nextCycleStart) {
            NextCycleStart.Scheduled -> awaitNextCycle(cycleAt)
            NextCycleStart.ScheduledAfterBackoff -> awaitNextCycle(now())
            NextCycleStart.TriggeredDuringBackoff -> now()
          }
          nextCycleStart = NextCycleStart.Scheduled

          logger.debug { "Executing repeating job [${jobDescriptor.schedule.description}]" }
          recordCycleStarted()
          notifyCycleListener { onCycleStarted() }

          try {
            runWithCycleTimeout(jobDescriptor.cycleTimeout) {
              jobDescriptor.block()
            }
            recordCycleCompleted()
            notifyCycleListener { onCycleCompleted() }
          } catch (e: StopRepeating) {
            //The block declared itself finished. A completed cycle, not a failure - and the supervisor
            //is told, so it does not revive a job that ended on purpose.
            logger.debug { "Repeating job [${jobDescriptor.schedule.description}] stopped itself" }
            recordCycleCompleted()
            lock.write { stoppedItself = true }
            notifyCycleListener { onCycleCompleted() }
            break
          } catch (e: TimeoutCancellationException) {
            //A timeout fired. Log loudly so the cycle hang shows up in Loki/Grafana, notify onError,
            //and continue with the outer loop.
            //
            //Which timeout it was cannot be read off the exception: a `withTimeout` inside the block
            //throws the very same type. Only the configured cycleTimeout tells the two apart, so
            //name it when it is set - and when it is not, say that instead of blaming a limit that
            //was never configured.
            val cycleTimeout = jobDescriptor.cycleTimeout
            if (cycleTimeout != null) {
              logger.error("Repeating job [${jobDescriptor.schedule.description}] exceeded its cycleTimeout of $cycleTimeout", e)
            } else {
              logger.error("Repeating job [${jobDescriptor.schedule.description}] timed out inside its own block - it has no cycleTimeout configured", e)
            }
            nextCycleStart = handleFailedCycle(e)
          } catch (e: CancellationException) {
            notifyCycleListener { onCycleCancelled() }
            throw e
          } catch (e: Throwable) {
            logger.error("Repeating job [${jobDescriptor.schedule.description}] failed a cycle - the loop continues", e)
            nextCycleStart = handleFailedCycle(e)
          }
        }

        logger.debug { "Repeating job [${jobDescriptor.schedule.description}] was cancelled" }
      }
    }
  }

  /**
   * Waits until the next cycle is due — or until [triggerNow] cuts the wait short — and returns the
   * moment that cycle stands for.
   */
  private suspend fun awaitNextCycle(after: Instant?): Instant {
    //A trigger from the previous cycle comes first: a schedule that returns at once would otherwise
    //race it in the select and leave it for a second cycle
    if (triggers.tryReceive().isSuccess) {
      logger.debug { "Repeating job [${jobDescriptor.schedule.description}] was triggered during the previous cycle - running a cycle now" }
      return now()
    }

    return coroutineScope {
      val scheduled = async { jobDescriptor.schedule.awaitCycle(after) }

      select {
        scheduled.onAwait { it }
        triggers.onReceive {
          logger.debug { "Repeating job [${jobDescriptor.schedule.description}] was triggered - running a cycle now" }
          scheduled.cancel()
          now()
        }
      }
    }
  }

  /**
   * Runs a cycle now instead of waiting for the schedule — for work that is normally periodic but
   * sometimes has a reason to happen right away: a webhook arrived, an operator pressed a button, a
   * dependency came back.
   *
   * Never blocks and never runs the block on the caller's coroutine: the cycle happens on the job's
   * own scope, with the same timeout, error handling and reporting as a scheduled one.
   *
   * Triggers coalesce. While a cycle is running, any number of triggers produce exactly one follow-up
   * cycle — the follow-up starts afterwards and therefore already covers what the later triggers were
   * about.
   *
   * Cuts an [RepeatingJobDescriptor.errorBackoff] short: "the dependency is back, run now" is the very
   * reason to trigger a job whose cycles are failing.
   *
   * Has no effect on a job that is not running — a trigger describes a moment, and [start] discards
   * the ones that arrived while the job was stopped.
   */
  public fun triggerNow() {
    triggers.trySend(Unit)
  }

  private fun recordCycleStarted() {
    lock.write {
      currentStatistics = currentStatistics.copy(startedCycles = currentStatistics.startedCycles + 1)
    }
  }

  private fun recordCycleCompleted() {
    lock.write {
      currentStatistics = currentStatistics.copy(
        completedCycles = currentStatistics.completedCycles + 1,
        consecutiveFailures = 0,
        lastCycleCompletedAt = now(),
      )
    }
  }

  /**
   * Records the failure, tells the observers, and — when an [RepeatingJobDescriptor.errorBackoff] is
   * configured — waits before letting the loop continue.
   *
   * Returns how the next cycle starts; a [triggerNow] during the backoff starts it at once, because
   * "the dependency is back" is the very reason to trigger a failing job.
   */
  private suspend fun handleFailedCycle(error: Throwable): NextCycleStart {
    val consecutiveFailures = lock.write {
      currentStatistics = currentStatistics.copy(
        failedCycles = currentStatistics.failedCycles + 1,
        consecutiveFailures = currentStatistics.consecutiveFailures + 1,
        lastError = error,
      )
      currentStatistics.consecutiveFailures
    }

    notifyCycleListener { onCycleFailed(error) }
    notifyErrorHandler(error)

    val backoff = jobDescriptor.errorBackoff ?: return NextCycleStart.Scheduled

    val waitFor = backoff.delayAfter(consecutiveFailures)
    logger.warn("Repeating job [${jobDescriptor.schedule.description}] failed $consecutiveFailures time(s) in a row - backing off for $waitFor")

    val triggered = withTimeoutOrNull(waitFor) { triggers.receive() } != null
    if (triggered.not()) {
      return NextCycleStart.ScheduledAfterBackoff
    }
    logger.debug { "Repeating job [${jobDescriptor.schedule.description}] was triggered during its backoff" }
    return NextCycleStart.TriggeredDuringBackoff
  }

  /**
   * How the worker loop starts the cycle after the current one.
   */
  private enum class NextCycleStart {
    /**
     * The next cycle follows the moment the previous cycle stood for.
     */
    Scheduled,

    /**
     * The next cycle follows the end of the error backoff, so the backoff adds to the schedule.
     */
    ScheduledAfterBackoff,

    /**
     * A trigger cut the error backoff short; the next cycle starts at once.
     */
    TriggeredDuringBackoff,
  }

  /**
   * Empties the trigger channel. See [start].
   */
  private fun discardPendingTriggers() {
    while (triggers.tryReceive().isSuccess) {
      //Drain
    }
  }

  /**
   * Invokes [notification] on the [RepeatingJobDescriptor.cycleListener], if one is set.
   *
   * A listener that throws is logged, not propagated: it observes the job (metrics, a liveness
   * registry) and must never take down the job it observes.
   */
  private inline fun notifyCycleListener(notification: RepeatingCycleListener.() -> Unit) {
    val cycleListener = jobDescriptor.cycleListener ?: return
    try {
      cycleListener.notification()
    } catch (e: Throwable) {
      //Worth a loud line even though the job carries on: the listener is what makes the job visible
      //in /health, so a listener that keeps throwing turns a working job into one that looks dead.
      logger.error("Cycle listener [$cycleListener] of the repeating job [${jobDescriptor.schedule.description}] failed - this cycle was not reported to it", e)
    }
  }

  /**
   * Invokes [RepeatingJobDescriptor.onError] with [error], if a handler is set.
   *
   * A handler that throws is logged, not propagated — for the same reason as the cycle listener, and
   * because it runs while the job is already handling a failure: a broken handler must not turn one
   * failed cycle into a job that stops running.
   */
  private fun notifyErrorHandler(error: Throwable) {
    val onError = jobDescriptor.onError ?: return
    try {
      onError(error)
    } catch (e: Throwable) {
      //Name the error that went unreported: the handler is usually the alerting path, so its failure
      //means nobody was told about [error] - and the log line is then the only trace left of it.
      logger.error("Error handler of the repeating job [${jobDescriptor.schedule.description}] failed - the cycle failure [$error] was not reported to it", e)
    }
  }

  /**
   * If [cycleTimeout] is set, runs [block] inside [withTimeout]; otherwise runs it directly.
   */
  private suspend fun runWithCycleTimeout(cycleTimeout: Duration?, block: suspend () -> Unit) {
    if (cycleTimeout == null) {
      block()
    } else {
      withTimeout(cycleTimeout) { block() }
    }
  }

  /**
   * Stops the supervisor and the worker. Idempotent, and [start] may be called again afterwards.
   */
  public fun cancel() {
    logger.debug { "Cancelling repeating job [${jobDescriptor.schedule.description}]" }
    lock.write {
      supervisorRun?.cancelledManually = true
      supervisorRun = null
      supervisorJob?.cancel(CancellationException("Canceled manually"))
      currentJob?.cancel(CancellationException("Canceled manually"))
      supervisorJob = null
      currentJob = null
      stoppedItself = false
      //The statistics survive: they describe what this job did, not whether it is running, and a
      //caller inspecting a cancelled job wants to know how it went.
    }
  }

  override fun dispose() {
    cancel()
  }

  override fun toString(): String = "Repeating(${jobDescriptor.schedule.description}, $state, ${statistics})"

  /**
   * The state of one supervisor launch that outlives the fields [start] resets.
   */
  private class SupervisorRun {
    /**
     * Set by [cancel] before it cancels the supervisor, read by the supervisor when it ends.
     */
    @Volatile
    var cancelledManually: Boolean = false
  }

  public companion object {
    /**
     * Repeats [repeatedBlock] with [delay] between the end of one cycle and the start of the next.
     * The first cycle runs immediately. See [IntervalSchedule].
     */
    public fun every(
      /**
       * The delay between each execution
       */
      delay: Duration,
      /**
       * The block itself that is executed repeatedly
       */
      repeatedBlock: suspend () -> Unit,
    ): RepeatingJobDescriptor.Builder {
      return following(IntervalSchedule(delay), repeatedBlock)
    }

    /**
     * Repeats [repeatedBlock] with no pause between cycles — the block itself paces the loop by
     * blocking or suspending, and ends it with [stopRepeating]. See [ImmediateSchedule].
     */
    public fun immediately(
      /**
       * The block itself that is executed repeatedly
       */
      repeatedBlock: suspend () -> Unit,
    ): RepeatingJobDescriptor.Builder {
      return following(ImmediateSchedule, repeatedBlock)
    }

    /**
     * Repeats [repeatedBlock] on [schedule] — the general form of [every], used by the wall-clock
     * schedules (`Repeating.hourlyAt`, `Repeating.dailyAt`, `Repeating.weeklyAt`).
     */
    public fun following(
      /**
       * Decides when each cycle runs
       */
      schedule: CycleSchedule,
      /**
       * The block itself that is executed repeatedly
       */
      repeatedBlock: suspend () -> Unit,
    ): RepeatingJobDescriptor.Builder {
      return RepeatingJobDescriptor.Builder(schedule, repeatedBlock)
    }

    private val logger: Logger = LoggerFactory.getLogger("it.neckar.open.concurrent.Repeating")
  }

  /**
   * A job that is called repeatedly
   */
  public data class RepeatingJobDescriptor(
    /**
     * The block itself that is executed repeatedly
     */
    val block: suspend () -> Unit,

    /**
     * Decides when each cycle runs
     */
    val schedule: CycleSchedule,

    /**
     * Callback that is called when an error occurs.
     * Is *not* called when the job is canceled. A handler that throws is logged and swallowed.
     */
    val onError: ((error: Throwable) -> Unit)? = null,

    /**
     * Upper bound for one execution of [block]. When set, each cycle is wrapped in
     * [withTimeout]; a cycle that exceeds the timeout is cancelled cooperatively and
     * [onError] is invoked with the [TimeoutCancellationException]. The next cycle
     * starts on schedule as normal.
     *
     * `null` (default) keeps the historical behaviour: no per-cycle timeout, cycles
     * may run forever. Opt-in for new callers that need self-healing.
     */
    val cycleTimeout: Duration? = null,

    /**
     * Observes every cycle of this job (started, completed, failed). `null` (default) reports nothing.
     *
     * The hook that makes an essential background job observable: `it.neckar.open.health` connects it
     * to the `BackgroundJobRegistry`, so a job that died or wedged shows up in `/health` and in the
     * liveness metric instead of only in the logs.
     */
    val cycleListener: RepeatingCycleListener? = null,

    /**
     * How long to wait after a failed cycle, on top of the schedule. `null` (default) retries at the
     * normal rhythm — right for a job whose cycles are independent attempts.
     *
     * See [ErrorBackoff]: a job whose dependency is down otherwise retries at full rate for as long
     * as the outage lasts.
     */
    val errorBackoff: ErrorBackoff? = null,
  ) {
    /**
     * The longest this job's loop pauses between two cycles: the schedule's longest pause plus the
     * longest [errorBackoff] after a failed cycle.
     */
    public val maxGap: Duration
      get() = schedule.maxGap + (errorBackoff?.maximumDelay ?: Duration.ZERO)

    /**
     * Builder for [RepeatingJobDescriptor]
     */
    public class Builder(
      /**
       * Decides when each cycle runs
       */
      public var schedule: CycleSchedule,

      /**
       * The block itself that is executed repeatedly
       */
      public val block: suspend () -> Unit,
    ) {

      public fun onError(onError: (error: Throwable) -> Unit): Builder {
        this.onError = onError
        return this
      }

      /**
       * Sets the per-cycle timeout. See [RepeatingJobDescriptor.cycleTimeout].
       */
      public fun cycleTimeout(cycleTimeout: Duration): Builder {
        this.cycleTimeout = cycleTimeout
        return this
      }

      /**
       * Sets the cycle listener. See [RepeatingJobDescriptor.cycleListener].
       */
      public fun cycleListener(cycleListener: RepeatingCycleListener): Builder {
        this.cycleListener = cycleListener
        return this
      }

      /**
       * Sets the backoff applied after a failed cycle. See [RepeatingJobDescriptor.errorBackoff].
       */
      public fun errorBackoff(errorBackoff: ErrorBackoff): Builder {
        this.errorBackoff = errorBackoff
        return this
      }

      public fun build(): RepeatingJobDescriptor {
        return RepeatingJobDescriptor(block, schedule, onError, cycleTimeout, cycleListener, errorBackoff)
      }

      /**
       * Starts the repeating job. Ignorable: a job that lives as long as the scope it was started in
       * needs no handle of its own.
       */
      @IgnorableReturnValue
      public fun start(
        /**
         * The coroutineScope scope in which the job is executed
         */
        coroutineScope: CoroutineScope,

        /**
         * The scope in which the supervisor job is executed. Defaults to [coroutineScope] — see
         * [Repeating.start].
         */
        supervisorScope: CoroutineScope = coroutineScope,

        /**
         * The delay between each execution of the supervisor job
         */
        supervisorJobDelay: Duration = 10.seconds,
      ): Repeating {
        return Repeating(build()).also {
          it.start(coroutineScope = coroutineScope, supervisorScope = supervisorScope, supervisorJobDelay = supervisorJobDelay)
        }
      }

      /**
       * Callback that is called when an error occurs
       */
      public var onError: ((error: Throwable) -> Unit)? = null

      /**
       * Per-cycle timeout. See [RepeatingJobDescriptor.cycleTimeout].
       */
      public var cycleTimeout: Duration? = null

      /**
       * Cycle listener. See [RepeatingJobDescriptor.cycleListener].
       */
      public var cycleListener: RepeatingCycleListener? = null

      /**
       * Backoff after a failed cycle. See [RepeatingJobDescriptor.errorBackoff].
       */
      public var errorBackoff: ErrorBackoff? = null
    }
  }
}
