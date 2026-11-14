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
 * Does not have a tag
 */
interface DockerImageDescriptorWithoutTag {
  /**
   * The repository of the image
   */
  val registry: DockerRegistry

  /**
   * The name component of the image - without the tag and without the registry.
   * E.g., `keycloak/keycloak`
   */
  val repository: DockerRepository

  /**
   * The complete registry/repository (without the tag)
   */
  val registryRepository: String
    get() = "$registry/$repository"

  /**
   * Checks whether the image is an official Docker image
   * Official images are hosted under `library/` in DockerHub
   */
  val isOfficialImage: Boolean
    get() = registry.isDockerHub() && repository.isOfficialImageRepository()

  /**
   * Returns a (new?) descriptor with the given tag
   */
  fun withTag(tag: DockerTag): DockerImageDescriptor

  fun withTagFromString(tag: String): DockerImageDescriptor {
    return withTag(DockerTag(tag))
  }

  companion object {
    /**
     * Pareses the given descriptor string (e.g., `docker.io/grafana/grafana-oss`) and returns a new instance
     */
    fun parse(descriptorAsString: String): DefaultDockerImageDescriptorWithoutTag {
      val registryPart = descriptorAsString.substringBefore("/")
      val repositoryPart = descriptorAsString.substringAfter("/")

      val registry = DockerRegistry(registryPart)
      val repository = DockerRepository(repositoryPart)

      return DefaultDockerImageDescriptorWithoutTag(registry, repository)
    }

    /**
     * Creates a new descriptor for the "default" registry
     */
    fun default(repository: DockerRepository): DefaultDockerImageDescriptorWithoutTag {
      return DefaultDockerImageDescriptorWithoutTag(
        registry = DockerRegistry.DockerHub,
        repository = repository,
      )
    }

    fun default(repositoryAsString: String): DefaultDockerImageDescriptorWithoutTag {
      return default(DockerRepository(repositoryAsString))
    }

    fun neckarIt(repository: DockerRepository): DefaultDockerImageDescriptorWithoutTag {
      return DefaultDockerImageDescriptorWithoutTag(
        registry = DockerRegistry.NeckarIT,
        repository = repository,
      )
    }

    /**
     * Helper method to create a new instance from strings
     */
    fun fromStrings(registry: String, nameComponent: String): DefaultDockerImageDescriptorWithoutTag {
      return DefaultDockerImageDescriptorWithoutTag(
        registry = DockerRegistry(registry),
        repository = DockerRepository(nameComponent),
      )
    }

    fun create(registry: DockerRegistry, repository: DockerRepository): DefaultDockerImageDescriptorWithoutTag {
      return DefaultDockerImageDescriptorWithoutTag(
        registry = registry,
        repository = repository,
      )
    }

    fun create(registry: DockerRegistry, repositoryAsString: String): DefaultDockerImageDescriptorWithoutTag {
      return create(registry, DockerRepository(repositoryAsString))
    }
  }

  /**
   * Default class for [DockerImageDescriptorWithoutTag]
   */
  data class DefaultDockerImageDescriptorWithoutTag(
    override val registry: DockerRegistry,
    override val repository: DockerRepository,
  ) : DockerImageDescriptorWithoutTag {

    /**
     * Returns a new descriptor with the given tag
     */
    override fun withTag(tag: DockerTag): DockerImageDescriptor {
      return DockerImageDescriptor.DefaultDockerImageDescriptor(
        registry = registry,
        repository = repository,
        tag = tag,
      )
    }

    override fun toString(): String {
      return "$registry/$repository"
    }
  }
}
