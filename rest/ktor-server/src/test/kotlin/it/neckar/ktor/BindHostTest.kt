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
package it.neckar.ktor

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isEqualTo
import io.ktor.server.application.Application
import it.neckar.open.test.utils.DisableLogging
import it.neckar.projects.common.LocalDev
import it.neckar.projects.common.Port
import it.neckar.projects.common.ServiceDescriptor
import it.neckar.runtime.context.ExecutionEnvironment
import org.junit.jupiter.api.Test
import java.net.BindException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisableLogging
class BindHostTest {
  @Test
  fun `a local run listens on the loopback address and the Dev-IP`() {
    assertThat(BindHost.of(ExecutionEnvironment.LocalDev, inUnitTest = false)).isEqualTo(BindHost.LocalRun)
  }

  @Test
  fun `a standalone process listens on all interfaces`() {
    assertThat(BindHost.of(ExecutionEnvironment.Standalone, inUnitTest = false)).isEqualTo(BindHost.AllInterfaces)
  }

  @Test
  fun `a container listens on all interfaces`() {
    assertThat(BindHost.of(ExecutionEnvironment.Docker, inUnitTest = false)).isEqualTo(BindHost.AllInterfaces)
  }

  @Test
  fun `a CI process listens on all interfaces`() {
    assertThat(BindHost.of(ExecutionEnvironment.CI, inUnitTest = false)).isEqualTo(BindHost.AllInterfaces)
  }

  @ParameterizedTest
  @EnumSource(ExecutionEnvironment::class)
  fun `a unit test listens on the loopback address in every environment`(executionEnvironment: ExecutionEnvironment) {
    assertThat(BindHost.of(executionEnvironment, inUnitTest = true)).isEqualTo(BindHost.Loopback)
  }

  private val service: KtorService = object : KtorService {
    override val serviceDescriptor: ServiceDescriptor = ServiceDescriptor("bind-host-service", apiPrefix = "bind-host", Port(10_100))
    override val additionalPorts: List<Port> = listOf(Port(10_101))

    override fun Application.configure() {
    }
  }

  @Test
  fun `the server of a local run binds every port on the loopback address and the Dev-IP`() {
    val connectors = server(service, service.effectivePort, bindHost = BindHost.LocalRun).engineConfig.connectors

    assertThat(connectors.map { "${it.host}:${it.port}" }).isEqualTo(
      listOf("127.0.0.1:10100", "${LocalDev.Address}:10100", "127.0.0.1:10101", "${LocalDev.Address}:10101"),
    )
  }

  @Test
  fun `a failed bind is hinted at only for a server that binds the Dev-IP`() {
    val failedBind = IllegalStateException("start failed", BindException("Cannot assign requested address"))

    assertThat(bindFailureHint(failedBind, BindHost.LocalRun.addresses)).isNotNull().contains(LocalDev.Address)
    assertThat(bindFailureHint(failedBind, BindHost.AllInterfaces.addresses)).isNull()
    assertThat(bindFailureHint(IllegalStateException("module failed"), BindHost.LocalRun.addresses)).isNull()
  }

  @Test
  fun `the server of a unit test binds every connector to the loopback address`() {
    assertThat(server(service, service.effectivePort).engineConfig.connectors.map { it.host }).isEqualTo(listOf("127.0.0.1", "127.0.0.1"))
    assertThat(service.baseUrl.value).isEqualTo("http://localhost:10100/")
  }
}
