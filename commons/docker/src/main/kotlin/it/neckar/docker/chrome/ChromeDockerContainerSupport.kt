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
import it.neckar.docker.ContainerId
import it.neckar.docker.ContainerName
import it.neckar.docker.DockerContainerSupport
import it.neckar.docker.network.NetworkName
import it.neckar.open.http.Url
import it.neckar.projects.common.Port

/**
 * Represents a (running) docker container with a Chrome browser
 * created by [ChromeDockerSupport]
 */
class ChromeDockerContainerSupport(
  dockerClient: DockerClient,
  containerId: ContainerId,
  containerName: ContainerName,
  val remoteDebuggingPort: Port,
  networkName: NetworkName = NetworkName.Bridge,
) : DockerContainerSupport(dockerClient, containerId, containerName, networkName) {


  @Deprecated("Use getExternalUrl() instead: Does sometimes work locally, depending on local configuration. Does work in CI, because the code is executed in gitlab-ci-runner container, which is in the bridge network.")
  fun getInternalUrl(): Url.Absolute {
    return Url.http(getContainerHostAddress(), ChromeDockerSupport.ExternalPort)
  }

  /**
   * Creates the URL that can be used to access via the container network. Uses external/remote port.
   * Connection via internal port does not work for ChromeDockerContainer.
   * Works in nearly all cases while getInternalUrl() fails.
   */
  fun getExternalUrl(): Url.Absolute {
    return Url.http(getDockerHostAddress(), remoteDebuggingPort)
  }
}
