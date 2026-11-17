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
import it.neckar.open.kotlin.serializers.AnySerializableSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Represents an example of a response or request body or parameter
 */
class OpenApiExample(
  /**
   * Human-readable identifier of the example
   */
  val exampleId: OpenApiExample.Id,

  /**
   * The actual example object/value
   */
  val example: @Serializable(with = AnySerializableSerializer::class) Any,

  /**
   * A short description of the example
   */
  override val summary: String? = null,

  /**
   * A long description of the example
   */
  override val description: String? = null,

  /**
   * Will be exported as [it.neckar.rest.openapi.AdditionalOpenApiKeys.SourceRef.KotlinType]
   */
  val originalType: KClass<out Any>?,

  /**
   * The full type of [example], type arguments included. [originalType] is taken from the instance
   * and therefore erased; a generic example can only be resolved to a schema and to a serializer
   * through this one.
   */
  val exampleType: KType? = null,

  ) : HasDescription {

  /**
   * Human-readable identifier of the example
   */
  @Serializable
  @JvmInline
  value class Id(
    val value: String,
  ) {

    init {
      require(value.isNotBlank()) { "Id cannot be blank" }
      require(!value.contains(" ")) { "Id cannot contain spaces but was [$value]" }
    }

    override fun toString(): String {
      return value
    }
  }
}


@OpenApiDsl
class OpenApiExampleBuilder<ExampleType : Any>(
  val exampleId: OpenApiExample.Id,
  var example: ExampleType,
) {
  /**
   * The summary of the example
   */
  var summary: String? = null

  /**
   * The description of the example
   */
  var description: String? = null

  /**
   * Will be exported as "x-original-type"
   */
  var originalType: KClass<out ExampleType>? = example::class

  /**
   * The full type of [example], type arguments included — see [OpenApiExample.exampleType].
   */
  var exampleType: KType? = null

  /**
   * Builds the example
   */
  fun build(): OpenApiExample = OpenApiExample(exampleId, example, summary, description, originalType, exampleType)
}

/**
 * Builds a list of [OpenApiExample]s from a list of [OpenApiExampleBuilder]s
 */
fun List<OpenApiExampleBuilder<*>>.build(): List<OpenApiExample> {
  return map { it.build() }
}

/**
 * Converts a list of [OpenApiExample]s to a list of [OpenApiExampleBuilder]s
 */
fun List<OpenApiExample>.toBuilder(): List<OpenApiExampleBuilder<*>> {
  return map { OpenApiExampleBuilder(it.exampleId, it) }
}
