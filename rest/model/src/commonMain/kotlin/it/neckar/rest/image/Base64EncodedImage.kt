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
package it.neckar.rest.image

import io.ktor.util.*
import it.neckar.open.kotlin.lang.substringBetween
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlin.jvm.JvmInline

/**
 * Represents an image encoded in base64
 */
@Serializable(with = Base64EncodedImageSerializer::class)
data class Base64EncodedImage(
  /**
   * The image type
   */
  val imageType: ImageType,
  /**
   * The base 64 encoded image data
   */
  val data: String,
) {
  /**
   * Returns the decoded byte array
   */
  fun toByteArray(): ByteArray {
    return data.decodeBase64Bytes()
  }

  /**
   * Returns the image as a data URL
   */
  fun toImgSrc(): String {
    return "data:image/${imageType.extension};base64,$data"
  }

  companion object {
    operator fun invoke(imageType: ImageType, data: ByteArray): Base64EncodedImage {
      return Base64EncodedImage(imageType, data.encodeBase64())
    }
  }

  /**
   * The image type
   */
  @JvmInline
  value class ImageType(val extension: String) {
    companion object {
      val PNG: ImageType = ImageType("png")
      val JPEG: ImageType = ImageType("jpeg")
      val SVG: ImageType = ImageType("svg+xml")
    }
  }
}

@Serializer(forClass = Base64EncodedImage::class)
object Base64EncodedImageSerializer : KSerializer<Base64EncodedImage> {
  override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Base64EncodedImage) {
    encoder.encodeString(value.toImgSrc())
  }

  override fun deserialize(decoder: Decoder): Base64EncodedImage {
    val imgSrc = decoder.decodeString()
    val data = imgSrc.substringAfterLast(',')

    val imageTypeAsString = imgSrc.substringBetween("/", ";")
    val imageType = Base64EncodedImage.ImageType(imageTypeAsString)

    return Base64EncodedImage(imageType, data)
  }
}
