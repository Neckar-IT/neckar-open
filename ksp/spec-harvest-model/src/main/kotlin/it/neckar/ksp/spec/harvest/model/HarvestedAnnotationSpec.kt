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
 * Represents a single annotation with its arguments.
 */
@Serializable
data class HarvestedAnnotationSpec(
  /**
   * Fully qualified annotation name (falls back to simple name if unavailable).
   */
  val name: String,
  /**
   * Named argument values rendered as strings for stable serialization - string values carry no surrounding quotes.
   *
   * An unnamed argument is keyed `value`; a key that is already taken gets a `#1`, `#2`, ... suffix so that no
   * argument is dropped from the map.
   */
  val arguments: Map<String, String?>,
  /**
   * The meta-annotations applied directly to this annotation's declaration; each one carries its own
   * [metaAnnotations] in turn, so the tree is nested rather than flattened.
   *
   * Empty for the Kotlin standard annotations in `kotlin.annotation` - the harvester stops there instead of
   * walking their (self-referential) meta-annotations. Empty as well whenever meta-annotations were not harvested.
   */
  val metaAnnotations: List<HarvestedAnnotationSpec> = emptyList(),
)
