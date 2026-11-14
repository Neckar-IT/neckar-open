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

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

class ImageUploadTest {
  private val imageBytes: ByteArray = ByteArray(14)

  @Test
  fun testEveryAcceptedContentType() {
    for (contentType in ImageUpload.AcceptedContentTypes) {
      val accepted = ImageUpload.check(contentType.value, imageBytes, 100L) as ImageUpload.Accepted

      assertThat(accepted.contentType.value).isEqualTo(contentType.value)
      assertThat(accepted.size).isEqualTo(14L)
    }
  }

  @Test
  fun testContentTypeIsCaseInsensitive() {
    val accepted = ImageUpload.check("Image/JPEG", imageBytes, 100L) as ImageUpload.Accepted

    assertThat(accepted.contentType).isEqualTo(ImageContentType.Jpeg)
  }

  @Test
  fun testWrongContentType() {
    assertThat(ImageUpload.check("application/pdf", imageBytes, 100L))
      .isEqualTo(ImageUpload.Rejected(FileUploadRejection.WrongContentType(actual = "application/pdf", expected = "image/jpeg, image/png, image/webp")))
  }

  @Test
  fun testWebpIsAccepted() {
    val accepted = ImageUpload.check("image/webp", imageBytes, 100L) as ImageUpload.Accepted

    assertThat(accepted.contentType).isEqualTo(ImageContentType.Webp)
  }

  @Test
  fun testTooLarge() {
    assertThat(ImageUpload.check("image/png", imageBytes, 13L))
      .isEqualTo(ImageUpload.Rejected(FileUploadRejection.TooLarge(size = 14L, maximum = 13L)))
  }

  @Test
  fun testExactlyTheMaximumIsAccepted() {
    assertThat(ImageUpload.check("image/png", imageBytes, 14L)).isInstanceOf<ImageUpload.Accepted>()
  }
}
