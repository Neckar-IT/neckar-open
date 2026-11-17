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
import java.util.Locale

/**
 *
 * FileType class.
 *
 */
data class FileType(
  /**
   *
   * Getter for the field `id`.
   *
   * @return a String object.
   */
  val id: String,
  val contentType: String,
  /**
   * Returns whether this file is a dependent type.
   * A dependent file is a file that needs another file it depends on.
   *
   * @return whether this file is a dependent type
   */
  val isDependentType: Boolean,

  val extensions: List<Extension>,
) {

  constructor(
    /**
     *
     * Getter for the field `id`.
     *
     * @return a String object.
     */
    id: String,
    contentType: String,
    /**
     * Returns whether this file is a dependent type.
     * A dependent file is a file that needs another file it depends on.
     *
     * @return whether this file is a dependent type
     */
    isDependentType: Boolean,

    vararg extensions: Extension,

    ) : this(id, contentType, isDependentType, extensions.toList())

  val defaultExtension: Extension
    get() = extensions[0]

  init {
    require(extensions.isNotEmpty()) { "Need at least one extension" }
  }

  /**
   *
   * matches
   *
   * @param fileName a String object.
   * @return a boolean.
   */
  fun matches(fileName: String): Boolean {
    for (ex in extensions) {
      if (fileName.lowercase(Locale.US).endsWith(ex.combined)) {
        return true
      }
    }
    return false
  }

  /**
   *
   * matches
   *
   * @param fileName a FileName object.
   * @return a boolean.
   */
  fun matches(fileName: FileName): Boolean {
    return matches(fileName.name)
  }

  /**
   *
   * getFileName
   *
   * @param file a File object.
   * @return a FileName object.
   */
  fun getFileName(file: File): FileName {
    return getFileName(file.name)
  }

  fun getFileName(fileName: String): FileName { //$NON-NLS-1$
    var bestBase: String? = null
    var bestExtension: Extension? = null

    for (extension in extensions) {
      val index = fileName.lowercase(Locale.ENGLISH).indexOf(extension.combined)
      if (index < 0) {
        continue
      }

      val base = fileName.substring(0, index)
      if (bestBase == null || base.length < bestBase.length) {
        bestBase = base
        bestExtension = extension.createCaseSensitiveExtension(fileName)
      }
    }

    requireNotNull(bestBase) { "Cannot get base for $fileName" }
    requireNotNull(bestExtension) { "bestExtension must not be null" }

    return FileName(bestBase, bestExtension)
  }

  fun getExtension(fileName: String): Extension {
    return getFileName(fileName).extension
  }

  fun getBaseName(fileName: String): String {
    return getFileName(fileName).baseName.name
  }

  fun isDefaultExtension(extension: Extension): Boolean {
    return defaultExtension == extension
  }
}
