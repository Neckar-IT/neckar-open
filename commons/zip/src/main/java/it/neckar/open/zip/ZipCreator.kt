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
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Helper class that creates zip files
 *
 */
class ZipCreator(val zipFile: File) {

  /**
   * Zip all files within the given directories
   *
   * @param directories the directories all files are zipped within
   * @return the zipped file
   *
   * @throws IOException if a directory cannot be listed or the zip file cannot be written
   */
  fun zip(vararg directories: File): File {
    ZipArchiveOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { outStream ->
      for (directory in directories) {
        val baseName = directory.getCanonicalPath()
        addFiles(baseName, outStream, directory)
      }
    }
    return zipFile
  }

  /**
   * Add the files within the given directory to the [ZipArchiveOutputStream]. This method will call itself for each subdirectory.
   *
   * @param baseName  the canonical base path that is stripped from every entry name
   * @param outStream the output stream
   * @param directory the directory
   * @throws IOException if the directory cannot be listed or an entry cannot be written
   */
  fun addFiles(baseName: String, outStream: ZipArchiveOutputStream, directory: File) {
    val files = directory.listFiles() ?: throw IOException("could not list <" + directory.absolutePath + ">")

    for (file in files) {
      val relativeName = getCanonicalRelativePath(baseName, file)
      val entry = ZipArchiveEntry(relativeName)

      outStream.putArchiveEntry(entry)

      if (file.isDirectory()) {
        // The directory entry must be closed before the next putArchiveEntry() call;
        // otherwise the recursion below opens a second entry while this one is still
        // open and ZipArchiveOutputStream throws IllegalStateException.
        outStream.closeArchiveEntry()
        //Add the files within the directory
        addFiles(baseName, outStream, file)
        continue
      }

      FileInputStream(file).buffered().use { origin ->
        origin.copyTo(outStream)
      }

      outStream.closeArchiveEntry()
    }
  }

  companion object {
    /**
     * Returns the relative canonical path
     *
     * @param baseName the canonical base path
     * @param file     the file
     * @return the path of the given file relative to the base name
     * @see getRelativePath
     * @throws IOException if the canonical path of the file cannot be resolved
     */
    fun getCanonicalRelativePath(baseName: String, file: File): String {
      var name = file.getCanonicalPath().substring(baseName.length + 1) // use #getCanonicalPath because baseName is canonical
      if (file.isDirectory()) {
        name += '/'
      }
      return name.replace("\\\\".toRegex(), "/")
    }

    /**
     * Returns the relative path
     *
     * @param baseName the base path
     * @param file     the file
     * @return the path of the given file relative to the base name
     * @see getCanonicalRelativePath
     */
    fun getRelativePath(baseName: String, file: File): String {
      var name = file.absolutePath.substring(baseName.length + 1)
      if (file.isDirectory()) {
        name += '/'
      }
      return name.replace("\\\\".toRegex(), "/")
    }
  }
}
