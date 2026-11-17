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
package it.neckar.open.zip

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Extracts ZIP files
 *
 */
class ZipExtractor @JvmOverloads constructor(
  /**
   * Decides which entries are extracted. `null` extracts every entry.
   */
  val condition: Condition? = null,
) {
  /**
   * Extract the zip file to the given destination.
   *
   * The destination is created if it does not exist, as are the parent directories of nested entries.
   * Entries whose resolved location lies outside the destination are rejected (Zip Slip).
   *
   * @param destination the destination the file will be extracted to
   * @param inputStream the input stream providing the zipped content
   * @throws IllegalArgumentException if the destination is not a directory and cannot be created as one, or if an entry resolves outside the destination
   * @throws IOException if reading the stream or writing an extracted file fails
   */
  fun extract(destination: File, inputStream: InputStream) {
    //Make the parent file directory structure
    destination.mkdirs()

    require(destination.exists() && destination.isDirectory()) { "Invalid destination: " + destination.getCanonicalPath() }

    // Cache the canonical path so the per-entry traversal check stays cheap.
    val canonicalDestinationPath = destination.canonicalPath
    val canonicalDestinationPrefix = canonicalDestinationPath + File.separator

    ZipArchiveInputStream(inputStream).use { zipInputStream ->
      var zipEntry = zipInputStream.nextEntry
      while (zipEntry != null) {
        if (condition != null && condition.shallExtract(zipEntry).not()) {
          zipEntry = zipInputStream.nextEntry
          continue
        }
        val entryName = zipEntry.name
        val newFile = File(destination, entryName)

        // Defend against Zip Slip — reject entries whose canonical path
        // escapes the destination directory.
        val newFileCanonicalPath = newFile.canonicalPath
        require(newFileCanonicalPath == canonicalDestinationPath || newFileCanonicalPath.startsWith(canonicalDestinationPrefix)) {
          "Zip entry [$entryName] resolves outside destination [$canonicalDestinationPath]"
        }

        //Is a directory
        if (zipEntry.isDirectory) {
          newFile.mkdirs()
          zipEntry = zipInputStream.nextEntry
          continue
        }

        //Ensure the parent directory exists: many zip writers emit nested file entries
        //without an explicit directory entry, so we cannot rely on isDirectory having run.
        newFile.parentFile?.mkdirs()

        FileOutputStream(newFile).use { fileOut ->
          zipInputStream.copyTo(fileOut)
        }
        zipEntry = zipInputStream.nextEntry
      }
    }
  }

  /**
   * A condition can decide whether a given zip entry shall be extracted or not
   */
  fun interface Condition {
    /**
     * Returns whether the given entry shall be extracted
     *
     * @param zipEntry the zip entry
     * @return whether the entry shall be extracted
     */
    fun shallExtract(zipEntry: ArchiveEntry): Boolean
  }

  /**
   * Inverts a condition
   */
  class InvertedCondition(
    val condition: Condition,
  ) : Condition {

    override fun shallExtract(zipEntry: ArchiveEntry): Boolean {
      return condition.shallExtract(zipEntry).not()
    }
  }
}
