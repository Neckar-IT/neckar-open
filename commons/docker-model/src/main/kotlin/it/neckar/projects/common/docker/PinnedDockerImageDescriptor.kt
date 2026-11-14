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
 * An image referenced by tag and digest. The digest decides what a host pulls; the tag names the release of a foreign image for the
 * reader and for Renovate, which bumps both together, and the input-addressed tag of an own base image.
 */
data class PinnedDockerImageDescriptor(
  override val registry: DockerRegistry,
  override val repository: DockerRepository,
  override val tag: DockerTag,
  val digest: DockerDigest,
) : DockerImageDescriptor {

  override val fqName: String
    get() = "$registry/$repositoryTag@$digest"

  /** Another tag names other content, so the digest does not carry over. */
  override fun withTag(tag: DockerTag): DockerImageDescriptor {
    return DockerImageDescriptor.create(registry, repository, tag)
  }

  override fun toString(): String {
    return fqName
  }

  companion object {
    /** [reference] `<registry>/<repository>:<tag>@<digest>` of [registry], e.g. `registry.git.neckar.it/neckarit/neckar-hub/static-site:in-5c1e…@sha256:3cef…`. */
    fun parse(registry: DockerRegistry, reference: String): PinnedDockerImageDescriptor {
      return requireNotNull(parseOrNull(registry, reference)) { "[$reference] is no <repository>:<tag>@<digest> of $registry" }
    }

    /** [parse], or null for a [reference] without tag and digest or of another registry than [registry]. */
    fun parseOrNull(registry: DockerRegistry, reference: String): PinnedDockerImageDescriptor? {
      val parsed = TagAndDigestReference.parseOrNull(reference) ?: return null
      if (parsed.name.startsWith("${registry.hostName}/").not()) {
        return null
      }
      val repository = registry.repositoryOf(parsed.name) ?: return null
      return PinnedDockerImageDescriptor(registry = registry, repository = repository, tag = parsed.tag, digest = parsed.digest)
    }
  }
}
