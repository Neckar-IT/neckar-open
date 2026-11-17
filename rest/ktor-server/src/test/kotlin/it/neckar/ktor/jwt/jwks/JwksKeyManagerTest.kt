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

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import it.neckar.open.concurrent.repeating.RepeatingEndReason
import it.neckar.open.health.BackgroundJobName
import it.neckar.open.health.BackgroundJobRegistry
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.test.utils.awaitCondition
import it.neckar.open.time.VirtualNowProvider
import it.neckar.open.time.now
import it.neckar.open.time.nowProvider
import it.neckar.open.time.resetNowProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class JwksKeyManagerTest {
  private val realm = "https://auth.neckar.it/realms/internal"
  private val client = "patterns.neckar.it"

  private val seedKey = JwksTestKey.generate("seed-key")
  private val liveKey = JwksTestKey.generate("live-key")
  private val rotatedKey = JwksTestKey.generate("rotated-key")

  private lateinit var virtualNow: VirtualNowProvider

  @BeforeEach
  fun fixTime() {
    virtualNow = VirtualNowProvider(1_800_000_000_000.0)
    nowProvider = virtualNow
  }

  @AfterEach
  fun releaseTime() {
    resetNowProvider()
  }

  private fun advance(by: Duration) {
    virtualNow.virtualNow += by.inWholeMilliseconds.toDouble()
  }

  /**
   * A source under the test's control: which document it answers with, how often it was asked, and
   * whether the next fetch fails.
   */
  private class RecordingJwksSource(var document: JwksDocument) : JwksSource {
    private val fetches = AtomicInteger()

    var failing: Boolean = false

    /**
     * Atomic: the regular cycle can be inside [fetch] while the out-of-turn one enters it.
     */
    val fetchCount: Int
      get() = fetches.get()

    override suspend fun fetch(): JwksDocument {
      fetches.incrementAndGet()
      if (failing) {
        throw JwksFetchFailedException("realm unreachable")
      }
      return document
    }
  }

  private fun manager(
    source: JwksSource,
    persistence: JwksPersistence = JwksPersistence.None,
    seed: JwksSeed? = null,
    planner: JwksRefreshPlanner = JwksRefreshPlanner(window = 30.seconds, maxRefreshesPerMinute = 4),
  ): JwksKeyManager = JwksKeyManager(
    realmName = "internal",
    source = source,
    prefilter = JwksRefreshPrefilter(issuer = realm, audience = client),
    persistence = persistence,
    seed = seed,
    planner = planner,
  )

  @Test
  fun `without persistence and without seed the manager holds nothing`() {
    val manager = manager(RecordingJwksSource(jwksDocumentOf(liveKey)))

    manager.loadHeldSet()

    assertThat(manager.held).isNull()
  }

  @Test
  fun `the persisted set of the previous process wins over the seed`() {
    val persistence = InMemoryJwksPersistence(jwksDocumentOf(liveKey), now() - 2.hours)
    val manager = manager(RecordingJwksSource(jwksDocumentOf(liveKey)), persistence, seedOf(seedKey))

    manager.loadHeldSet()

    assertThat(manager.held?.origin).isEqualTo(JwksOrigin.Persisted)
    assertThat(manager.keyOf(seedKey.keyId)).isNull()
  }

  @Test
  suspend fun `a successful fetch replaces the seed, and the seed is gone for good`() {
    val source = RecordingJwksSource(jwksDocumentOf(liveKey))
    val manager = manager(source, seed = seedOf(seedKey))
    manager.loadHeldSet()
    assertThat(manager.held?.origin).isEqualTo(JwksOrigin.Seed)

    manager.runRegularCycle()

    assertThat(manager.held?.origin).isEqualTo(JwksOrigin.Live)
    assertThat(manager.keyOf(seedKey.keyId)).isNull()
    assertThat(manager.keyOf(liveKey.keyId)).isNotNull()
  }

  @Test
  suspend fun `a failing fetch leaves the held set untouched`() {
    val source = RecordingJwksSource(jwksDocumentOf(liveKey))
    val manager = manager(source, seed = seedOf(seedKey))
    manager.loadHeldSet()
    manager.runRegularCycle()

    source.failing = true
    manager.runOutOfTurnCycle()

    assertThat(manager.held?.origin).isEqualTo(JwksOrigin.Live)
    assertThat(manager.keyOf(liveKey.keyId)).isNotNull()
  }

  @Test
  suspend fun `a successful fetch is persisted, so the next process starts from it`() {
    val persistence = InMemoryJwksPersistence()
    val manager = manager(RecordingJwksSource(jwksDocumentOf(liveKey)), persistence)
    manager.loadHeldSet()
    manager.runRegularCycle()

    val afterRestart = manager(RecordingJwksSource(jwksDocumentOf(liveKey)), persistence, seedOf(seedKey))
    afterRestart.loadHeldSet()

    assertThat(afterRestart.held?.origin).isEqualTo(JwksOrigin.Persisted)
    assertThat(afterRestart.keyOf(liveKey.keyId)).isNotNull()
  }

  @Test
  fun `an unknown kid with our claims requests a refresh, an unknown kid of a stranger does not`() {
    val manager = manager(RecordingJwksSource(jwksDocumentOf(liveKey)))

    assertThat(manager.requestRefreshFor(rotatedKey.keyId, tokenOf(rotatedKey))).isTrue()

    advance(1.minutes)
    assertThat(manager.requestRefreshFor(rotatedKey.keyId, tokenOf(rotatedKey, issuer = "https://auth.example.com/realms/internal"))).isFalse()
  }

  @Test
  suspend fun `a flood of distinct kids is capped, and the rotated key still arrives`() {
    val source = RecordingJwksSource(jwksDocumentOf(liveKey))
    val manager = manager(source, planner = JwksRefreshPlanner(window = 30.seconds, maxRefreshesPerMinute = 4))
    manager.loadHeldSet()
    manager.runRegularCycle()
    val fetchesBeforeFlood = source.fetchCount

    var requested = 0
    repeat(1000) { index ->
      val strangerKey = JwkKeyId("flood-$index")
      if (manager.requestRefreshFor(strangerKey, tokenOf(rotatedKey))) {
        requested++
        manager.runOutOfTurnCycle()
      }
      advance(60.milliseconds)
    }

    assertThat(requested).isEqualTo(2)
    assertThat(source.fetchCount - fetchesBeforeFlood).isEqualTo(2)

    //The realm rotated during the flood. A regular cycle is not paced by the flood, so the new key
    //arrives on the next interval — a fetch always brings the whole document.
    source.document = jwksDocumentOf(liveKey, rotatedKey)
    manager.runRegularCycle()

    assertThat(manager.keyOf(rotatedKey.keyId)).isNotNull()
  }

  @Test
  suspend fun `a kid the fetch did not deliver stops asking until the regular cycle`() {
    val source = RecordingJwksSource(jwksDocumentOf(liveKey))
    val manager = manager(source)
    manager.loadHeldSet()

    val stranger = JwkKeyId("stranger")
    assertThat(manager.requestRefreshFor(stranger, tokenOf(rotatedKey))).isTrue()
    manager.runOutOfTurnCycle()

    advance(1.minutes)
    assertThat(manager.requestRefreshFor(stranger, tokenOf(rotatedKey))).isFalse()

    manager.runRegularCycle()
    advance(1.minutes)
    assertThat(manager.requestRefreshFor(stranger, tokenOf(rotatedKey))).isTrue()
  }

  @Test
  fun `the start-up fetch adopts the realm's set before the service serves`() {
    val manager = manager(RecordingJwksSource(jwksDocumentOf(liveKey)), seed = seedOf(seedKey))
    manager.loadHeldSet()

    manager.fetchOnceBeforeServing()

    assertThat(manager.held?.origin).isEqualTo(JwksOrigin.Live)
  }

  @Test
  fun `a failing start-up fetch leaves the seed in place instead of failing the start`() {
    val source = RecordingJwksSource(jwksDocumentOf(liveKey)).also { it.failing = true }
    val manager = manager(source, seed = seedOf(seedKey))
    manager.loadHeldSet()

    manager.fetchOnceBeforeServing()

    assertThat(manager.held?.origin).isEqualTo(JwksOrigin.Seed)
    assertThat(manager.keyOf(seedKey.keyId)).isNotNull()
  }

  /**
   * `ServiceAuthentication.Keycloak` holds one manager, so a second Application built from the same
   * configuration installs authentication again while the first still runs. A second consumer of the
   * conflated refresh channel would split the requests with the first.
   */
  @Test
  fun `starting again while running starts nothing`() {
    val manager = manager(RecordingJwksSource(jwksDocumentOf(liveKey)))
    val scope = CoroutineScope(Job())
    try {
      assertThat(manager.start(scope)).isTrue()
      assertThat(manager.start(scope)).isFalse()
      assertThat(manager.isRunning).isTrue()
    } finally {
      scope.cancel()
      BackgroundJobRegistry.clear()
    }
  }

  /**
   * The guard asks whether the jobs are running, not whether they ever ran: both live in the
   * Application's scope, so the next Application built from the same manager has to start them again.
   */
  @Test
  suspend fun `starts again once the scope its jobs lived in is gone`() {
    val manager = manager(RecordingJwksSource(jwksDocumentOf(liveKey)))

    val firstScope = CoroutineScope(Job())
    assertThat(manager.start(firstScope)).isTrue()
    firstScope.cancel()
    firstScope.coroutineContext.job.join()
    assertThat(manager.isRunning).isFalse()

    val secondScope = CoroutineScope(Job())
    try {
      assertThat(manager.start(secondScope)).isTrue()
      assertThat(manager.isRunning).isTrue()
    } finally {
      secondScope.cancel()
      BackgroundJobRegistry.clear()
    }
  }

  /**
   * Two Applications built from one configuration share the manager. When the one that installed
   * authentication first ends, the other still validates tokens and needs both the regular cycle and
   * the out-of-turn path — and `/health` must not see the essential fetch job end.
   */
  @Test
  suspend fun `the jobs keep running for the surviving scope when the first scope ends`() {
    BackgroundJobRegistry.clear()
    val source = RecordingJwksSource(jwksDocumentOf(liveKey))
    //A window of zero spares the test an advance of the virtual clock, see below.
    val manager = manager(source, planner = JwksRefreshPlanner(window = Duration.ZERO, maxRefreshesPerMinute = 4))

    val firstScope = CoroutineScope(Job())
    val survivingScope = CoroutineScope(Job())
    try {
      assertThat(manager.start(firstScope)).isTrue()
      assertThat(manager.start(survivingScope)).isFalse()
      awaitFetchCount(source, 1)

      firstScope.cancel()
      firstScope.coroutineContext.job.join()

      assertThat(manager.isRunning).isTrue()
      assertThat(BackgroundJobRegistry.brokenJobs()).isEmpty()

      assertThat(manager.requestRefreshFor(JwkKeyId("after-first-scope"), tokenOf(rotatedKey))).isTrue()
      //The regular cycle is 15 minutes away, so a second fetch can only come from the consumer.
      awaitFetchCount(source, 2)
    } finally {
      firstScope.cancel()
      survivingScope.cancel()
      survivingScope.coroutineContext.job.join()
      BackgroundJobRegistry.clear()
    }
  }

  /**
   * Each started pair fetches at once, so a second pair would show as a second fetch.
   */
  @Test
  @Suppress("RealTimeDelayInTest") // a second fetch would show within the wait; no event signals its absence
  suspend fun `two running scopes share one pair of jobs`() {
    val source = RecordingJwksSource(jwksDocumentOf(liveKey))
    val manager = manager(source)

    val hostingScope = CoroutineScope(Job())
    val secondScope = CoroutineScope(Job())
    try {
      manager.start(hostingScope)
      manager.start(secondScope)
      awaitFetchCount(source, 1)

      //The second scope ending leaves the pair in the hosting scope alone.
      secondScope.cancel()
      secondScope.coroutineContext.job.join()
      delay(200.milliseconds)

      assertThat(manager.isRunning).isTrue()
      assertThat(source.fetchCount).isEqualTo(1)
    } finally {
      hostingScope.cancel()
      secondScope.cancel()
      BackgroundJobRegistry.clear()
    }
  }

  /**
   * The pair ends with the last scope, as cancelled by its owner: the registry does not report the
   * essential job as ended while the process shuts down.
   */
  @Test
  suspend fun `stops once the last scope ends`() {
    BackgroundJobRegistry.clear()
    val manager = manager(RecordingJwksSource(jwksDocumentOf(liveKey)))

    val firstScope = CoroutineScope(Job())
    val secondScope = CoroutineScope(Job())
    try {
      manager.start(firstScope)
      manager.start(secondScope)

      firstScope.cancel()
      firstScope.coroutineContext.job.join()
      secondScope.cancel()
      secondScope.coroutineContext.job.join()

      assertThat(manager.isRunning).isFalse()
      //The supervisor records its end from its own finally, after the cancellation reached it.
      awaitCondition(10.seconds, { "the end of ${manager.backgroundJobName}" }) { BackgroundJobRegistry.stateOf(manager.backgroundJobName)?.endReason != null }
      assertThat(BackgroundJobRegistry.stateOf(manager.backgroundJobName)?.endReason).isEqualTo(RepeatingEndReason.Cancelled)
    } finally {
      firstScope.cancel()
      secondScope.cancel()
      BackgroundJobRegistry.clear()
    }
  }

  @Test
  fun `a scope that already ended starts nothing`() {
    val manager = manager(RecordingJwksSource(jwksDocumentOf(liveKey)))
    val endedScope = CoroutineScope(Job()).also { it.cancel() }

    assertThat(manager.start(endedScope)).isFalse()
    assertThat(manager.isRunning).isFalse()
  }

  /**
   * The refresh channel outlives every scope its consumer ran in. A consuming operator would close it
   * with the first consumer, and `requestRefreshFor` would go on accepting requests nobody answers.
   */
  @Test
  suspend fun `an out-of-turn request reaches the consumer of the second scope`() {
    val source = RecordingJwksSource(jwksDocumentOf(liveKey))
    //A window of zero spares the test an advance of the virtual clock. Writing it while the started
    //jobs read now() on their own threads would be a data race.
    val manager = manager(source, planner = JwksRefreshPlanner(window = Duration.ZERO, maxRefreshesPerMinute = 4))

    val firstScope = CoroutineScope(Job())
    manager.start(firstScope)
    awaitFetchCount(source, 1)
    //The first scope answers an out-of-turn request too, so a failure in the second one cannot mean
    //that the path never worked.
    assertThat(manager.requestRefreshFor(JwkKeyId("first-scope"), tokenOf(rotatedKey))).isTrue()
    awaitFetchCount(source, 2)

    firstScope.cancel()
    firstScope.coroutineContext.job.join()

    val secondScope = CoroutineScope(Job())
    try {
      manager.start(secondScope)
      awaitFetchCount(source, 3)

      assertThat(manager.requestRefreshFor(JwkKeyId("second-scope"), tokenOf(rotatedKey))).isTrue()
      //The regular cycle is 15 minutes away, so a fourth fetch can only come from the consumer.
      awaitFetchCount(source, 4)
    } finally {
      secondScope.cancel()
      BackgroundJobRegistry.clear()
    }
  }

  /**
   * Registration and cycle recording are two different claims. The registration happens when the job
   * is declared; that a completed cycle reaches the registry is what makes `/health` show the job
   * doing its work rather than merely existing.
   */
  @Test
  suspend fun `the fetch job is registered and its completed cycles reach the registry`() {
    BackgroundJobRegistry.clear()
    val manager = manager(RecordingJwksSource(jwksDocumentOf(liveKey)))

    val scope = CoroutineScope(Job())
    try {
      manager.start(scope)

      awaitCondition(10.seconds, { "a completed cycle of ${manager.backgroundJobName}" }) {
        (BackgroundJobRegistry.stateOf(manager.backgroundJobName)?.completedCycles ?: 0) > 0
      }
      val completed = BackgroundJobRegistry.stateOf(manager.backgroundJobName).requireNotNull { "${manager.backgroundJobName} is not registered" }

      assertThat(completed.name).isEqualTo(BackgroundJobName("jwks-fetch-internal"))
      assertThat(completed.stalenessThreshold).isEqualTo(manager.refreshInterval * 3)
      assertThat(completed.completedCycles).isGreaterThan(0)
    } finally {
      scope.cancel()
      BackgroundJobRegistry.clear()
    }
  }

  /**
   * Waits until [source] has been asked for [expected] fetches.
   */
  private suspend fun awaitFetchCount(source: RecordingJwksSource, expected: Int) {
    awaitCondition(10.seconds, { "$expected fetches, reached ${source.fetchCount}" }) { source.fetchCount >= expected }
    //One fetch too many fails as well.
    assertThat(source.fetchCount).isEqualTo(expected)
  }

  private fun seedOf(key: JwksTestKey): JwksSeed = JwksSeed { jwksDocumentOf(key) }

  private fun tokenOf(key: JwksTestKey, issuer: String = realm): DecodedJWT {
    val signed = JWT.create()
      .withKeyId(key.keyId.value)
      .withIssuer(issuer)
      .withAudience(client)
      .withExpiresAt(Date(now().toEpochMilliseconds() + 1.hours.inWholeMilliseconds))
      .sign(Algorithm.RSA256(key.publicKey, key.privateKey))

    return JWT.decode(signed)
  }
}

/**
 * [JwksPersistence] in memory — the file behaviour is [FileJwksPersistenceTest]'s subject, the
 * manager only needs something that survives a simulated restart.
 */
class InMemoryJwksPersistence(
  private var document: JwksDocument? = null,
  private var fetchedAt: Instant = now(),
) : JwksPersistence {
  override fun read(): PersistedJwks? = document?.let { PersistedJwks(it, fetchedAt) }

  override fun write(document: JwksDocument) {
    this.document = document
    this.fetchedAt = now()
  }
}
