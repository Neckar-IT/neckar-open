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
package it.neckar.rest.openapi.spec.harvest

import it.neckar.ksp.spec.harvest.model.SpecHarvestResult
import it.neckar.open.file.requireIsDirectory
import it.neckar.open.kotlin.lang.requireNotNull
import java.io.File
import java.net.URL

/**
 * Returns the harvested spec files and loads them
 */
object HarvestedSpecSupport {
  /**
   * Lists all URLs for all specs on the classpath
   */
  fun findAllSpecDirs(): List<URL> {
    return Thread.currentThread().contextClassLoader
      .getResources("META-INF/harvested-specs")
      .toList()
  }

  fun findProductionSpecDirs(): List<URL> {
    return findAllSpecDirs().filter {
      //Skip test specs
      isInTestPath(it).not()
    }
  }

  fun findTestSpecDirs(): List<URL> {
    return findAllSpecDirs().filter {
      //Skip test specs
      isInTestPath(it)
    }
  }

  private fun isInTestPath(uRL: URL): Boolean = uRL.path.contains("test/META-INF")

  fun findAllSpecFiles(): List<File> {
    return listSpecFilesFromDirs(findAllSpecDirs())
  }

  fun findProductionSpecFiles(): List<File> {
    return listSpecFilesFromDirs(findProductionSpecDirs())
  }

  fun findTestSpecFiles(): List<File> {
    return listSpecFilesFromDirs(findTestSpecDirs())
  }

  private fun listSpecFilesFromDirs(dirUrls: List<URL>): List<File> {
    return dirUrls
      .filter {
        it.protocol == "file"
      }
      .map {
        try {
          File(it.toURI()).requireIsDirectory()
        } catch (e: IllegalArgumentException) {
          throw IllegalArgumentException("Could not convert URL to File: [$it]", e)
        }
    }.flatMap {
      it.listFiles { _, name -> name.endsWith(".json") }
        .requireNotNull {
          "Could not list files in directory: ${it.absolutePath}"
        }.toList()
    }
  }

  fun loadAllSpecs(): List<SpecHarvestResult> {
    return findAllSpecFiles().map {
      it.inputStream().use { inputStream ->
        SpecHarvestResult.load(inputStream)
      }
    }
  }

  /**
   * Loads all production specs
   */
  fun loadProductionSpecs(): List<SpecHarvestResult> {
    return findProductionSpecFiles().map {
      it.inputStream().use { inputStream ->
        SpecHarvestResult.load(inputStream)
      }
    }
  }

  fun loadTestSpecs(): List<SpecHarvestResult> {
    return findTestSpecFiles().map {
      it.inputStream().use { inputStream ->
        SpecHarvestResult.load(inputStream)
      }
    }
  }
}
