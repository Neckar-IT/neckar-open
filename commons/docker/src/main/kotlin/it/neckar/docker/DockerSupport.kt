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
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import it.neckar.docker.network.NetworkName
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.annotations.Blocking
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.collections.fastForEach
import it.neckar.open.dispose.Disposable
import it.neckar.projects.common.docker.DockerImageDescriptor
import it.neckar.projects.common.docker.PinnedDockerImageDescriptor
import it.neckar.runtime.context.guessInCIEnvironment

/**
 * Base class for docker support.
 *
 * A docker support creates/starts/stops docker containers
 */
open class DockerSupport(
  /**
   * The docker client to use.
   */
  val dockerClient: DockerClient,

  /**
   * The image to use
   */
  val image: DockerImageDescriptor,
) : Disposable {
  /**
   * Pulls [image]; returns when the pull has completed. An image the registry lacks fails naming its reference, an own base image also
   * the job that pushes it.
   */
  @Blocking
  fun pullDockerImage() {
    // A pinned image pulls its digest, the manifest [createContainer] starts.
    val reference: String = when (val descriptor = image) {
      is PinnedDockerImageDescriptor -> descriptor.digest.value
      else -> image.tag.value
    }
    try {
      dockerClient.pullImageCmd(image.registryRepository)
        .withTag(reference)
        .start()
        .awaitCompletion()
    } catch (e: NotFoundException) {
      throw IllegalStateException(missingImageMessage(image), e)
    }
  }

  /**
   * Removes existing docker containers.
   */
  fun cleanupOldContainers(name: ContainerName) {
    val containers = dockerClient.listContainersCmd()
      .withShowAll(true)
      .withNameFilter(listOf(name.value))
      .exec()

    containers.fastForEach { container ->
      logger.info("Removing container ${container.id}")
      dockerClient.removeContainerCmd(container.id)
        .withForce(true)
        .exec()
    }
  }


  /**
   * Creates a new container - that is *not* running.
   *
   * The [shmSize] configures the shared memory size in bytes.
   * `null` uses Docker's default (64 MB).
   */
  protected fun createContainer(
    name: ContainerName,
    binds: List<Bind>,
    portBindings: List<PortBinding>,
    networkName: NetworkName?,
    shmSize: Long? = null,
  ): ContainerId {
    val hostConfig = HostConfig()
      .withBinds(binds)
      .withPortBindings(portBindings)
      .also {
        if (networkName != null) {
          it.withNetworkMode(networkName.value)
        }
        if (shmSize != null) {
          it.withShmSize(shmSize)
        }
      }

    val idAsString = dockerClient.createContainerCmd(image.fqName)
      .withName(name.value)
      .withHostConfig(
        hostConfig
      )
      .exec()
      .id

    return ContainerId(idAsString)
  }

  override fun dispose() {
    dockerClient.close()
  }

  /**
   * Retrieves the source path of the /builds volume mount from host to the gitlab-runner container.
   * Only use this in CI!
   *
   * The [targetPath] is the target path of the original mount on the current container.
   * Returns the source path of the mount with [targetPath] as destination.
   */
  fun getRunnerContainerBindSourcePath(targetPath: String): String {
    val containerId = retrieveIdOfCurrentContainer()

    val runnerInspect = dockerClient.inspectContainerCmd(containerId).exec()

    val mounts = runnerInspect.mounts.requireNotNull { "The mounts inspect of the container with id: <$containerId> is null" }
    require(mounts.isNotEmpty()) { "The list of mounts of the container with id: <$containerId> is empty" }

    mounts.forEach {
      if (it.destination?.path?.contains(targetPath) == true) {
        return it.source ?: throw IllegalStateException("mount source for targetPath: <$targetPath> is null!")
      }
    }
    throw IllegalStateException("Could not find the mount with target <$targetPath> of the container with id: <$containerId>")
  }


  /**
   * Retrieves the Id of the container on which this code is executed.
   */
  private fun retrieveIdOfCurrentContainer(): String {
    require(guessInCIEnvironment()) { "getRunnerContainerBindSourcePath() must only be used in CI!" }
    // mountinfo contains the container id in every docker container
    val mountInfoText = ProcessBuilder("cat", "/proc/self/mountinfo")
      .redirectErrorStream(true)
      .start()
      .inputStream.bufferedReader().readText()

    val containerId = mountInfoText.lineSequence()
      .find { it.contains("/var/lib/docker/containers") }
      ?.substringAfter("/var/lib/docker/containers/")
      ?.substringBefore("/")

    return containerId.requireNotNull { "The retrieved containerId of the gitlab-ci-runner container is null" }
  }


  companion object {
    /**
     * Creates a new instance with the default configuration
     */
    fun create(dockerImage: DockerImageDescriptor): DockerSupport {
      val dockerClient: DockerClient = createDockerClient()

      return DockerSupport(dockerClient, dockerImage)
    }

    private val logger: Logger = LoggerFactory.getLogger("it.neckar.docker.DockerSupport")

    private fun missingImageMessage(image: DockerImageDescriptor): String {
      return "Cannot pull ${image.fqName}: the registry holds no such image, or the login lacks pull access."
    }
  }
}
