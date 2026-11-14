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
package it.neckar.docker.mongodb

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.PortBinding
import it.neckar.docker.ContainerName
import it.neckar.docker.DockerSupport
import it.neckar.docker.network.NetworkName
import it.neckar.projects.common.docker.DockerImageDescriptor
import javax.annotation.WillClose

/**
 * Starts a docker container with MongoDB
 */
class MongoDbDockerSupport(
  /**
   * The docker client to use.
   * Will be closed when calling [dispose]
   */
  @WillClose dockerClient: DockerClient,
  /**
   * The image to use
   */
  image: DockerImageDescriptor,
) : DockerSupport(
  dockerClient = dockerClient,
  image = image,
) {

  /**
   * Creates a new docker container - that is *not* running
   */
  fun createDockerContainer(
    name: ContainerName,
    portBindings: List<PortBinding> = DefaultPortBindings,
    networkName: NetworkName?,
  ): MongoDbDockerContainerSupport {
    val id = createContainer(name, emptyList(), portBindings, networkName)

    return MongoDbDockerContainerSupport(
      dockerClient = dockerClient,
      containerId = id,
      containerName = name,
      networkName = networkName ?: NetworkName.Bridge,
    )
  }

  companion object {
    /**
     * Creates a new instance for [dockerImage]
     */
    fun create(dockerClient: DockerClient, dockerImage: DockerImageDescriptor): MongoDbDockerSupport {
      return MongoDbDockerSupport(dockerClient, dockerImage)
    }

    /**
     * The default port bindings
     */
    val DefaultPortBindings: List<PortBinding> = listOf(
      PortBinding.parse("27017:27017"),
    )
  }
}
