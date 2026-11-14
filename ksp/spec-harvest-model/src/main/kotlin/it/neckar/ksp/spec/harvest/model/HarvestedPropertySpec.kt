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
package it.neckar.ksp.spec.harvest.model

import kotlinx.serialization.Serializable

/**
 * Represents a single property inside a class/interface/etc.
 */
@Serializable
data class HarvestedPropertySpec(
  /**
   * Simple property name as declared in the source.
   */
  val name: String,
  /**
   * Rendered Kotlin type, including generics/nullability. Names are qualified: `kotlin.collections.List<kotlin.String>?`.
   */
  val type: String,
  /**
   * Whether the resolved type allows `null`.
   */
  val isNullable: Boolean,
  /**
   * True when the property is declared `var`; otherwise immutable `val`.
   */
  val isMutable: Boolean,
  /**
   * Indicates that the backing constructor parameter defines a default value. False for a property declared in the
   * class body, even with an initializer - KSP exposes the flag on constructor parameters only.
   */
  val hasDefault: Boolean,
  /**
   * KDoc or comment text assigned to the property, trimmed.
   */
  val documentation: String? = null,
  /**
   * Annotations that decorate the property declaration.
   */
  val annotations: List<HarvestedAnnotationSpec>,
  /**
   * The type annotations (annotations that are applied to the type itself)
   */
  val typeAnnotations: List<HarvestedAnnotationSpec> = emptyList(),
)
