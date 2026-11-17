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
package it.neckar.open.test.utils.testcontainer

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PublishPortsOnDockerHostOnlyTest {

  @Test
  fun `every binding is rewritten, each keeping its own host port`() {
    val randomized = ExposedPort.tcp(27017)
    val fixed = ExposedPort.tcp(8080)
    val hostConfig = HostConfig().withPortBindings(
      PortBinding(Ports.Binding.empty(), randomized),
      PortBinding(Ports.Binding.bindPort(38080), fixed),
    )

    createCommand(hostConfig).rewritePublishedPortsTo("172.17.0.1")

    val rewritten = hostConfig.portBindings.bindings
    assertThat(rewritten.getValue(randomized).single().hostIp).isEqualTo("172.17.0.1")
    assertThat(rewritten.getValue(randomized).single().hostPortSpec).isNull()
    assertThat(rewritten.getValue(fixed).single().hostIp).isEqualTo("172.17.0.1")
    assertThat(rewritten.getValue(fixed).single().hostPortSpec).isEqualTo("38080")
  }

  @Test
  fun `a port with two bindings keeps both`() {
    val exposedPort = ExposedPort.tcp(5432)
    val hostConfig = HostConfig().withPortBindings(
      PortBinding(Ports.Binding.bindPort(15432), exposedPort),
      PortBinding(Ports.Binding.bindPort(25432), exposedPort),
    )

    createCommand(hostConfig).rewritePublishedPortsTo("127.0.0.1")

    val rewritten = hostConfig.portBindings.bindings.getValue(exposedPort)
    assertThat(rewritten.map { it.hostIp }).isEqualTo(listOf("127.0.0.1", "127.0.0.1"))
    assertThat(rewritten.map { it.hostPortSpec }).isEqualTo(listOf("15432", "25432"))
  }

  @Test
  fun `a blank bind address is rejected`() {
    val hostConfig = HostConfig().withPortBindings(PortBinding(Ports.Binding.empty(), ExposedPort.tcp(80)))

    val exception = assertThrows<IllegalArgumentException> {
      createCommand(hostConfig).rewritePublishedPortsTo("")
    }
    assertThat(exception).hasMessage("Blank bind address for <the-image>")
  }

  @Test
  fun `an exposed port without a binding is rejected`() {
    val exposedPort = ExposedPort.tcp(80)
    val hostConfig = HostConfig().withPortBindings(Ports().apply { bind(exposedPort, null) })

    val exception = assertThrows<IllegalArgumentException> {
      createCommand(hostConfig).rewritePublishedPortsTo("127.0.0.1")
    }
    assertThat(exception).hasMessage("Port $exposedPort of <the-image> is exposed without a binding to rewrite")
  }

  @Test
  fun `a command without port bindings is rejected`() {
    val exception = assertThrows<IllegalArgumentException> {
      createCommand(HostConfig()).rewritePublishedPortsTo("127.0.0.1")
    }
    assertThat(exception).hasMessage("No port bindings on the create command for <the-image>")
  }

  @Test
  fun `a command without a host config is rejected`() {
    val exception = assertThrows<IllegalArgumentException> {
      createCommand(hostConfig = null).rewritePublishedPortsTo("127.0.0.1")
    }
    assertThat(exception).hasMessage("No host config on the create command for <the-image>")
  }

  @Test
  fun `a literal address is taken as it is`() {
    assertThat(resolveBindAddress("172.17.0.1")).isEqualTo("172.17.0.1")
  }

  @Test
  fun `an absent docker host is rejected`() {
    listOf(null, "", "  ").forEach { dockerHost ->
      val exception = assertThrows<IllegalArgumentException> {
        resolveBindAddress(dockerHost)
      }
      assertThat(exception).hasMessage("Testcontainers resolved no Docker host — check DOCKER_HOST and TESTCONTAINERS_HOST_OVERRIDE")
    }
  }

  private fun createCommand(hostConfig: HostConfig?): CreateContainerCmd {
    return mockk<CreateContainerCmd> {
      every { this@mockk.hostConfig } returns hostConfig
      every { image } returns "the-image"
    }
  }
}
