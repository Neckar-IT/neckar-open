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
package it.neckar.ktor.upload

import it.neckar.open.unit.number.Positive
import it.neckar.open.unit.other.B
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Why an upload was refused before anything was stored.
 */
@Serializable
sealed interface FileUploadRejection {
  /**
   * The upload carries another content type than the one the field accepts
   */
  @Serializable
  @SerialName("WrongContentType")
  data class WrongContentType(
    /**
     * The content type the upload declares
     */
    val actual: String,
    /**
     * The content type the field accepts
     */
    val expected: String,
  ) : FileUploadRejection

  /**
   * The file is larger than the field accepts
   */
  @Serializable
  @SerialName("TooLarge")
  data class TooLarge(
    /**
     * The size of the file
     */
    val size: @B @Positive Long,
    /**
     * The largest size the field accepts
     */
    val maximum: @B @Positive Long,
  ) : FileUploadRejection

  /**
   * The file cannot be opened or rendered as a document: it is damaged, protected by a password or has no pages
   */
  @Serializable
  @SerialName("Unreadable")
  data object Unreadable : FileUploadRejection
}
