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

import assertk.*
import assertk.assertions.*
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageCmd
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import it.neckar.docker.ContainerName
import it.neckar.docker.ContainerState
import it.neckar.projects.common.docker.DockerImageDescriptor
import it.neckar.projects.common.docker.DockerRepository
import it.neckar.projects.common.docker.DockerTag
import it.neckar.docker.createDockerClient
import it.neckar.open.collections.fastForEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 *
 */
class ChromeDockerSupportTest {
  @Disabled
  @Test
  fun testDockerSupport() {
    val dockerClient: DockerClient = createDockerClient()

    val chromeDockerSupport = ChromeDockerSupport.create(dockerClient, DockerImageDescriptor.default(DockerRepository("chromedp/headless-shell"), DockerTag("latest")))
    chromeDockerSupport.pullDockerImage()

    chromeDockerSupport.createDockerContainerSupport(name = ContainerName("demo-chrome-docker-container"), networkName = null).use { dockerContainer ->
      assertThat(dockerContainer.getStatus()).isEqualTo(ContainerState.Created)

      dockerContainer.start()

      assertThat(dockerContainer.getStatus()).isEqualTo(ContainerState.Running)


      val ports = dockerContainer.getPorts()
      println("ports: $ports")
      Thread.sleep(2000_000)
    }
  }

  @Disabled
  @Test
  fun testApiManual() {
    //Start the docker container
    val dockerClient: DockerClient = createDockerClient()

    val pullImageCmd: PullImageCmd = dockerClient.pullImageCmd("selenium/standalone-chrome:latest")

    println("Before exec ########################################")

    pullImageCmd.start().also {
      it.awaitCompletion()
      println("Image pulled")
    }

    println("########################################")


    val createContainerResponse = dockerClient.createContainerCmd("selenium/standalone-chrome:latest")
      .withHostConfig(HostConfig().withPortBindings(PortBinding.parse("9222:9222")))
      .exec()

    if (createContainerResponse.warnings.isNotEmpty()) {
      println("----------------- WARNINGS -----------------")
      createContainerResponse.warnings.fastForEach { warning ->
        println("Warning: $warning")
      }
    }

    println("Container created ${createContainerResponse.id}")

    dockerClient.startContainerCmd(createContainerResponse.id).exec()

    println("Container started")

    Thread.sleep(4_000)

    println("Stopping Container")
    dockerClient.stopContainerCmd(createContainerResponse.id).exec()
    println("Stopped container")

    //Thread.sleep(10_000)

    println("Removing container")
    dockerClient.removeContainerCmd(createContainerResponse.id).exec()
    println("Removed container")

  }
}
