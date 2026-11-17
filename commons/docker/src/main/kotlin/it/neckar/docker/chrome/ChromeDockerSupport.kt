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
package it.neckar.docker.chrome

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateNetworkResponse
import com.github.dockerjava.api.model.Network
import com.github.dockerjava.api.model.PortBinding
import it.neckar.docker.ContainerName
import it.neckar.docker.DockerSupport
import it.neckar.docker.network.DockerNetworkSupport
import it.neckar.docker.network.NetworkId
import it.neckar.docker.network.NetworkName
import it.neckar.projects.common.Port
import it.neckar.projects.common.docker.DockerImageDescriptor
import javax.annotation.WillClose

/**
 * Starts a docker container with a Chrome browser
 */
class ChromeDockerSupport(
  /**
   * The docker client to use.
   * Will be closed when calling [dispose]
   */
  @WillClose dockerClient: DockerClient,
  /**
   * The image to use
   */
  image: DockerImageDescriptor,
) : DockerSupport(dockerClient, image) {

  /**
   * Creates a new docker container - that is *not* running
   */
  fun createDockerContainerSupport(
    name: ContainerName,
    portBindings: List<PortBinding> = DefaultPortBindings,
    networkName: NetworkName?,
  ): ChromeDockerContainerSupport {
    val id = createContainer(
      name = name,
      binds = emptyList(),
      portBindings = portBindings,
      networkName = networkName,
      shmSize = OneGigabyteInBytes,
    )

    return ChromeDockerContainerSupport(
      dockerClient = dockerClient,
      containerId = id,
      containerName = name,
      remoteDebuggingPort = InternalPort,
      networkName = networkName ?: NetworkName.Bridge,
    )
  }

  /**
   * Creates a new network
   */
  @Deprecated("Use DockerNetworkSupport.createNetwork")
  fun createNetwork(networkName: NetworkName): DockerNetworkSupport {
    val ipamConfig: Network.Ipam = Network.Ipam()
      .withConfig(
      )
      .withConfig(
        Network.Ipam.Config()
        //.withSubnet("172.28.0.0/16")
      )

    val network: CreateNetworkResponse = dockerClient.createNetworkCmd()
      .withName(networkName.value)
      .withDriver("bridge")
      .withIpam(ipamConfig)
      .exec()

    return DockerNetworkSupport(dockerClient, NetworkId(network.id))
  }

  companion object {
    /**
     * Creates a new instance for [dockerImage]
     */
    fun create(dockerClient: DockerClient, dockerImage: DockerImageDescriptor): ChromeDockerSupport {
      return ChromeDockerSupport(dockerClient, dockerImage)
    }



    val ExternalPort: Port = Port.of(9222)

    /**
     * The chrome remote debugging port.
     */
    val InternalPort: Port = Port.of(9222)

    /**
     * The default port bindings
     */
    val DefaultPortBindings: List<PortBinding> = listOf(
      PortBinding.parse("$ExternalPort:$InternalPort"),
    )

    /**
     * 1 GB in bytes — shared memory size for Chrome containers.
     * Chrome requires sufficient /dev/shm for stable operation.
     */
    const val OneGigabyteInBytes: Long = 1L * 1024 * 1024 * 1024
  }
}
