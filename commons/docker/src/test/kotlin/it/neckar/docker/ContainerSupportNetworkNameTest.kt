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
package it.neckar.docker

import assertk.*
import assertk.assertions.*
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import io.mockk.every
import io.mockk.mockk
import it.neckar.docker.chrome.ChromeDockerSupport
import it.neckar.docker.mongodb.MongoDbDockerSupport
import it.neckar.docker.network.NetworkName
import it.neckar.docker.nginx.NginxDockerSupport
import it.neckar.projects.common.docker.DockerImageDescriptor
import it.neckar.projects.common.docker.DockerRepository
import it.neckar.projects.common.docker.DockerTag
import org.junit.jupiter.api.Test

/**
 * The network a factory puts a container on has to reach [DockerContainerSupport.networkName] -
 * [DockerContainerSupport.getDockerHostAddress] looks the gateway up under exactly that name.
 */
class ContainerSupportNetworkNameTest {
  /** No container starts, so any reference does. */
  private val chromeImage: DockerImageDescriptor = DockerImageDescriptor.neckarIt(DockerRepository("chrome-headless-beta"), DockerTag("test"))

  @Test
  fun `chrome factory hands its network to the container support`() {
    val containerSupport = ChromeDockerSupport.create(dockerClientCreatingContainers(), chromeImage)
      .createDockerContainerSupport(name = ContainerName("chrome"), networkName = NetworkName("my-e2e-net"))

    assertThat(containerSupport.networkName).isEqualTo(NetworkName("my-e2e-net"))
  }

  @Test
  fun `nginx factory hands its network to the container support`() {
    val containerSupport = NginxDockerSupport.create(dockerClientCreatingContainers(), DockerImageDescriptor.default(DockerRepository("library/nginx"), DockerTag("1.31.8-alpine")))
      .createDockerContainer(name = ContainerName("nginx"), binds = emptyList(), networkName = NetworkName("my-e2e-net"))

    assertThat(containerSupport.networkName).isEqualTo(NetworkName("my-e2e-net"))
  }

  @Test
  fun `mongodb factory hands its network to the container support`() {
    val containerSupport = MongoDbDockerSupport.create(dockerClientCreatingContainers(), DockerImageDescriptor.default(DockerRepository("library/mongo"), DockerTag("9.0.4")))
      .createDockerContainer(name = ContainerName("mongodb"), networkName = NetworkName("my-e2e-net"))

    assertThat(containerSupport.networkName).isEqualTo(NetworkName("my-e2e-net"))
  }

  @Test
  fun `no network means the default bridge`() {
    val containerSupport = ChromeDockerSupport.create(dockerClientCreatingContainers(), chromeImage)
      .createDockerContainerSupport(name = ContainerName("chrome"), networkName = null)

    assertThat(containerSupport.networkName).isEqualTo(NetworkName("bridge"))
  }

  /**
   * Answers `createContainerCmd` with a container id - no docker daemon is involved.
   */
  private fun dockerClientCreatingContainers(): DockerClient {
    val response = mockk<CreateContainerResponse>()
    every { response.id } returns "0".repeat(64)

    val createContainerCmd = mockk<CreateContainerCmd>()
    every { createContainerCmd.withName(any()) } returns createContainerCmd
    every { createContainerCmd.withHostConfig(any()) } returns createContainerCmd
    every { createContainerCmd.exec() } returns response

    val dockerClient = mockk<DockerClient>()
    every { dockerClient.createContainerCmd(any<String>()) } returns createContainerCmd
    return dockerClient
  }
}
