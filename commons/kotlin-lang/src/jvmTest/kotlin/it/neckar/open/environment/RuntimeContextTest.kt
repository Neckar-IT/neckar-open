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
package it.neckar.open.environment

import assertk.*
import assertk.assertions.*
import it.neckar.runtime.context.DeploymentStage
import it.neckar.runtime.context.ExecutionEnvironment
import it.neckar.runtime.context.FixedHostRegistry
import it.neckar.runtime.context.Hostname
import it.neckar.runtime.context.RuntimeContext
import it.neckar.runtime.context.RuntimeContextDetector
import it.neckar.runtime.context.ServiceHost
import it.neckar.runtime.context.initialExecutionEnvironment
import it.neckar.runtime.context.initializeFromEnvironment
import it.neckar.runtime.context.RuntimeContextEnv
import it.neckar.runtime.context.RuntimeContextSys
import org.junit.jupiter.api.Test

/**
 *
 */
class RuntimeContextTest {
  @Test
  fun testInitial() {
    assertThat(RuntimeContext.current.inUnitTest).isTrue()
  }

  @Test
  fun testInitializationDefaults() {
    val runtimeContext = RuntimeContext.initializeFromEnvironment(TestHost)
    assertThat(RuntimeContext.current).isEqualTo(runtimeContext)

    assertThat(runtimeContext.inUnitTest).isTrue()
    assertThat(runtimeContext.stage).isEqualTo(DeploymentStage.Development)
  }

  @Test
  fun testInitializationSystemProperties() {
    val source = RuntimeContextDetector.Source(
      systemProperties = mapOf(
        RuntimeContextSys.DeploymentStageKey to DeploymentStage.Production.name,
        RuntimeContextSys.ExecutionEnvironmentKey to ExecutionEnvironment.Docker.name,
      ),
      environment = mapOf(),
    )

    val runtimeContext = RuntimeContext.initializeFromEnvironment(TestHost, source, forcedExecutionEnvironment = ExecutionEnvironment.Docker)

    assertThat(runtimeContext.inUnitTest).isTrue()
    assertThat(runtimeContext.stage).isEqualTo(DeploymentStage.Production)
    assertThat(runtimeContext.executionEnvironment).isEqualTo(ExecutionEnvironment.Docker)
  }

  @Test
  fun `the initial value takes a declared execution environment`() {
    val declared = RuntimeContextDetector.Source(emptyMap(), mapOf(RuntimeContextEnv.ExecutionEnvironmentKey to ExecutionEnvironment.Docker.name))
    val undeclared = RuntimeContextDetector.Source(emptyMap(), emptyMap())

    assertThat(initialExecutionEnvironment(inCI = false, declared)).isEqualTo(ExecutionEnvironment.Docker)
    assertThat(initialExecutionEnvironment(inCI = false, undeclared)).isEqualTo(ExecutionEnvironment.LocalDev)
    assertThat(initialExecutionEnvironment(inCI = true, declared)).isEqualTo(ExecutionEnvironment.CI)
  }

  @Test
  fun testInitializationSystemEnv() {
    val source = RuntimeContextDetector.Source(
      systemProperties = mapOf(
      ),
      environment = mapOf(
        RuntimeContextEnv.DeploymentStageKey to DeploymentStage.Production.name,
        RuntimeContextEnv.ExecutionEnvironmentKey to ExecutionEnvironment.Docker.name,
      ),
    )

    val runtimeContext = RuntimeContext.initializeFromEnvironment(serviceHostRegistry = TestHost, source = source, forcedExecutionEnvironment = ExecutionEnvironment.Docker)

    assertThat(runtimeContext.inUnitTest).isTrue()
    assertThat(runtimeContext.stage).isEqualTo(DeploymentStage.Production)
    assertThat(runtimeContext.executionEnvironment).isEqualTo(ExecutionEnvironment.Docker)
  }

  private enum class TestHost(
    override val hostname: Hostname,
    ) : ServiceHost {
    Localhost(Hostname.localhost),
    Remote(Hostname("remote.example.com"));


    companion object : FixedHostRegistry<TestHost> {
      override val knownHosts: List<TestHost> = entries.toList()

      override fun resolveHost(hostname: Hostname?): TestHost {
        return when (hostname) {
          Localhost.hostname -> Localhost
          Remote.hostname -> Remote
          else -> Localhost // Default to localhost if not found
        }
      }
    }
  }
}
