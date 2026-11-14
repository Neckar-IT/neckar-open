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
package it.neckar.rest.openapi


/**
 * Contains additional OpenAPI keys that are used in the generated OpenAPI schemas
 */
object AdditionalOpenApiKeys {
  /**
   * The reason a property is deprecated - the message of its Kotlin @Deprecated annotation.
   */
  const val DeprecatedReason: String = "x-deprecated-reason"

  /**
   * Per-enum-value descriptions (object keyed by enum value) - from the enum constants' KDoc.
   */
  const val EnumDescriptions: String = "x-enum-descriptions"

  /**
   * Per-enum-value German terms (object keyed by enum value) - from the enum constants' `@GermanTerm`.
   */
  const val EnumGermanTerms: String = "x-enum-german-terms"

  /**
   * The category of a route: it selects the OpenAPI document under `doc/openapi/{category}` that carries the route.
   */
  const val Category: String = "x-category"

  /**
   * The Kotlin properties of a class that are not serialized.
   */
  const val IgnoredProperties: String = "x-ignored-properties"

  /**
   * The groups of tags in Scalar's sidebar.
   */
  const val TagGroups: String = "x-tagGroups"

  /**
   * The human-readable name of a tag in Scalar's sidebar.
   */
  const val DisplayName: String = "x-displayName"

  /**
   * The special type ref - used in examples to reference the schema type
   */
  const val TypeRef: String = "x-type-ref"

  /**
   * Contains keys to reference the source code of elements in a OpenAPI schema
   */
  object SourceRef {
    /**
     * The key that is used to reference the source Kotlin Type for a type/route
     */
    const val KotlinType: String = "x-source-kotlin-type"

    /**
     * The key that is used to reference the source Kotlin file for a type/route
     */
    const val SourceInfo: String = "x-source-location"

    /**
     * The key that contains the unit symbol for a property (e.g., "mo" for months, "%" for percent).
     * Only present when the property is annotated with a unit annotation (like @Months, @pct).
     */
    const val Unit: String = "x-unit"
  }

  /**
   * Contains keys for value class metadata (unit information)
   */
  object ValueClassMeta {
    /**
     * The unit symbol (e.g., "Wp", "kWh", "m²")
     */
    const val UnitSymbol: String = "x-unit-symbol"

    /**
     * The unit name (e.g., "watt-peak", "kilowatt-hour")
     */
    const val UnitName: String = "x-unit-name"

    /**
     * The German technical term for the value (e.g., "PV-Modulleistung")
     */
    const val GermanTerm: String = "x-german-term"
  }
}

