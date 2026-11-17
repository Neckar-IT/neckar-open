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
package it.neckar.ksp.ts.model

import it.neckar.gradle.ksp.generating.types.list.model.Class2ProjectMapping
import it.neckar.gradle.ksp.generating.types.list.model.ProjectPath
import it.neckar.ksp.ts.model.KotlinModulesGuesser.guessProjectPath

/**
 * Returns the module name for a fully qualified package
 */
object KotlinModulesGuesser {
  /**
   * This instance should be set before using [guessProjectPath]
   */
  private val class2ProjectMappings: MutableList<Class2ProjectMapping> = mutableListOf()

  /**
   * Clears all entries
   */
  fun clear() {
    class2ProjectMappings.clear()
  }

  fun registerClass2ProjectMapping(class2ProjectMapping: Class2ProjectMapping) {
    if (class2ProjectMappings.contains(class2ProjectMapping)) {
      //Might happen if there are multiple passes
      return
    }

    class2ProjectMappings.add(class2ProjectMapping)
  }

  /**
   * Guesses the module name for a fully qualified package
   */
  fun guessProjectPath(fqName: String): ProjectPath? {
    if (class2ProjectMappings.isEmpty()) {
      throw IllegalStateException("class2ProjectMappings is empty")
    }

    val projectPath = class2ProjectMappings
      .asSequence()
      .mapNotNull { it.mapping[fqName] }
      .firstOrNull()


    return projectPath
  }

  /**
   * Returns the number of registered classes
   */
  fun registeredClassesCount(): Int {
    return class2ProjectMappings.size
  }
}
