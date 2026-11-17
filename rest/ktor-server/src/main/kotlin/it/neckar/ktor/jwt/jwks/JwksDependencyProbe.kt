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

import it.neckar.open.health.DependencyClassification
import it.neckar.open.health.DependencyProbe
import it.neckar.open.health.DependencyStatus
import it.neckar.open.time.now
import it.neckar.rest.health.DependencyName

/**
 * Makes the realm's public signature keys the required dependency they are: a service that holds
 * none can authenticate nothing and answers every caller `401`, so it must not be ready.
 *
 * What it reports is whether a set is *held*, never whether the last fetch succeeded. An unreachable
 * realm leaves a service with a valid set fully functional, and taking it out of the load balancer
 * would turn a Keycloak outage into a total one.
 *
 * The origin of the held set travels with the status: a set that came from the seed serves and
 * reports `degraded`, because no fetch of this deployment ever confirmed those keys.
 *
 * Reads memory only — nothing here can block, so the default probe timeout is never approached.
 */
class JwksDependencyProbe(
  private val store: JwksStore,
  override val name: DependencyName = DependencyName.Jwks,
  override val classification: DependencyClassification = DependencyClassification.Required,
) : DependencyProbe {

  override suspend fun check(): DependencyStatus {
    val held = store.held ?: return DependencyStatus.Down(NoJwksHeldException(), now())

    return when (held.origin) {
      JwksOrigin.Seed -> DependencyStatus.Degraded("${JwksOrigin.Seed.label}, never confirmed", now())
      JwksOrigin.Live -> DependencyStatus.Up(held.origin.label)
      JwksOrigin.Persisted -> DependencyStatus.Up("${held.origin.label}, fetched ${held.obtainedAt}")
    }
  }
}

/**
 * The service holds no signature keys at all: no fetch has succeeded, no set was persisted, and the
 * image carries no seed. Every token it is handed is unverifiable.
 */
class NoJwksHeldException : RuntimeException("no JWKS key set held")
