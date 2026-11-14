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
package it.neckar.ktor.jwt.jwks

import com.auth0.jwk.Jwk
import com.auth0.jwt.interfaces.DecodedJWT
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.logging.debug
import it.neckar.open.concurrent.repeating.Repeating
import it.neckar.open.concurrent.repeating.RepeatingState
import it.neckar.open.health.BackgroundJobName
import it.neckar.open.health.trackedAs
import it.neckar.open.unit.other.GuardedBy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Owns the JWKS key set of one realm: fetches it, holds it, persists it, and answers the
 * authentication path from what is held.
 *
 * The three things this separates were one before:
 * - **fetching** happens here and nowhere else, on the job's own interval and on an out-of-turn
 *   request the [JwksRefreshPlanner] allowed. No incoming request ever waits for a fetch.
 * - **holding** is the [JwksStore]: no expiry, replaced only by the whole result of a successful
 *   fetch.
 * - **having confirmed** is the origin of the held set, which [probe] reports: a set that came from
 *   the seed serves, and says so.
 *
 * A failed fetch changes nothing that is held. It is a `WARN` and a failed cycle of the background
 * job — not a health state, because a service that holds a valid key set keeps validating tokens
 * while the realm is unreachable.
 */
class JwksKeyManager(
  /**
   * The realm this manager holds the keys of, e.g. `internal`. Names the seed resource, the health job and
   * the log lines; two managers of one service need two names.
   */
  val realmName: String,
  private val source: JwksSource,
  private val prefilter: JwksRefreshPrefilter,
  private val store: JwksStore = JwksStore(),
  private val persistence: JwksPersistence = JwksPersistence.None,
  /**
   * The JWKS file built into the image, or `null` for a service that ships none.
   */
  private val seed: JwksSeed? = ClasspathJwksSeed(realmName),
  private val planner: JwksRefreshPlanner = JwksRefreshPlanner(),
  /**
   * How often the regular cycle fetches. The upper bound on how long a rotation stays invisible —
   * the planner's window is the lower one.
   */
  val refreshInterval: Duration = DefaultRefreshInterval,
) {
  /**
   * Conflated: while a fetch runs, further requests collapse into the single one that follows it.
   * Never blocks a request thread, so [requestRefreshFor] can be called from the authentication path.
   */
  private val refreshRequests = Channel<Unit>(Channel.CONFLATED)

  /**
   * The pair that runs, or `null` before the first [start] and after the last registered scope ended.
   */
  @GuardedBy("startLock")
  private var running: RunningJobs? = null

  /**
   * The job of every scope [start] was called with that has not completed yet.
   *
   * `ServiceAuthentication.Keycloak` holds one manager for the lifetime of the process, and every
   * Application built from that configuration installs authentication again. The pair runs while one
   * of these scopes is active; [onScopeEnded] cancels it once none is.
   */
  @GuardedBy("startLock")
  private val registeredScopes: MutableSet<Job> = HashSet()

  /**
   * Guards [running] and [registeredScopes]. An atomic reference would make a read and a write
   * visible but not indivisible, and two callers could still both find nothing running and both
   * launch a pair.
   */
  private val startLock = Any()

  /**
   * The regular cycle and the consumer of the out-of-turn requests, launched together in [scope].
   *
   * [scope] belongs to this manager, so an Application that ends leaves the pair running for the
   * Applications that remain. In the scope of an Application the pair would end with it, and the
   * background job registry would report the essential fetch job as ended.
   */
  private class RunningJobs(val scope: CoroutineScope, val regularCycle: Repeating, val outOfTurnConsumer: Job) {
    /**
     * Both halves alive. One half alone is not a running job: a lone consumer would receive requests
     * no cycle answers, a lone cycle would leave the out-of-turn path dead.
     */
    val isAlive: Boolean
      get() = regularCycle.state == RepeatingState.Running && outOfTurnConsumer.isActive

    /**
     * Ends the pair. Disposing the cycle first records its end as cancelled by its owner, which the
     * background job registry does not count as a stall.
     */
    fun cancel() {
      regularCycle.dispose()
      scope.cancel()
    }
  }

  /**
   * The key set this service holds, with its origin, or `null` while it holds none.
   */
  val held: HeldJwks?
    get() = store.held

  /**
   * The name the fetch job is registered under, per realm so a service validating against two realms
   * reports two jobs.
   */
  val backgroundJobName: BackgroundJobName = BackgroundJobName.of("jwks-fetch", realmName)

  /**
   * The probe that makes the key set a dependency: without a held set the service is not ready, with
   * a seed-only set it is degraded.
   */
  val probe: JwksDependencyProbe = JwksDependencyProbe(store)

  /**
   * Reads the persisted set of the previous process, and the seed when there is none.
   *
   * Called once before the service starts serving, so the first request finds a set even when the
   * realm is unreachable. The seed is tried last and only when nothing else was adopted.
   */
  fun loadHeldSet() {
    readPersisted()?.let { persisted ->
      if (store.adoptPersisted(persisted.document, persisted.fetchedAt)) {
        logger.info("Realm '$realmName': adopted the persisted key set of ${persisted.fetchedAt} (${persisted.document.keyIds.joinToString()})")
        return
      }
    }

    readSeed()?.let { document ->
      if (store.adoptSeed(document)) {
        logger.warn("Realm '$realmName': serving from the seed built into the image — no fetch has confirmed these keys (${document.keyIds.joinToString()})")
      }
    }
  }

  /**
   * Tries one fetch before the service starts serving, bounded by [timeout].
   *
   * Without it a service would report itself not-ready for the first moments of every start, or
   * serve from the seed until the background job's first cycle landed. A failure is a `WARN`: the
   * service then serves from whatever [loadHeldSet] adopted, and the background job keeps trying.
   */
  fun fetchOnceBeforeServing(timeout: Duration = InitialFetchTimeout) {
    try {
      runBlocking {
        withTimeoutOrNull(timeout) { fetchAndAdopt() }
          ?: logger.warn("Realm '$realmName': the first JWKS fetch did not answer within $timeout, serving from ${describeHeldSet()}")
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      logger.warn("Realm '$realmName': the first JWKS fetch failed, serving from ${describeHeldSet()}", e)
    }
  }

  /**
   * Registers [scope] and starts the regular fetch cycle and the consumer of the out-of-turn requests,
   * unless they already run. Returns whether it started them.
   *
   * The pair runs in a scope of this manager, on the dispatcher of [scope], until every registered
   * scope has ended. [scope] has to carry a [Job], as the scope of an Application does; a scope that
   * is no longer active starts nothing.
   *
   * The two are separate coroutines on purpose: the regular cycle keeps its interval whatever the
   * out-of-turn path does, so a flood of unknown `kid`s can only pull a refresh forward.
   */
  @IgnorableReturnValue
  fun start(scope: CoroutineScope): Boolean = synchronized(startLock) {
    val scopeJob = scope.coroutineContext.job
    if (scopeJob.isActive.not()) {
      logger.debug { "Realm '$realmName': the scope is no longer active, not starting the JWKS fetch job in it" }
      return@synchronized false
    }
    if (registeredScopes.add(scopeJob)) {
      scopeJob.invokeOnCompletion { onScopeEnded(scopeJob) }
    }

    val previous = running
    if (previous?.isAlive == true) {
      // Two consumers of a conflated channel would split the refresh requests between them.
      logger.debug { "Realm '$realmName': the JWKS fetch job is already running, not starting a second one" }
      return@synchronized false
    }
    //Half of a pair can still be alive here; it would survive beside the pair below.
    previous?.cancel()

    val pairScope = CoroutineScope(scope.coroutineContext.minusKey(Job) + SupervisorJob())

    // An IntervalSchedule runs its first cycle at once, so this fetches again right after
    // fetchOnceBeforeServing: two requests for one static document per restart, and worth it.
    val regularCycle = Repeating.every(refreshInterval) { runRegularCycle() }
      .cycleTimeout(refreshInterval)
      .onError { logger.warn("Realm '$realmName': regular JWKS fetch failed, keeping ${describeHeldSet()}", it) }
      .trackedAs(backgroundJobName, stalenessThreshold = refreshInterval * StalenessThresholdFactor)
      .start(coroutineScope = pairScope)

    val outOfTurnConsumer = pairScope.launch {
      //receiveAsFlow, not consumeEach: a consuming operator closes the channel when this collector
      //ends, and the channel has to outlive the pair for the next start to have anything to read.
      refreshRequests.receiveAsFlow().collect { runOutOfTurnCycle() }
    }

    running = RunningJobs(pairScope, regularCycle, outOfTurnConsumer)
    true
  }

  /**
   * Forgets [scopeJob] and cancels the pair once no registered scope is active any more. A scope that
   * is cancelling counts as ended: its own completion follows and finds nothing left to do.
   */
  private fun onScopeEnded(scopeJob: Job): Unit = synchronized(startLock) {
    registeredScopes.remove(scopeJob)
    if (registeredScopes.any { it.isActive }) {
      return@synchronized
    }
    running?.let {
      logger.info("Realm '$realmName': the last scope that started the JWKS fetch job ended, stopping it")
      it.cancel()
    }
    running = null
  }

  /**
   * Whether the fetch jobs of this manager are running. For tests and diagnosis.
   */
  val isRunning: Boolean
    get() = synchronized(startLock) { running?.isAlive == true }

  /**
   * The key [keyId] out of the held set, or `null` when the set does not contain it — or when no set
   * is held. Reads memory only; this is the whole of what the authentication path does.
   */
  fun keyOf(keyId: JwkKeyId): Jwk? = store.keyOf(keyId)

  /**
   * Asks for an out-of-turn refresh because a token carried the unknown [keyId], if the [prefilter]
   * finds the token plausible and the [planner] allows another fetch now. Returns whether one was
   * requested — the caller rejects the request either way.
   *
   * @param token the *unverified* token the `kid` came from. Read for the prefilter only; it grants
   * nothing.
   */
  fun requestRefreshFor(keyId: JwkKeyId, token: DecodedJWT): Boolean {
    if (prefilter.worthARefresh(token).not()) {
      return false
    }
    if (planner.planRefreshFor(keyId).not()) {
      return false
    }

    refreshRequests.trySend(Unit)
    return true
  }

  /**
   * One cycle of the regular fetch job. Internal so a test can drive it without a scheduler.
   */
  internal suspend fun runRegularCycle() {
    fetchAndAdopt()
    //The renewed set may hold what the memory recorded as absent, so the regular cycle voids it.
    planner.forgetAbsentKeyIds()
  }

  /**
   * One cycle of the out-of-turn path. Internal for the same reason as [runRegularCycle].
   */
  internal suspend fun runOutOfTurnCycle() {
    try {
      fetchAndAdopt()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      logger.warn("Realm '$realmName': out-of-turn JWKS fetch failed, keeping ${describeHeldSet()}", e)
    }
  }

  /**
   * Fetches the whole document and replaces the held set with it. Throws on failure — nothing that is
   * held changes then, which is what lets a service outlive an unreachable realm.
   */
  private suspend fun fetchAndAdopt() {
    val document = source.fetch()
    store.adoptFetched(document)
    planner.onFetchSucceeded(document.keyIds)

    try {
      persistence.write(document)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      logger.warn("Realm '$realmName': could not persist the fetched key set to $persistence", e)
    }
  }

  private fun readPersisted(): PersistedJwks? = try {
    persistence.read()
  } catch (e: Throwable) {
    logger.warn("Realm '$realmName': could not read the persisted key set from $persistence", e)
    null
  }

  private fun readSeed(): JwksDocument? = try {
    seed?.read()
  } catch (e: Throwable) {
    logger.warn("Realm '$realmName': could not read the seed $seed", e)
    null
  }

  /**
   * The held set in the words a log line and a `SigningKeyNotFoundException` message carry — which
   * origin, which keys, since when.
   */
  fun describeHeldSet(): String = held?.let {
    "the ${it.origin.label} key set of ${it.obtainedAt} (${it.document.keyIds.joinToString()})"
  } ?: "no key set at all"

  companion object {
    /**
     * The upper bound on how long a rotated key stays unusable. Chosen with
     * [JwksRefreshPlanner.window], which is the lower bound.
     */
    val DefaultRefreshInterval: Duration = 15.minutes

    /**
     * How long the start-up fetch may take before the service starts serving from what it loaded.
     *
     * Longer than [HttpJwksSource.DefaultConnectTimeout] plus [HttpJwksSource.DefaultRequestTimeout]
     * on purpose: the JDK HTTP client blocks a thread that no coroutine cancellation reaches, so the
     * socket has to give up first — at an equal value the two would expire in either order.
     */
    val InitialFetchTimeout: Duration = 20.seconds

    /**
     * How many intervals the job may miss before it counts as stale. More than one, so a realm that
     * is briefly unreachable does not raise an alert.
     */
    const val StalenessThresholdFactor: Int = 3

    private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.jwt.jwks.JwksKeyManager")
  }
}
