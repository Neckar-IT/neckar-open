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


import java.io.File

/**
 * Represents a file name
 *
 */
data class FileName(
  /**
   *
   * Getter for the field `baseName`.
   *
   * @return a BaseName object.
   */
  val baseName: BaseName,
  /**
   *
   * Getter for the field `extension`.
   *
   * @return a Extension object.
   */
  val extension: Extension,
) {

  /**
   *
   * getName
   *
   * @return a String object.
   */
  val name: String
    get() = baseName.toString() + extension.combined

  /**
   * Creates a new file name
   *
   * @param baseName  the base name
   * @param extension the file extension
   */
  constructor(baseName: String, extension: String) : this(BaseName(baseName), Extension(extension = extension))

  /**
   * Creates a new file name
   *
   * @param baseName  the base name
   * @param extension the extension
   */
  constructor(baseName: String, extension: Extension) : this(BaseName(baseName), extension)

  /**
   *
   * Constructor for FileName.
   *
   * @param baseName  a String object.
   * @param delimiter a String object.
   * @param extension a String object.
   */
  constructor(baseName: String, delimiter: String, extension: String) : this(BaseName(baseName), Extension(extension, delimiter))

  /**
   * Creates a file name
   *
   * @param baseName  the base name
   * @param delimiter the delimiter
   * @param extension the extension
   */
  constructor(baseName: BaseName, delimiter: String, extension: String) : this(baseName, Extension(extension, delimiter))

  /**
   * {@inheritDoc}
   */
  override fun toString(): String {
    return baseName.toString() + extension.toString()
  }

  /**
   * Returns the corresponding file
   *
   * @param baseDir the base dir
   * @return the file represented
   */
  fun getFile(baseDir: File): File {
    baseDir.requireIsDirectory()
    return File(baseDir, name)
  }
}
