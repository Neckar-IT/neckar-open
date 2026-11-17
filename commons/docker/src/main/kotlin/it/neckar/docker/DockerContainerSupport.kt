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

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.Ports
import it.neckar.docker.network.NetworkName
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.dispose.Disposable
import it.neckar.open.dispose.DisposeSupport
import it.neckar.open.dispose.OnDispose
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Base class for a single, already created docker container.
 *
 * Both [close] and [dispose] *remove* the container, they do not merely stop it.
 */
open class DockerContainerSupport(
  val dockerClient: DockerClient,
  val containerId: ContainerId,
  /**
   * The name of the container
   */
  val containerName: ContainerName,

  /**
   * The network the container joined. Read by [getDockerHostAddress] only, and it has to match reality:
   * for a container that joined a different network that call throws.
   */
  val networkName: NetworkName = NetworkName.Bridge,

  ) : AutoCloseable, Disposable, OnDispose {

  private val logger: Logger = LoggerFactory.getLogger("it.neckar.docker.DockerContainerSupport[$containerName]")

  private val disposeSupport = DisposeSupport().also {
    it.onDispose { close() }
  }

  fun start() {
    dockerClient.startContainerCmd(containerId.value).exec()
    logger.info("starting container <${containerId.value}>")
  }

  fun stop() {
    dockerClient.stopContainerCmd(containerId.value).exec()
    logger.info("stopped container <${containerId.value}>")
  }

  override fun onDispose(action: () -> Unit) {
    disposeSupport.onDispose(action)
  }

  /**
   * Runs every action registered via [onDispose]. [close] is registered first, so disposing removes the container.
   */
  override fun dispose() {
    disposeSupport.dispose()
  }

  /**
   * Stops and *removes* the container - it no longer exists afterwards.
   */
  override fun close() {
    try {
      dockerClient.stopContainerCmd(containerId.value).exec()
    } catch (_: NotModifiedException) {
      //Docker answers 304 for a container that is already stopped. Nothing to do - it still has to be removed.
    }
    dockerClient.removeContainerCmd(containerId.value).exec()
  }

  /**
   * Logs the *running* containers of the whole docker daemon, not just this one. Works in CI and locally.
   */
  fun logContainers() {
    val exec: MutableList<Container> = dockerClient.listContainersCmd().exec()
    logger.info("---------- All containers ------------------")
    exec.forEach { container: Container ->
        logger.info("container image: <${container.image}>, id: <${container.id}>, name: <${container.names.first()}>, state: <${container.state}>, status: <${container.status}>, ports: ")
        container.ports.forEach {
          logger.info("       ip: ${it.ip}, private port: ${it.privatePort}, public port: ${it.publicPort}")
        }
        logger.info("         host config: ${container.hostConfig.toString()}")
        logger.info("         network settings: ${container.networkSettings.toString()}")
      }
    logger.info("---------- ########## ------------------")

  }


  /**
   * Returns the current state of the container.
   *
   * Throws when docker reports no status at all, or one [ContainerState] does not know.
   */
  fun getStatus(): ContainerState {
    val exec = inspect()
    val state: InspectContainerResponse.ContainerState = exec.state

    return state.status?.let { ContainerState.parse(it) } ?: throw IllegalStateException("No state info found")
  }

  /**
   * Suspends until the container reports [ContainerState.Running], throws after at least ten seconds of waiting.
   *
   * Running is not ready: no health check is involved, so a server inside the container may still be booting.
   */
  suspend fun waitForContainerToBeRunning() {
    logger.info("waitForContainerToBeRunning <${containerName}>")

    //wait at most 10 seconds
    var repeatCounter = 0

    while (getStatus() != ContainerState.Running) {
      logger.info("Waiting for container to be running...")
      delay(100.milliseconds)
      repeatCounter++

      if (repeatCounter > 100) {
        throw IllegalStateException("Container did not start")
      }
    }
    logger.info("Container <${containerName}> has started!")
  }

  /**
   * Returns the port bindings docker reports for this container - `null` when the inspect response
   * carries no network settings at all.
   */
  fun getPorts(): Ports? {
    val exec = inspect()
    return exec.networkSettings?.ports
  }

  /**
   * Returns the container's own IP address inside its docker network.
   *
   * Routable only from within that network: in CI this code runs in the gitlab-ci-runner container and reaches it,
   * from a workstation it does not. [getDockerHostAddress] plus a published port works in both.
   */
  fun getContainerHostAddress(): String {
    val exec = inspect()
    return exec.networkSettings.ipAddress
  }

  /**
   * Returns the gateway of [networkName] - the docker host as seen from inside the container.
   * Usually `172.17.0.1` for the default `bridge` network.
   *
   * Combined with a published port this reaches the container from CI and from a workstation alike.
   * Throws when the container did not join [networkName].
   */
  fun getDockerHostAddress(): String {
    val exec = inspect()
    val bridgeGateWay = exec.networkSettings.networks[networkName.value]?.gateway
    requireNotNull(bridgeGateWay) { "Docker network [${networkName.value}] did not expose a gateway in inspect output" }

    return bridgeGateWay
  }

  /**
   * Inspects the container and returns the result
   */
  fun inspect(): InspectContainerResponse {
    return dockerClient.inspectContainerCmd(containerId.value)
      .exec()
  }
}
