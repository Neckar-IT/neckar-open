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

/**
 *
 * FileTypeRegistry class.
 *
 */
object FileTypeRegistry {
  /**
   * Constant `LIGHT_ZONE`
   */
  @JvmStatic
  val LIGHT_ZONE: FileType = FileType("LightZone", "application/lightzone", true, Extension("lzn.jpg", "_"))

  /**
   * Constant `JPEG`
   */
  @JvmStatic
  val JPEG: FileType = FileType("JPEG", "image/jpeg", false, Extension("jpg", "."), Extension("jpeg", "."))

  /**
   * Constant `TIFF`
   */
  @JvmStatic
  val TIFF: FileType = FileType("TIFF", "image/tiff", false, Extension("tif", "."), Extension("tiff", "."))

  /**
   * Constant `GIMP`
   */
  @JvmStatic
  val GIMP: FileType = FileType("Gimp", "image/xcf", false, Extension("xcf", "."))

  /**
   * Constant `PHOTO_SHOP`
   */
  @JvmStatic
  val PHOTO_SHOP: FileType = FileType("Photoshop", "image/psd", false, Extension("psd", "."))

  /**
   * Constant `RAW_CANON`
   */
  @JvmStatic
  val RAW_CANON: FileType = FileType("CanonRaw", "image/cr2", false, Extension("cr2", "."))

  @JvmStatic
  val Default: List<FileType> = listOf(LIGHT_ZONE, JPEG, TIFF, GIMP, RAW_CANON, PHOTO_SHOP)
}
