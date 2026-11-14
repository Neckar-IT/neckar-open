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
package it.neckar.open.app.xdg

import it.neckar.open.kotlin.lang.checkNotNull
import java.io.File

/**
 * Created from http://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html
 */
object XdgUtil {
  private val homeDir: File
    get() {
      val userHomeAsString = System.getProperty("user.home").checkNotNull { "No property found for user.home" }

      val home = File(userHomeAsString)
      if (!home.isDirectory) {
        throw IllegalStateException("User home <" + home.absolutePath + "> is not a directory")
      }
      return home
    }

  /**
   * The base directory relative to which user specific data files should be stored.
   * If $XDG_DATA_HOME is either not set or empty, a default equal to $HOME/.local/share should be used.
   */
  const val xdgDataHome: String = "XDG_DATA_HOME"

  /**
   * The base directory relative to which user specific configuration files should be stored.
   * If $XDG_CONFIG_HOME is either not set or empty, a default equal to $HOME/.config should be used.
   */
  const val xdgConfigHome: String = "XDG_CONFIG_HOME"

  /**
   * The preference-ordered set of base directories to search for data files in addition to
   * the $XDG_DATA_HOME base directory.
   * The directories in $XDG_DATA_DIRS should be separated with a colon ':'.
   *
   * If $XDG_DATA_DIRS is either not set or empty, a value equal to /usr/local/share/:/usr/share/ should be used.
   */
  const val xdgDataDirs: String = "XDG_DATA_DIRS"

  /**
   * The preference-ordered set of base directories to search for configuration
   * files in addition to the $XDG_CONFIG_HOME base directory.
   * The directories in $XDG_CONFIG_DIRS should be seperated with a colon ':'.
   *
   * If $XDG_CONFIG_DIRS is either not set or empty, a value equal to /etc/xdg should be used.
   *
   * The order of base directories denotes their importance; the first directory listed is the most important.
   * When the same information is defined in multiple places the information defined relative to the more important base directory takes precedent.
   * The base directory defined by $XDG_DATA_HOME is considered more important than any of the base directories defined by $XDG_DATA_DIRS.
   * The base directory defined by $XDG_CONFIG_HOME is considered more important than any of the base directories defined by $XDG_CONFIG_DIRS.

   */
  const val xdgConfigDirs: String = "XDG_CONFIG_DIRS"

  /**
   * The base directory relative to which user specific non-essential data files should be stored.
   * If $XDG_CACHE_HOME is either not set or empty, a default equal to $HOME/.cache should be used.
   */
  const val xdgCacheHome: String = "XDG_CACHE_HOME"

  /**
   * The base directory relative to which user-specific non-essential runtime files and other file objects (such as sockets, named pipes, ...) should be stored.
   * The directory MUST be owned by the user, and he MUST be the only one having read and write access to it. Its Unix access mode MUST be 0700.
   */
  const val xdgRuntimeDir: String = "XDG_RUNTIME_DIR"

  /**
   * Returns the data home dir
   */
  @JvmStatic
  val dataHome: File
    get() {
      val value = System.getenv()[xdgDataHome]
      return getDir(value, File(homeDir, ".local/share"))
    }

  /**
   * Returns the config home dir
   */
  @JvmStatic
  val configHome: File
    get() {
      val value = System.getenv()[xdgConfigHome]
      return getDir(value, File(homeDir, ".config"))
    }

  /**
   * Returns the cache home dir
   */
  @JvmStatic
  val cacheHome: File
    get() {
      val value = System.getenv()[xdgCacheHome]
      return getDir(value, File(homeDir, ".cache"))
    }

  /**
   * Resolves an XDG base directory: returns [defaultDir] when the environment value is not set or
   * empty (per the XDG base directory spec), otherwise a [File] for the given value.
   */
  internal fun getDir(dirName: String?, defaultDir: File): File {
    if (dirName.isNullOrEmpty()) {
      return defaultDir
    }
    return File(dirName)
  }

}
