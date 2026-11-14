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

/**
 * The outcome of checking an image upload: the bytes with their content type, or the reason it was refused.
 *
 * Nothing is stored here. The caller stores [Accepted.dataBytes] in the file store and persists what the store answered.
 */
sealed interface ImageUpload {
  class Accepted(
    /**
     * The uploaded file
     */
    val dataBytes: ByteArray,
    /**
     * The content type of the upload, one of [AcceptedContentTypes]
     */
    val contentType: ImageContentType,
  ) : ImageUpload {
    val size: @B @Positive Long
      get() = dataBytes.size.toLong()
  }

  data class Rejected(val rejection: FileUploadRejection) : ImageUpload

  companion object {
    /**
     * The content types an image upload accepts
     */
    val AcceptedContentTypes: List<ImageContentType> = ImageContentType.entries

    /**
     * Checks the [contentType] and the size of [dataBytes] of an upload.
     *
     * [contentType] is the media type without parameters, e.g. `image/png`. A refusal for the content type names
     * [AcceptedContentTypes] joined by `, ` as the expected value.
     */
    fun check(contentType: String, dataBytes: ByteArray, maximumSize: @B @Positive Long): ImageUpload {
      val normalizedContentType = ImageContentType.fromValueOrNull(contentType.lowercase())
      if (normalizedContentType == null) {
        return Rejected(FileUploadRejection.WrongContentType(actual = contentType, expected = AcceptedContentTypes.joinToString(", ")))
      }

      if (dataBytes.size > maximumSize) {
        return Rejected(FileUploadRejection.TooLarge(size = dataBytes.size.toLong(), maximum = maximumSize))
      }

      return Accepted(dataBytes, normalizedContentType)
    }
  }
}
