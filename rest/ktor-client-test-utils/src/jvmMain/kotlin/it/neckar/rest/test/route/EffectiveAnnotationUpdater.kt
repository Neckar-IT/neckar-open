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
import java.io.File

/**
 * Updates effective annotations in the Kotlin source file
 */
class EffectiveAnnotationUpdater {
  fun annotateSourceFile(sourceFile: File, effectiveValueProvider: (qualifiedIdentifier: String) -> String) {
    sourceFile.requireIsFile { "Source file $sourceFile does not exist or is not a file" }
    require(sourceFile.canRead()) { "Source file $sourceFile is not readable" }
    require(sourceFile.canWrite()) { "Source file $sourceFile is not writable" }
    require(sourceFile.extension == "kt") { "Source file $sourceFile is not a Kotlin file" }

    val oldLines = sourceFile.readLines()

    val newLines = annotate(oldLines, effectiveValueProvider)
    sourceFile.writeText(newLines.joinToString("\n"))
  }

  /**
   * Returns a list of the updated lines for the source file
   */
  fun annotate(lines: List<String>, effectiveValueProvider: (qualifiedIdentifier: String) -> String): List<String> {
    //Contains the current object names
    val objectNamesStack = mutableListOf<String>()

    return buildList {
      lines
        //Skip the existing @Effective annotations
        .filterNot { it.trimStart().startsWith("@Effective") }
        .forEach { line ->
          val trimmedLine = line.trim()

          val objectOpenMatchResult = objectOpenRegex.find(trimmedLine)
          if (objectOpenMatchResult != null) {
            val objectName = objectOpenMatchResult.groupValues[1]

            //Ensure there is a '{' on the same line.
            //If not, this might be a multi-line declaration - we look for '{' on subsequent lines
            if (trimmedLine.contains("{").not()) {
              //Skip multi-line object declarations that don't have '{' on the same line
              //The next line with '{' will be handled separately
              return@forEach
            }

            objectNamesStack.add(objectName)
          }

          val valRegexMatchResult = valRegex.find(line.substringBefore("//").trimEnd())
          if (valRegexMatchResult != null) {
            val valName = valRegexMatchResult.groupValues.get(1)
            val valInitializer = valRegexMatchResult.groupValues.get(2) //the code that is assigned to the val

            //Detect the Parameters vals and skip them
            if (valInitializer.startsWith("UrlParameterName(")) {
              add(line)
              //Not a [UrlPattern] - skip this value
              return@forEach
            }

            val qualifiedName = (objectNamesStack + valName).joinToString(".")
            val value = effectiveValueProvider(qualifiedName)

            val indent = line.takeWhile { it.isWhitespace() }
            add("${indent}@Effective(\"$value\")")
          }

          add(line)

          if (trimmedLine == "}" && objectNamesStack.isNotEmpty()) {
            objectNamesStack.removeLast()
          }
        }
    }
  }

  companion object {
    //Regex that matches val definitions (group 1 contains the name)
    val valRegex: Regex = Regex(""".*\bval\s+(\w+)[^=]*=\s*(.+)$""")

    //Regex that matches object openings (group 1 contains the name)
    //Allows optional annotations before 'object' keyword
    //Anchored to start of string to avoid matching 'object' in the middle of text (e.g., in comments)
    val objectOpenRegex: Regex = Regex("""^(?:@\S+\s*)*object\s+(\w+)""")
  }
}
