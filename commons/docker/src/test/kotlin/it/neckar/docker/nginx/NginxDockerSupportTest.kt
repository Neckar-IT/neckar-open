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

import assertk.*
import assertk.assertions.*
import com.github.dockerjava.api.DockerClient
import it.neckar.docker.ContainerName
import it.neckar.docker.ContainerState
import it.neckar.docker.createDockerClient
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.projects.common.docker.DockerImageDescriptor
import it.neckar.projects.common.docker.DockerRepository
import it.neckar.projects.common.docker.DockerTag
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class NginxDockerSupportTest {
  @Disabled
  @Test
  fun testDockerSupport() {
    val dockerClient: DockerClient = createDockerClient()

    val nginxDockerSupport = NginxDockerSupport.create(dockerClient, DockerImageDescriptor.default(DockerRepository("library/nginx"), DockerTag("1.31.8-alpine")))
    if (true) {
      nginxDockerSupport.pullDockerImage()
    }

    val contentRoot = File("/tmp")

    nginxDockerSupport.createDockerContainer(name = ContainerName("demo-nginx-docker-container"), contentRoot.absolutePath).use { dockerContainer ->
      assertThat(dockerContainer.getStatus()).isEqualTo(ContainerState.Created)

      dockerContainer.start()

      assertThat(dockerContainer.getStatus()).isEqualTo(ContainerState.Running)


      val ports = dockerContainer.getPorts().requireNotNull()
      println("ports: $ports")

      println("Port Bindings:")
      ports.bindings.forEach {
        println("Exposed: ${it.key} : ${it.value.joinToString()}")
      }

      Thread.sleep(1_000)
      println("#################")

      println("External URL: ${dockerContainer.getExternalUrl()}")
      println("Internal URL: ${dockerContainer.getInternalUrl()}")

      Thread.sleep(2000_000)
    }
  }
}
