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
package it.neckar.open.app

import it.neckar.open.app.xdg.WindowsUtil
import it.neckar.open.app.xdg.XdgUtil
import it.neckar.open.kotlin.lang.checkNotNull
import it.neckar.open.time.nowMillisLong
import java.io.File

/**
 * Offers access to the application home dir.

 */
interface ApplicationHomeAccess {
  /**
   * Returns the application name
   */
  val applicationName: String

  /**
   * Returns the directory that should be used for the configuration
   */
  val configHome: File

  /**
   * Returns the directory that should be used for the user specific data
   */
  val dataHome: File

  /**
   * Returns the directory that should be used for caches
   */
  val cacheHome: File

  companion object {
    fun create(applicationName: String): ApplicationHomeAccess {
      return ApplicationHomeAccessFactory.create(applicationName)
    }
  }
}

/**
 * Factory for application home access
 */
object ApplicationHomeAccessFactory {
  @JvmStatic
  fun create(applicationName: String): ApplicationHomeAccess {
    val osName = System.getProperty("os.name").checkNotNull { "Property os.name not found" }

    if (osName.contains("Linux")) {
      return createLinuxHomeAccess(applicationName)
    }

    if (osName.contains("Windows")) {
      return createWindowsHomeAccess(applicationName)
    }

    throw IllegalStateException("Unsupported OS: $osName")
  }

  @JvmStatic
  private fun createWindowsHomeAccess(applicationName: String): ApplicationHomeAccess {
    val appData = File(WindowsUtil.appData, applicationName)
    createDirIfNecessary(appData)
    val localAppData = File(WindowsUtil.localAppData, applicationName)
    createDirIfNecessary(localAppData)
    return StaticApplicationHomeAccess(applicationName, appData, appData, localAppData)
  }


  @JvmStatic
  private fun createLinuxHomeAccess(applicationName: String): ApplicationHomeAccess {
    val configHome = File(XdgUtil.configHome, applicationName)
    createDirIfNecessary(configHome)
    val dataHome = File(XdgUtil.dataHome, applicationName)
    createDirIfNecessary(dataHome)
    val cacheHome = File(XdgUtil.cacheHome, applicationName)
    createDirIfNecessary(cacheHome)

    return StaticApplicationHomeAccess(applicationName, configHome, dataHome, cacheHome)
  }

  /**
   * Creates app home access within the temp dir
   */
  @JvmStatic
  fun createTemporaryApplicationHomeAccess(): ApplicationHomeAccess {
    val dir = File(File(System.getProperty("java.io.tmpdir")), "." + nowMillisLong())
    return createTemporaryApplicationHomeAccess(dir)
  }

  @JvmStatic
  fun createTemporaryApplicationHomeAccess(dir: File): ApplicationHomeAccess {
    val configHome = File(dir, "config")
    createDirIfNecessary(configHome)
    val data = File(dir, "data")
    createDirIfNecessary(data)
    val cacheHome = File(dir, "cache")
    createDirIfNecessary(cacheHome)

    return StaticApplicationHomeAccess("mockDir", configHome, data, cacheHome)
  }

  @JvmStatic
  private fun createDirIfNecessary(dir: File) {
    if (dir.isDirectory) {
      return
    }

    if (dir.isFile) {
      throw IllegalStateException(dir.absolutePath + " is a file")
    }

    if (dir.exists()) {
      throw IllegalStateException(dir.absolutePath + " still exists but is not a dir")
    }

    if (!dir.mkdirs()) {
      throw IllegalStateException("Could not create directory <" + dir.absolutePath + ">")
    }
  }
}
