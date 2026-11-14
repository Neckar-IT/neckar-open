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
package it.neckar.ksp.types.list.generator

/**
 * Holds the results collected during a [TypesListGeneratorProcessor] run.
 *
 * ## Important: Incremental Compilation
 *
 * This context is only populated during the first processing pass of a KSP invocation.
 * During incremental builds, the values may not reflect the complete module state.
 *
 * For production use, prefer reading the generated `.list` files directly.
 */
class TypesListGenerationContext {
  /**
   * All package names found in the module.
   * Populated only during the first processing pass.
   */
  var packageNames: List<String>? = null

  /**
   * All class names (including type aliases) found in the module.
   * Populated only during the first processing pass.
   */
  var classNames: List<String>? = null
}
