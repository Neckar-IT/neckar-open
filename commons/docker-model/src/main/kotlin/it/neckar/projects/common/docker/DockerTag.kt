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
 * Represents a docker tag (e.g., `latest`, `1.0.0`, ...)
 */
@Serializable
@JvmInline
value class DockerTag(val value: String) : Comparable<DockerTag> {
  init {
    require(value.isNotBlank()) { "Docker tag must not be blank" }
    require(value.length <= 128) { "Docker tag must not be longer than 128 characters but was <${value.length}>" }
    require(value.contains(' ').not()) { "Docker tag must not contain spaces but was <$value>" }
  }

  override fun toString(): String {
    return value
  }

  override fun compareTo(other: DockerTag): Int {
    return this.value.compareTo(other.value)
  }

  companion object {
    val latest: DockerTag = DockerTag("latest")

    /** The tag grammar of the distribution spec; a `${…}` placeholder is outside it. */
    private val Grammar: Regex = Regex("""^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$""")

    /** Null for a text outside the tag grammar. */
    fun parseOrNull(value: String): DockerTag? {
      return if (Grammar.matches(value)) DockerTag(value) else null
    }
  }
}
