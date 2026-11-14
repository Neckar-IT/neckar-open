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
import io.ktor.http.*
import it.neckar.docker.ContainerId
import it.neckar.docker.ContainerName
import it.neckar.docker.DockerContainerSupport
import it.neckar.docker.network.NetworkName
import it.neckar.ktor.client.createHttpClient
import it.neckar.ktor.client.get
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.http.Url
import it.neckar.open.time.monotonicTimeSource
import it.neckar.projects.common.Port
import kotlinx.coroutines.*
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Represents a (running) docker container with a Nginx server.
 *
 * Can be started using [NginxDockerSupport.createDockerContainer].
 */
class NginxDockerContainerSupport(
  dockerClient: DockerClient,
  containerId: ContainerId,
  containerName: ContainerName,
  val remotePort: Port,
  networkName: NetworkName = NetworkName.Bridge,
) : DockerContainerSupport(dockerClient, containerId, containerName, networkName) {

  /**
   * Creates a URL that can be used to access the container directly.
   * Contains the internal port (80) nginx is listening to.
   * Does sometimes work locally, depending on local configuration.
   * Does work in CI, because the code is executed in gitlab-ci-runner container, which is in the bridge network.
   */
  @Deprecated("Use getExternalUrl() instead: Does not work locally, because the machine that executes this code (localhost) is not in the bridge network. Does work in CI, because the code is executed in gitlab-ci-runner container, which is in the bridge network.")
  fun getInternalUrl(): Url.Absolute {
    return Url.http(getContainerHostAddress(), NginxDockerSupport.InternalPort)
  }

  /**
   * Creates the URL that can be used to access via the container network. Uses external/remote port.
   * Works in CI and locally.
   */
  fun getExternalUrl(): Url.Absolute {
    return Url.http(getDockerHostAddress(), remotePort)
  }

  /**
   * Polls [getExternalUrl] until nginx answers with a success status. A refused connection counts as not ready yet:
   * the container runs before nginx listens. Bounded by real time, so a caller in virtual time cannot skip the wait.
   */
  suspend fun waitForNginxToBeReady() {
    waitForContainerToBeRunning()

    val url = getExternalUrl()
    logger.info("Waiting for nginx to be ready at <$url>")

    createHttpClient("nginxAvailabilityChecker").use { httpClient ->
      withContext(Dispatchers.IO) {
        val deadline = monotonicTimeSource.markNow() + ReadyTimeout
        var attempts = 0
        while (true) {
          attempts++
          val status = try {
            httpClient.get(url).status
          } catch (e: IOException) {
            logger.debug("nginx not reachable yet: ${e.message}")
            null
          }
          if (status?.isSuccess() == true) {
            logger.info("nginx is ready after $attempts attempt(s)")
            return@withContext
          }
          check(deadline.hasNotPassedNow()) { "nginx at <$url> answered no success status within $ReadyTimeout ($attempts attempts, last status: $status)" }
          delay(ReadyPollInterval)
        }
      }
    }
  }

  companion object {
    private val ReadyTimeout: Duration = 60.seconds

    private val ReadyPollInterval: Duration = 200.milliseconds

    private val logger: Logger = LoggerFactory.getLogger("com.meistercharts.tests.e2e.docker.nginx.NginxDockerContainerSupport")
  }
}
