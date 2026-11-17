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
package it.neckar.runtime.context

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * An absolute path on a deploy target: `/srv/mea.neckar.it`.
 *
 * Deploy commands pass it to a remote shell unquoted, so it holds no whitespace and no quote. Only the normal
 * spelling is accepted, so one path never makes two values and a prefix check cannot be escaped by `..`.
 */
@JvmInline
@Serializable
value class HostPath(val value: String) {
  init {
    require(SegmentsRegex.matches(value)) {
      "HostPath [$value] must be an absolute path of segments made of letters, digits, '.', '_', '-', '@' and '+', without a trailing '/'"
    }
    require(value.split('/').none { it == "." || it == ".." }) { "HostPath [$value] must not contain '.' or '..' segments" }
  }

  override fun toString(): String {
    return value
  }

  private companion object {
    /** One or more non-empty segments, or the root itself. */
    val SegmentsRegex: Regex = Regex("""/|(/[A-Za-z0-9._@+-]+)+""")
  }
}
