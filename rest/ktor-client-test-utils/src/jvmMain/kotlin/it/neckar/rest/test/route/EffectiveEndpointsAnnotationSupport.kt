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
package it.neckar.rest.test.route

import it.neckar.open.file.requireIsFile
import it.neckar.open.kotlin.lang.qualifiedNameNonNull
import it.neckar.open.test.utils.LocalResourceUpdateSupport
import java.io.File
import kotlin.reflect.KClass

/**
 * Supports calculation/verification of effective endpoint annotations
 */
class EffectiveEndpointsAnnotationSupport<T : Any>(
  val clazz: KClass<T>,
  val sourceFile: File,
) {
  private fun verifyValidSourceFile() {
    sourceFile.requireIsFile { "Source file $sourceFile does not exist or is not a file" }
    require(sourceFile.canRead()) { "Source file $sourceFile is not readable" }
    require(sourceFile.canWrite()) { "Source file $sourceFile is not writable" }
    require(sourceFile.extension == "kt") { "Source file $sourceFile is not a Kotlin file" }
  }

  /**
   * Reads the content of the source file
   */
  fun readSourceFile(): List<String> {
    verifyValidSourceFile()

    return sourceFile.readLines()
  }

  inline fun collectEffectiveValues(): EffectiveValues {
    return EffectiveEndpointsCalculator.calculateEffectiveValues(clazz)
  }

  fun generateUpdatedSourceFile(): List<String> {
    val oldLines = readSourceFile()
    val effectiveValues = collectEffectiveValues()

    return EffectiveAnnotationUpdater().annotate(oldLines) {
      effectiveValues[it] ?: error("No effective value for $it found")
    }
  }

  fun updateLocalResourceIfNecessary() {
    val updatedFileContent = generateUpdatedSourceFile().joinToString("\n")
    val localResourceUpdateSupport = LocalResourceUpdateSupport(sourceFile, generateIfNotExists = false)
    localResourceUpdateSupport.assertLinesEqual(updatedFileContent)
  }

  companion object {
    /**
     * Creates the support for the given type [T]. Guesses the source file.
     */
    inline fun <reified T : Any> fromType(): EffectiveEndpointsAnnotationSupport<T> {
      val clazz = T::class

      return EffectiveEndpointsAnnotationSupport(
        clazz = clazz,
        sourceFile = guessSourceFile(clazz)
      )
    }

    /**
     * Guesses the source file for the given class
     */
    fun <T : Any> guessSourceFile(clazz: KClass<T>): File {
      val fileNameWithPath = clazz.qualifiedNameNonNull.replace('.', '/') + ".kt"

      val candidates = listOf(
        "src/main/kotlin",
        "src/commonMain/kotlin",
        "src/jvmMain/kotlin",
        "src/jsMain/kotlin",

        "src/test/kotlin",
        "src/commonTest/kotlin",
        "src/jvmTest/kotlin",
        "src/jsTest/kotlin",
      ).map {
        File("$it/$fileNameWithPath")
      }

      return candidates.firstOrNull { it.exists() } ?: error("Cannot guess source file for class ${clazz.qualifiedNameNonNull}")
    }
  }
}
