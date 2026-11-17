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
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 * Describes a header element
 */
data class OpenApiHeader(
  /**
   * The name of the header
   */
  val name: String,

  /**
   * The schema of the header - only primitive types are supported.
   */
  val type: SchemaDescriptor.NativePrimitiveSchema,

  /**
   * A description of the header
   */
  override val description: String? = null,

  override val summary: String? = null,

  /**
   * Determines whether this header is mandatory
   */
  val required: Boolean? = null,

  override val examples: List<OpenApiExample> = emptyList(),

  override val archiveState: ArchiveState = ArchiveState.Active,

  ) : HasDescription, HasArchiveState, HasExamples {

  init {
    require(name.isNotBlank()) { "Header name must not be blank" }
  }
}

/**
 * Builder for [OpenApiHeader]
 */
@OpenApiDsl
class OpenApiHeaderBuilder<ExampleType : Any>(var name: String, var type: SchemaDescriptor.NativePrimitiveSchema) : HasExamplesBuilder<ExampleType>() {
  var description: String? = null
  var summary: String? = null
  var required: Boolean? = null
  var archiveState: ArchiveState = ArchiveState.Active

  constructor(header: OpenApiHeader) : this(name = header.name, type = header.type) {
    this.description = header.description
    this.summary = header.summary
    this.required = header.required
    this.examples.addAll(header.examples.toBuilder())
    this.archiveState = header.archiveState
  }

  fun type(type: SchemaDescriptor.NativePrimitiveSchema): OpenApiHeaderBuilder<ExampleType> = apply { this.type = type }

  fun description(description: String?): OpenApiHeaderBuilder<ExampleType> = apply { this.description = description }
  fun summary(summary: String?): OpenApiHeaderBuilder<ExampleType> = apply { this.summary = summary }
  fun required(required: Boolean?): OpenApiHeaderBuilder<ExampleType> = apply { this.required = required }

  fun archiveState(archiveState: ArchiveState): OpenApiHeaderBuilder<ExampleType> = apply { this.archiveState = archiveState }

  fun build(): OpenApiHeader {
    return OpenApiHeader(name, type, description, summary, required, examples.build(), archiveState)
  }
}
