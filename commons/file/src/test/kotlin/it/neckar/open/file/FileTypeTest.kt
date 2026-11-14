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
package it.neckar.open.file

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFailsWith

/**
 *
 */
class FileTypeTest {
  @Test
  fun testInva() {
    assertFailsWith<IllegalArgumentException> {
      FileType("asdf", "asdf", true)
    }
  }

  @Test
  fun testFileName() {
    val f = File("asdf.jpg")
    assertEquals("asdf", FileTypeRegistry.JPEG.getFileName(f).baseName.name)
  }

  @Test
  fun testContentType() {
    assertEquals("image/jpeg", FileTypeRegistry.JPEG.contentType)
    assertEquals("image/xcf", FileTypeRegistry.GIMP.contentType)
    assertEquals("image/psd", FileTypeRegistry.PHOTO_SHOP.contentType)
    assertEquals("image/cr2", FileTypeRegistry.RAW_CANON.contentType)
    assertEquals("application/lightzone", FileTypeRegistry.LIGHT_ZONE.contentType)
  }

  @Test
  fun testDefaultExtension() {
    assertEquals("jpg", FileTypeRegistry.JPEG.defaultExtension.extension)
    assertEquals(".jpg", FileTypeRegistry.JPEG.defaultExtension.combined)
    Assertions.assertTrue(FileTypeRegistry.JPEG.isDefaultExtension(FileTypeRegistry.JPEG.defaultExtension))
  }

  @Test
  fun testGetFileName() {
    assertEquals("asdf", FileTypeRegistry.JPEG.getFileName("asdf.jpg").baseName.name)
    assertEquals("jpg", FileTypeRegistry.JPEG.getFileName("asdf.jpg").extension.extension)
    assertEquals(".", FileTypeRegistry.JPEG.getFileName("asdf.jpg").extension.delimiter)
    assertEquals("asdf_", FileTypeRegistry.JPEG.getBaseName("asdf_.jpg"))
    assertEquals("asdf_", FileTypeRegistry.JPEG.getBaseName("asdf_.jpeg"))
    assertEquals("asdf_", FileTypeRegistry.JPEG.getBaseName("asdf_.JPEG"))
    assertEquals("Asdf_", FileTypeRegistry.JPEG.getBaseName("Asdf_.JPEG"))
    assertEquals("asdf", FileTypeRegistry.LIGHT_ZONE.getBaseName("asdf_lzn.jpg"))
    assertEquals("asdf_", FileTypeRegistry.LIGHT_ZONE.getBaseName("asdf__lzn.jpg"))
    assertEquals("asdf_", FileTypeRegistry.LIGHT_ZONE.getBaseName("asdf__LZN.JPG"))
    assertEquals("_", FileTypeRegistry.JPEG.getBaseName("_.jpeg"))
    assertEquals("", FileTypeRegistry.JPEG.getBaseName(".jpeg"))
    assertFailsWith<IllegalArgumentException> {
      FileTypeRegistry.JPEG.getBaseName("jpeg")
    }
  }

  @Test
  fun testGetBase() {
    assertEquals("asdf", FileTypeRegistry.JPEG.getBaseName("asdf.jpg"))
    assertEquals("asdf_", FileTypeRegistry.JPEG.getBaseName("asdf_.jpg"))
    assertEquals("asdf_", FileTypeRegistry.JPEG.getBaseName("asdf_.jpeg"))
    assertEquals("asdf_", FileTypeRegistry.JPEG.getBaseName("asdf_.JPEG"))
    assertEquals("Asdf_", FileTypeRegistry.JPEG.getBaseName("Asdf_.JPEG"))
    assertEquals("asdf", FileTypeRegistry.LIGHT_ZONE.getBaseName("asdf_lzn.jpg"))
    assertEquals("asdf_", FileTypeRegistry.LIGHT_ZONE.getBaseName("asdf__lzn.jpg"))
    assertEquals("asdf_", FileTypeRegistry.LIGHT_ZONE.getBaseName("asdf__LZN.JPG"))
  }

  @Test
  fun testGetExtension() {
    assertEquals("jpg", FileTypeRegistry.JPEG.getExtension("asdf.jpg").extension)
    assertEquals("jpg", FileTypeRegistry.JPEG.getExtension("asdf_.jpg").extension)
    assertEquals("jpeg", FileTypeRegistry.JPEG.getExtension("asdf_.jpeg").extension)
    assertEquals("JPEG", FileTypeRegistry.JPEG.getExtension("asdf_.JPEG").extension)
    assertEquals("JPEG", FileTypeRegistry.JPEG.getExtension("Asdf_.JPEG").extension)
    assertEquals(2, FileTypeRegistry.JPEG.extensions.size.toLong())
    assertEquals(".jpg", FileTypeRegistry.JPEG.defaultExtension.combined)
    assertEquals("lzn.jpg", FileTypeRegistry.LIGHT_ZONE.getExtension("asdf_lzn.jpg").extension)
    assertEquals("lzn.jpg", FileTypeRegistry.LIGHT_ZONE.getExtension("asdf__lzn.jpg").extension)
    assertEquals("LZN.JPG", FileTypeRegistry.LIGHT_ZONE.getExtension("asdf__LZN.JPG").extension)
    assertEquals(1, FileTypeRegistry.LIGHT_ZONE.extensions.size.toLong())
  }

  @Test
  fun testGetDelimiter() {
    assertEquals(".", FileTypeRegistry.JPEG.getExtension("asdf.jpg").delimiter)
    assertEquals(".", FileTypeRegistry.JPEG.getExtension("asdf_.jpg").delimiter)
    assertEquals(".", FileTypeRegistry.JPEG.getExtension("asdf_.jpeg").delimiter)
    assertEquals(".", FileTypeRegistry.JPEG.getExtension("asdf_.JPEG").delimiter)
    assertEquals(".", FileTypeRegistry.JPEG.getExtension("Asdf_.JPEG").delimiter)
    assertEquals("_", FileTypeRegistry.LIGHT_ZONE.getExtension("asdf_lzn.jpg").delimiter)
    assertEquals("_", FileTypeRegistry.LIGHT_ZONE.getExtension("asdf__lzn.jpg").delimiter)
    assertEquals("_", FileTypeRegistry.LIGHT_ZONE.getExtension("asdf__LZN.JPG").delimiter)
  }

  @Test
  fun testDependentFiles() {
    Assertions.assertFalse(FileTypeRegistry.GIMP.isDependentType)
    Assertions.assertFalse(FileTypeRegistry.JPEG.isDependentType)
    Assertions.assertTrue(FileTypeRegistry.LIGHT_ZONE.isDependentType)
  }
}
