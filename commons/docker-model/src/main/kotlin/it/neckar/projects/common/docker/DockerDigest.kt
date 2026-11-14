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

import kotlinx.serialization.Serializable

/**
 * The content digest of a docker manifest or of an image config (the image id) — `sha256:` followed by 64 hex characters.
 *
 * Unlike a [DockerTag], which is a label that can be moved to other content at any time, a digest
 * identifies exactly one piece of content. That is what makes a digest-pinned base image reproducible.
 */
@Serializable
@JvmInline
value class DockerDigest(val value: String) {
  init {
    require(Pattern.matches(value)) { "Docker digest must be sha256:<64 hex characters> but was <$value>" }
  }

  override fun toString(): String {
    return value
  }

  companion object {
    /** The only digest algorithm the registries in use produce. */
    private val Pattern: Regex = Regex("""^sha256:[a-f0-9]{64}$""")

    /** Returns the digest, or null when [value] is not a well-formed digest. */
    fun parseOrNull(value: String): DockerDigest? {
      return if (Pattern.matches(value)) DockerDigest(value) else null
    }
  }
}
