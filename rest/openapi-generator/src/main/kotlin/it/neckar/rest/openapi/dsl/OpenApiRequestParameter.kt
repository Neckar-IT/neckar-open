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
package it.neckar.rest.openapi.dsl
import it.neckar.lifeCycle.HasArchiveState
import it.neckar.lifeCycle.ArchiveState
import it.neckar.open.http.UrlParameterName
import it.neckar.open.kotlin.serializers.KClassSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 * Represents a request parameter
 */
data class OpenApiRequestParameter(
  /**
   * The name (case-sensitive) of the parameter
   */
  val name: UrlParameterName,

  /**
   * The type of the parameter
   */
  val type: SchemaDescriptor,

  val required: Boolean? = null,

  val location: Location = Location.Query,

  val description: String? = null,

  override val examples: List<OpenApiExample> = emptyList(),

  override val archiveState: ArchiveState = ArchiveState.Active,

  val kotlinSourceType: @Serializable(with = KClassSerializer::class) KClass<*>? = null,

  ) : HasArchiveState, HasExamples {

  init {
    if (location == Location.Path) {
      require(required != null && required) { "Path parameter $name must be required" }
    }
  }

  enum class Location {
    /**
     * Query parameter
     */
    Query,

    /**
     * Path parameter
     */
    Path,

    /**
     * Header parameter
     */
    Header,
  }
}


@OpenApiDsl
class OpenApiRequestParameterBuilder<ExampleType : Any>(val name: UrlParameterName, val type: SchemaDescriptor, val kotlinSourceType: KClass<*>? = null) : HasExamplesBuilder<ExampleType>() {
  var required: Boolean? = null
  var location: OpenApiRequestParameter.Location = OpenApiRequestParameter.Location.Query
  var archiveState: ArchiveState = ArchiveState.Active
  var description: String? = null

  fun required(required: Boolean?): OpenApiRequestParameterBuilder<ExampleType> = apply { this.required = required }

  fun location(location: OpenApiRequestParameter.Location): OpenApiRequestParameterBuilder<ExampleType> = apply { this.location = location }

  fun archiveState(archiveState: ArchiveState): OpenApiRequestParameterBuilder<ExampleType> = apply { this.archiveState = archiveState }

  fun build(): OpenApiRequestParameter {
    return OpenApiRequestParameter(name, type, effectiveRequired(), location, description, buildExamples(), archiveState, kotlinSourceType)
  }

  private fun buildExamples(): List<OpenApiExample> {
    return examples.map { builder -> builder.build() }
  }

  private fun effectiveRequired(): Boolean? {
    return required ?: (location == OpenApiRequestParameter.Location.Path)
  }

  /**
   * Initializes the values from the old parameter
   */
  fun initializeValues(oldParameter: OpenApiRequestParameterBuilder<*>) {
    this.required = oldParameter.required
    this.location = oldParameter.location
    this.archiveState = oldParameter.archiveState
  }
}

fun List<OpenApiRequestParameterBuilder<*>>.build(): List<OpenApiRequestParameter> {
  return map { it.build() }
}
