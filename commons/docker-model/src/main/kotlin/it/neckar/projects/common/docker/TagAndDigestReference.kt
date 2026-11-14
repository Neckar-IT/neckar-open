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
 * An image reference of the form `<name>:<tag>@sha256:<digest>`, e.g. `docker.io/library/ubuntu:26.04@sha256:da6f…`.
 * The digest decides what a pull fetches; the tag names it for the reader.
 */
data class TagAndDigestReference(val name: String, val tag: DockerTag, val digest: DockerDigest) {
  override fun toString(): String {
    return "$name:$tag@$digest"
  }

  companion object {
    /** Null for anything but `<name>:<tag>@<digest>` with a tag of the Docker tag grammar, which excludes a `${…}` placeholder. */
    fun parseOrNull(reference: String): TagAndDigestReference? {
      val digest = DockerDigest.parseOrNull(reference.substringAfter('@', missingDelimiterValue = "")) ?: return null
      val nameWithTag = reference.substringBefore('@')
      val name = nameOf(reference)
      // A second colon in the last segment leaves one in the name, which no valid reference carries.
      if (name == nameWithTag || name.isEmpty() || ':' in name.substringAfterLast('/')) {
        return null
      }
      val tag = DockerTag.parseOrNull(nameWithTag.removePrefix("$name:")) ?: return null
      return TagAndDigestReference(name, tag, digest)
    }

    /** The image name of [reference], without digest and tag; a colon before the last slash separates a registry port, not a tag. */
    fun nameOf(reference: String): String {
      val nameWithTag = reference.substringBefore('@')
      return if (':' in nameWithTag.substringAfterLast('/')) nameWithTag.substringBeforeLast(':') else nameWithTag
    }
  }
}
