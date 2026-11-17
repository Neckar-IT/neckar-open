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

/**
 * Represents a module reference for a type.
 */
sealed interface TsModuleReference {
  /**
   * Returns true if import statements are required for this module
   */
  fun requiresImport(): Boolean

  interface NoImportRequired : TsModuleReference {
    override fun requiresImport(): Boolean {
      return false
    }
  }

  /**
   * Represents a native type (e.g., string, number, boolean, Promise, Element)
   */
  data object NativeType : NoImportRequired

  /**
   * Represents a generic type parameter - that does not require any imports
   */
  data object GenericType : NoImportRequired

  /**
   * Represents a reference to the same file - no import required
   */
  data object SameFile : NoImportRequired


  /**
   * Interface that represents references that require an import statement.
   */
  interface RequiresImport : TsModuleReference {
    override fun requiresImport(): Boolean {
      return true
    }

    /**
     * Returns the base file name for the module.
     * Does not include the ".d.ts" suffix.
     */
    fun moduleFileNameBase(): String
  }

  /**
   * Represents Kotlin Native types (e.g., Uuid)
   */
  data object KotlinNativeTypes : RequiresImport {
    override fun moduleFileNameBase(): String {
      return "neckarIT-kotlin-native-types"
    }
  }

  /**
   * Represents a reference to a module file - using the project path
   */
  data class ProjectModuleReference(val projectPath: String) : RequiresImport {
    override fun toString(): String {
      return projectPath
    }

    /**
     * Creates the matching module file name.
     * This uses the same implementation as `GradleProjectUtilsKt.pathAsBaseFileName`
     */
    override fun moduleFileNameBase(): String {
      return "neckarIT-${projectPath.trimStart(':').replace(':', '-')}"
    }
  }

  /**
   * References a module directly - with the *real* name of the module
   */
  data class ModuleReference(val moduleName: String) : RequiresImport {
    override fun toString(): String {
      return moduleName
    }

    override fun moduleFileNameBase(): String {
      return moduleName
    }
  }

}
