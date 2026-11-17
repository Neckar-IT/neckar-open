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
package it.neckar.projects.common.docker

/**
 * Represents a docker registry
 */
data class DockerRegistry(
  /**
   * The host name of the docker registry
   */
  val hostName: String,
) {
  override fun toString(): String {
    return hostName
  }

  fun isDockerHub(): Boolean {
    return this == DockerHub
  }

  /**
   * Extracts the [DockerRepository] from a full [imageReference] that belongs to this registry.
   * Returns null when the reference points at a different registry.
   *
   * For [NeckarIT]:
   * - `registry.git.neckar.it/neckarit/neckar-hub/glossary.neckar.it:main` → `glossary.neckar.it`
   * - `registry.git.neckar.it/neckarit/neckar-hub/auto-merge.neckar.it/auto-merge-ui:20260711-3b399a4addcd` → `auto-merge.neckar.it/auto-merge-ui`
   */
  fun repositoryOf(imageReference: String): DockerRepository? {
    val repositoryWithTag = imageReference.substringAfter("$hostName/", missingDelimiterValue = "")
    if (repositoryWithTag.isEmpty()) return null
    val repository = repositoryWithTag.substringBefore('@').substringBefore(':')
    if (repository.isEmpty()) return null
    return DockerRepository(repository)
  }

  companion object {
    val DockerHub: DockerRegistry = DockerRegistry("docker.io")
    val NeckarIT: DockerRegistry = DockerRegistry("registry.git.neckar.it/neckarit/neckar-hub")

    /**
     * Repository managed by Red Hat
     */
    val Quay: DockerRegistry = DockerRegistry("quay.io")

    /**
     * "GitHub Container Registry"
     */
    val Ghcr: DockerRegistry = DockerRegistry("ghcr.io")
  }
}
