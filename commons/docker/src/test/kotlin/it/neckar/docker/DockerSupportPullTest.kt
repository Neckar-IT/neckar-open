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

import assertk.assertFailure
import assertk.assertions.hasMessage
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import io.mockk.every
import io.mockk.mockk
import it.neckar.projects.common.docker.DockerImageDescriptor
import it.neckar.projects.common.docker.DockerRepository
import it.neckar.projects.common.docker.DockerTag
import org.junit.jupiter.api.Test

class DockerSupportPullTest {
  private val inputTag = DockerTag("in-${"c".repeat(64)}")

  @Test
  fun `a failed pull of a foreign image names its reference`() {
    val image = DockerImageDescriptor.default(DockerRepository("library/nginx"), DockerTag("1.31.6-alpine"))

    assertFailure { DockerSupport(clientWithoutImage(), image).pullDockerImage() }.hasMessage("Cannot pull ${image.fqName}: the registry holds no such image, or the login lacks pull access.")
  }

  private fun clientWithoutImage(): DockerClient {
    return mockk {
      every { pullImageCmd(any()) } returns mockk {
        every { withTag(any()) } returns this
        every { start() } returns mockk {
          // docker-java reports a failed pull asynchronously, out of awaitCompletion.
          every { awaitCompletion() } throws NotFoundException("manifest unknown")
        }
      }
    }
  }
}
