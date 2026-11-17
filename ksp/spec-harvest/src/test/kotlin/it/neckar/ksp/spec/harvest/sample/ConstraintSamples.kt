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
package it.neckar.ksp.spec.harvest.sample

import kotlinx.serialization.Serializable

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
internal annotation class MinLength(val value: Int)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
internal annotation class Pattern(val value: String)

/**
 * Value class whose wrapped property carries JSON-Schema constraint annotations, exercising the
 * harvesting of an integer (`@MinLength(4)`) and a string (`@Pattern`) annotation argument.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
internal annotation class GermanTerm(val value: String)

@Serializable
@JvmInline
internal value class PartNumber(
  @MinLength(4)
  @Pattern("[A-Z0-9]+")
  val value: String,
)

/**
 * Enum whose constants carry KDoc and annotations - exercising enum-entry harvesting.
 */
@Serializable
internal enum class SampleStatus {
  /** The item is active. */
  @GermanTerm("Aktiv")
  Active,

  /** The item is archived. */
  Archived,
}

/**
 * Data class with a property-level `@Deprecated` annotation, exercising the harvesting of its
 * `message` argument. The message is passed positionally, so KSP has to resolve the argument name
 * to `message` (not the `value` fallback) for reverse lookup via `findArgument("Deprecated", "message")`.
 */
@Serializable
internal data class DeprecatedTokenHolder(
  @property:Deprecated("Use the OAuth2 flow instead")
  val legacyToken: String,
)
