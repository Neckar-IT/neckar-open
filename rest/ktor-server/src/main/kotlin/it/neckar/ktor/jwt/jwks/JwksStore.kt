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
import it.neckar.open.time.now
import it.neckar.open.unit.other.GuardedBy
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Instant

/**
 * Where the key set a service currently holds came from.
 */
enum class JwksOrigin(
  /**
   * The word an operator reads on `/health/ready` and in `global-health`.
   */
  val label: String,
) {
  /**
   * A fetch in this process succeeded.
   */
  Live("live"),

  /**
   * The last successful fetch of an earlier process, read back from disk.
   */
  Persisted("persisted"),

  /**
   * The JWKS file checked into the repository and built into the image. Never confirmed against the
   * realm by this deployment.
   */
  Seed("seed"),
}

/**
 * The key set a service holds, and where it came from.
 */
data class HeldJwks(
  val document: JwksDocument,
  val origin: JwksOrigin,
  /**
   * When the set was fetched — for [JwksOrigin.Persisted] the time of the fetch that wrote the file,
   * not the time it was read back.
   */
  val obtainedAt: Instant,
)

/**
 * Holds the JWKS key set of one realm for the lifetime of the process.
 *
 * Separates the three things `JwkProviderBuilder(…).cached(…)` conflated: fetching, holding, and
 * having confirmed. Nothing here fetches, nothing here expires — a set is replaced only by the whole
 * result of a successful fetch, so a realm that never rotates keeps its keys for as long as the
 * process runs.
 *
 * Thread-safe. Read by the authentication path on every request, written by the fetch job.
 */
class JwksStore {
  private val lock = ReentrantReadWriteLock()

  @GuardedBy("lock")
  private var _held: HeldJwks? = null

  /**
   * Set once a fetch has succeeded — including a fetch of an earlier process, which is what the
   * persisted set is. Never cleared: the seed is a first-boot fallback, and something newer has been
   * known since.
   */
  @GuardedBy("lock")
  private var confirmed: Boolean = false

  /**
   * The held key set, or `null` while the service holds none.
   */
  val held: HeldJwks?
    get() = lock.read { _held }

  /**
   * The key [keyId], or `null` when the held set does not contain it — and `null` too while no set
   * is held at all.
   */
  fun keyOf(keyId: JwkKeyId): Jwk? = lock.read { _held?.document?.get(keyId) }

  /**
   * Adopts the result of a successful fetch. Replaces whatever was held, whole, and marks the store
   * confirmed for the rest of the process.
   */
  fun adoptFetched(document: JwksDocument) {
    lock.write {
      _held = HeldJwks(document, JwksOrigin.Live, now())
      confirmed = true
    }
  }

  /**
   * Adopts the persisted set of an earlier process, [fetchedAt] being when that fetch happened.
   *
   * Counts as confirmed: a file only exists because a fetch produced it. Ignored when a set is
   * already held, so a late read never overwrites a live one. Returns whether it was adopted.
   */
  fun adoptPersisted(document: JwksDocument, fetchedAt: Instant): Boolean = lock.write {
    if (_held != null) {
      return@write false
    }
    _held = HeldJwks(document, JwksOrigin.Persisted, fetchedAt)
    confirmed = true
    true
  }

  /**
   * Adopts the seed built into the image.
   *
   * Applies only while nothing is held and no fetch has ever succeeded — the seed is what a service
   * starts from before it has ever spoken to the realm, and it is never reached for again afterwards.
   * Returns whether it was adopted.
   */
  fun adoptSeed(document: JwksDocument): Boolean = lock.write {
    if (_held != null || confirmed) {
      return@write false
    }
    _held = HeldJwks(document, JwksOrigin.Seed, now())
    true
  }
}
