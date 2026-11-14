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
package it.neckar.rest.meta

import it.neckar.open.version.ImageDigestText
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Digest of the image a container runs — `sha256:` followed by 64 hex characters. It changes exactly
 * when the content of the image changes, so it identifies a deployed artifact.
 */
@Serializable
@JvmInline
value class ImageDigest(val value: String) {
  init {
    ImageDigestText.requireWellFormed(value)
  }

  /**
   * [ImageDigestText.shortOf] of this digest, for display.
   */
  val short: String
    get() = ImageDigestText.shortOf(value)

  override fun toString(): String = value

  companion object {
    /**
     * Returns the digest, or null when [value] is not a well-formed digest.
     */
    fun parseOrNull(value: String): ImageDigest? {
      return if (ImageDigestText.isWellFormed(value)) ImageDigest(value) else null
    }
  }
}
