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
package it.neckar.docker.nginx

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Volume
import it.neckar.docker.ContainerName
import it.neckar.docker.DockerSupport
import it.neckar.docker.network.NetworkName
import it.neckar.docker.nginx.NginxDockerSupport.Companion.ExternalPort
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.projects.common.Port
import it.neckar.projects.common.docker.DockerImageDescriptor

/**
 * Starts a docker containers with a Nginx server
 */
class NginxDockerSupport(
  /**
   * The docker client to use.
   */
  dockerClient: DockerClient,
  /**
   * The image to use
   */
  image: DockerImageDescriptor,
) : DockerSupport(dockerClient, image) {

  /**
   * Creates a new docker container - that is *not* running
   */
  fun createDockerContainer(
    name: ContainerName,
    binds: List<Bind>,
    portBindings: List<PortBinding> = DefaultPortBindings,
    networkName: NetworkName? = null,
  ): NginxDockerContainerSupport {
    val id = super.createContainer(name, binds, portBindings, networkName, shmSize = null)

    return NginxDockerContainerSupport(
      dockerClient = dockerClient,
      containerId = id,
      containerName = name,
      remotePort = ExternalPort,
      networkName = networkName ?: NetworkName.Bridge,
    )
  }

  /**
   * Creates the docker container for the provided content root
   */
  fun createDockerContainer(
    name: ContainerName,
    contentPath: String,
    portBindings: List<PortBinding> = DefaultPortBindings,
    networkName: NetworkName? = null,
  ): NginxDockerContainerSupport {

    val logger: Logger = LoggerFactory.getLogger("it.neckar.docker.nginx.NginxDockerSupport")


    logger.info("contentPath: <${contentPath}>")

    val binds = listOf(
      Bind(contentPath, Volume("/usr/share/nginx/html"), AccessMode.ro),
    )


    return createDockerContainer(
      name = name,
      binds = binds,
      portBindings = portBindings,
      networkName = networkName,
    )
  }

  companion object {
    /**
     * Creates a new instance for [dockerImage]
     */
    fun create(dockerClient: DockerClient, dockerImage: DockerImageDescriptor): NginxDockerSupport {
      return NginxDockerSupport(dockerClient, dockerImage)
    }

    /**
     * The port nginx is listening to internally
     */
    val InternalPort: Port = Port.Http

    /**
     * The internal nginx port is bound to this port
     */
    val ExternalPort: Port = Port.of(7080)

    /**
     * Binds the [ExternalPort] to 80
     */
    val DefaultPortBindings: List<PortBinding> = listOf(PortBinding.parse("$ExternalPort:$InternalPort"))
  }
}
