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
import java.io.IOException
import javax.annotation.Nonnull

/**
 * Offers utility methods for file copying.
 *
 * Thin wrapper around the Kotlin stdlib file operations ([copyTo], [copyRecursively],
 * [deleteRecursively]) which handle buffered full-length copies and resource cleanup correctly.
 */
object FileCopyManager {
  /**
   * Deletes [toDelete] and, if it is a directory, its whole subtree. The file must exist.
   *
   * @throws IOException if the deletion does not remove the file (or a descendant).
   */
  @JvmStatic
  fun deleteForced(@Nonnull toDelete: File) {
    require(toDelete.exists()) { "File must exist: ${toDelete.absolutePath}" }
    if (!toDelete.deleteRecursively()) {
      throw IOException("Failed to delete <${toDelete.absolutePath}>")
    }
  }

  /**
   * Copies [src] to [dest], dispatching to [copyDirectory] for directories and [copyFile] for files.
   *
   * @throws IOException if any.
   */
  @JvmStatic
  fun copy(@Nonnull src: File, @Nonnull dest: File) {
    if (src.isDirectory) {
      copyDirectory(src, dest)
    } else {
      copyFile(src, dest)
    }
  }

  /**
   * Copies the file [source] to [target], overwriting an existing target.
   *
   * @throws IOException if any.
   */
  @JvmStatic
  fun copyFile(@Nonnull source: File, @Nonnull target: File) {
    source.copyTo(target, overwrite = true)
  }

  /**
   * Copies the directory [srcDir] and its whole subtree into [destDir], overwriting existing files.
   *
   * @throws IOException if any.
   */
  @JvmStatic
  fun copyDirectory(@Nonnull srcDir: File, @Nonnull destDir: File) {
    srcDir.copyRecursively(destDir, overwrite = true)
  }
}
