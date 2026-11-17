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
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import it.neckar.open.health.DependencyClassification
import it.neckar.open.health.DependencyState
import it.neckar.open.health.DependencyStatus
import it.neckar.open.health.HealthProjection
import it.neckar.open.health.HealthHttpStatus
import it.neckar.open.health.ServiceState
import it.neckar.open.health.ServiceStateHolder
import it.neckar.open.health.from
import it.neckar.open.time.now
import it.neckar.rest.error.GlobalHealth
import it.neckar.rest.health.DependencyName
import it.neckar.rest.health.DependencyReachability
import it.neckar.rest.health.ServiceStateDto
import org.junit.jupiter.api.Test

class JwksDependencyProbeTest {
  private val key = JwksTestKey.generate("some-key")
  private val store = JwksStore()
  private val probe = JwksDependencyProbe(store)

  @Test
  suspend fun `without any key set the dependency is down and the service is not ready`() {
    val status = probe.check()
    assertThat(status).isInstanceOf(DependencyStatus.Down::class)
      .prop(DependencyStatus.Down::error).isInstanceOf(NoJwksHeldException::class)

    val dependencies = listOf(DependencyState(DependencyName.Jwks, DependencyClassification.Required, status))
    assertThat(HealthProjection.wireState(ServiceState.Ready, dependencies)).isEqualTo(ServiceStateDto.DependencyUnavailable)
    assertThat(HealthProjection.readinessStatus(ServiceState.Ready, dependencies)).isEqualTo(HealthHttpStatus.ServiceUnavailable)
  }

  @Test
  suspend fun `a seed-only key set serves and reports degraded`() {
    check(store.adoptSeed(jwksDocumentOf(key))) { "the seed must be adopted into an empty store" }

    val status = probe.check()
    assertThat(status).isInstanceOf(DependencyStatus.Degraded::class)
      .prop(DependencyStatus.Degraded::detail).isEqualTo("seed, never confirmed")

    val dependencies = listOf(DependencyState(DependencyName.Jwks, DependencyClassification.Required, status))
    assertThat(HealthProjection.wireState(ServiceState.Ready, dependencies)).isEqualTo(ServiceStateDto.Degraded)
    assertThat(HealthProjection.readinessStatus(ServiceState.Ready, dependencies)).isEqualTo(HealthHttpStatus.Ok)
  }

  @Test
  suspend fun `a fetched key set is up and names its origin`() {
    store.adoptFetched(jwksDocumentOf(key))

    assertThat(probe.check()).isEqualTo(DependencyStatus.Up("live"))
  }

  @Test
  suspend fun `a persisted key set is up and names when it was fetched`() {
    val fetchedAt = now()
    check(store.adoptPersisted(jwksDocumentOf(key), fetchedAt)) { "the persisted set must be adopted into an empty store" }

    assertThat(probe.check()).isEqualTo(DependencyStatus.Up("persisted, fetched $fetchedAt"))
  }

  @Test
  suspend fun `global-health reports unhealthy without a key set and healthy with a fetched one`() {
    val holder = ServiceStateHolder()
    holder.transitionTo(ServiceState.Verifying)
    holder.transitionTo(ServiceState.Ready)
    holder.updateDependency(DependencyName.Jwks, DependencyClassification.Required, probe.check())

    assertThat(GlobalHealth.from(holder)).isInstanceOf(GlobalHealth.Unhealthy::class)
      .prop(GlobalHealth.Unhealthy::state).isEqualTo(ServiceStateDto.DependencyUnavailable)

    store.adoptFetched(jwksDocumentOf(key))
    holder.updateDependency(DependencyName.Jwks, DependencyClassification.Required, probe.check())

    assertThat(GlobalHealth.from(holder)).isEqualTo(GlobalHealth.Healthy)
  }

  @Test
  suspend fun `global-health names the seed as the degradation, so nothing reads it as a fresh deployment`() {
    val holder = ServiceStateHolder()
    holder.transitionTo(ServiceState.Verifying)
    holder.transitionTo(ServiceState.Ready)
    check(store.adoptSeed(jwksDocumentOf(key))) { "the seed must be adopted into an empty store" }
    holder.updateDependency(DependencyName.Jwks, DependencyClassification.Required, probe.check())

    assertThat(GlobalHealth.from(holder)).isInstanceOf(GlobalHealth.Unhealthy::class)
      .prop(GlobalHealth.Unhealthy::message).isEqualTo("jwks is degraded: seed, never confirmed")
  }

  @Test
  suspend fun `the origin reaches the readiness body`() {
    check(store.adoptSeed(jwksDocumentOf(key))) { "the seed must be adopted into an empty store" }
    val dependencies = listOf(DependencyState(DependencyName.Jwks, DependencyClassification.Required, probe.check()))

    val body = HealthProjection.readinessResponse(ServiceState.Ready, dependencies)

    assertThat(body.dependencies.single().status).isEqualTo(DependencyReachability.Degraded)
    assertThat(body.dependencies.single().detail).isEqualTo("seed, never confirmed")
  }
}
