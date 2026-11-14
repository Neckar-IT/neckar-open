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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The media type of an image in lower case without parameters, e.g. `image/png`.
 *
 * An enum, so that the OpenAPI schema lists the values and the frontend takes the accepted types from the generated client.
 */
@Serializable
enum class ImageContentType(
  /**
   * The media type, e.g. `image/jpeg`
   */
  val value: String,
) {
  @SerialName("image/jpeg")
  Jpeg("image/jpeg"),

  @SerialName("image/png")
  Png("image/png"),

  @SerialName("image/webp")
  Webp("image/webp"),
  ;

  /**
   * The extension of a file of this type without the dot, e.g. `jpg` for `image/jpeg`, `png` for `image/png` and `webp` for `image/webp`
   */
  val fileExtension: String
    get() = value.substringAfter('/').replace("jpeg", "jpg")

  override fun toString(): String {
    return value
  }

  companion object {
    /**
     * The type with the media type [value], or `null` for any other
     */
    fun fromValueOrNull(value: String): ImageContentType? {
      return entries.firstOrNull { it.value == value }
    }

    /**
     * The type whose [fileExtension] or media subtype is [extension], case-insensitive: `jpg` and `jpeg` both name [Jpeg]; `null` for any other.
     */
    fun fromFileExtensionOrNull(extension: String): ImageContentType? {
      val lowercase: String = extension.lowercase()
      return entries.firstOrNull { it.fileExtension == lowercase || it.value == "image/$lowercase" }
    }
  }
}
