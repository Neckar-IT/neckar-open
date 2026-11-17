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
package it.neckar.open.http

import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.lang.fromBase64
import it.neckar.open.kotlin.lang.toBase64
import org.junit.jupiter.api.Test

/**
 *
 */
class UrlDataSchemeTest {
  @Test
  fun testImage() {
    val url = Url.image("R0lGODlhAQABAIAAAP///////yH5BAEAAAAALAAAAAABAAEAAAIBRAA7".fromBase64(), "gif")
    assertThat(url.value).isEqualTo("data:image/gif;base64,R0lGODlhAQABAIAAAP///////yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")

    assertThat(url.mediaType).isEqualTo("image/gif")
    assertThat(url.data).isEqualTo("R0lGODlhAQABAIAAAP///////yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
    assertThat(url.dataBytes.toBase64()).isEqualTo("R0lGODlhAQABAIAAAP///////yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")

    assertThat(url.isImage()).isTrue()
    assertThat(url.isImage("gif")).isTrue()
    assertThat(url.isImage("png")).isFalse()
  }

  @Test
  fun testLoadImage() {
    val git = Url.image("R0lGODlhAQABAIAAAP///////yH5BAEAAAAALAAAAAABAAEAAAIBRAA7".fromBase64(), "gif")
    assertThat(git.loadImage()).isNotNull()
    val png = Url.data("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==")
    assertThat(png.loadImage()).isNotNull()

    assertThat(png.mediaType).isEqualTo("image/png")
    assertThat(png.isImage()).isTrue()
    assertThat(png.isImage("png")).isTrue()
    assertThat(png.isImage("jpg")).isFalse()
  }
}

