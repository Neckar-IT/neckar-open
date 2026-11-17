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
 * Describes a docker image.
 */
interface DockerImageDescriptor : DockerImageDescriptorWithoutTag {

  /**
   * The tag of the image - if there is one.
   * Represents the last part of the image name
   */
  val tag: DockerTag

  /**
   * The repository and tag (without the registry)
   */
  val repositoryTag: String
    get() = "$repository:$tag"

  /**
   * The complete fq name - including the tag
   */
  val fqName: String
    get() = "$registry/$repositoryTag"

  companion object {
    /**
     * Creates a new descriptor for the "default" registry
     */
    fun default(nameComponent: DockerRepository, tag: DockerTag): DockerImageDescriptor {
      return DefaultDockerImageDescriptor(
        registry = DockerRegistry.DockerHub,
        repository = nameComponent,
        tag = tag,
      )
    }

    /**
     * Creates a new descriptor for the "Neckar IT" registry
     */
    fun neckarIt(repository: DockerRepository, tag: DockerTag): DockerImageDescriptor {
      return DefaultDockerImageDescriptor(
        registry = DockerRegistry.NeckarIT,
        repository = repository,
        tag = tag,
      )
    }

    /**
     * Creates a new descriptor for the given registry.
     *
     * This is a helper method that allows instantiation using simple strings
     */
    fun fromString(registry: String, repository: String, tag: String): DockerImageDescriptor {
      return DefaultDockerImageDescriptor(
        registry = DockerRegistry(registry),
        repository = DockerRepository(repository),
        tag = DockerTag(tag),
      )
    }

    fun create(registry: DockerRegistry, repository: DockerRepository, tag: DockerTag): DockerImageDescriptor {
      return DefaultDockerImageDescriptor(
        registry = registry,
        repository = repository,
        tag = tag,
      )
    }

    /**
     * Parses the given descriptor as string.
     *
     * The string must be in the format "registry/repository:tag"
     */
    fun parse(descriptorAsString: String): DefaultDockerImageDescriptor {
      val registryPart = descriptorAsString.substringBefore("/")
      val repositoryPart = descriptorAsString.substringAfter("/").substringBefore(":")
      val tagPart = descriptorAsString.substringAfter(":")

      val registry = DockerRegistry(registryPart)
      val repository = DockerRepository(repositoryPart)
      val tag = DockerTag(tagPart)

      return DefaultDockerImageDescriptor(
        registry = registry,
        repository = repository,
        tag = tag,
      )
    }
  }

  data class DefaultDockerImageDescriptor(
    override val registry: DockerRegistry,
    override val repository: DockerRepository,
    override val tag: DockerTag,
  ) : DockerImageDescriptor {

    override fun withTag(tag: DockerTag): DockerImageDescriptor {
      return copy(tag = tag)
    }

    override fun toString(): String {
      return fqName
    }
  }
}

