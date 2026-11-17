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
import io.ktor.http.*
import it.neckar.open.kotlin.lang.simpleNameWithEnclosing
import it.neckar.rest.RestResponse
import it.neckar.rest.io.ContentTypeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.typeOf
import kotlin.reflect.full.isSubclassOf
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.isPrimitive
import it.neckar.rest.openapi.OpenApiException


/**
 * Simple body - as opposed to multipart body
 */
data class OpenApiSimpleBody(
  /**
   * The type defining the schema used for the body.
   */
  @SerialName("schemaType") //must be renamed to avoid clash with class discriminator
  val type: SchemaDescriptor,

  override val summary: String? = null,
  override val description: String? = null,
  override val required: Boolean?,
  /**
   * The media type - is used as key when generating the OpenAPI spec
   */
  val mediaType: @Serializable(with = ContentTypeSerializer::class) ContentType,
  /**
   * The examples for this body.
   */
  override val examples: List<OpenApiExample>,
) : OpenApiBody, HasExamples, HasDescription


/**
 * Builder for a simple body
 */
@OpenApiDsl
class OpenApiSimpleBodyBuilder<ExampleType : Any>(
  /**
   * The schema type for the example
   */
  type: SchemaDescriptor
) {

  var type: SchemaDescriptor = type
    set(value) {
      //Paranoia-check
      requireAvoidPossibleInvalidType(value)
      field = value
    }

  var summary: String? = null
  var description: String? = null
  var required: Boolean? = true
  var mediaType: ContentType? = null
  val examples: MutableList<OpenApiExampleBuilder<*>> = mutableListOf()

  /**
   * Alias for [mediaType]. Developers often use the term "content type" instead of "media type"
   */
  @Deprecated("Use mediaType instead")
  var contentType: ContentType?
    get() = mediaType
    set(value) {
      mediaType = value
    }

  /**
   * If set to true, no examples must be provided
   */
  private var noExamples: Boolean = false

  /**
   * Call this method to indicate that the examples are not relevant for this body.
   */
  fun noExamples() {
    noExamples = true
  }

  /**
   * Appends an example to this body.
   */
  inline fun <reified Actual : ExampleType> example(
    exampleIdAsString: String,
    example: Actual,
    summary: String? = guessSummary(example),
    originalType: KClass<out Actual>? = example::class,
    noinline block: OpenApiExampleBuilder<Actual>.() -> Unit = {}
  ) {
    example(OpenApiExample.Id(exampleIdAsString), example, summary, originalType, block)
  }

  /**
   * Appends an example to this body.
   */
  inline fun <reified Actual : ExampleType> example(
    exampleId: OpenApiExample.Id,
    example: Actual,
    summary: String? = guessSummary(example),
    originalType: KClass<out Actual>? = example::class,
    noinline block: OpenApiExampleBuilder<Actual>.() -> Unit = {}
  ) {
    examples.add(
      OpenApiExampleBuilder(exampleId, example)
        .apply {
          this.summary = summary
          this.originalType = originalType
          this.exampleType = typeOf<Actual>()
        }
        .apply(block)
    )
  }

  /**
   * Guesses a summary from the example type
   */
  @PublishedApi
  internal fun guessSummary(example: ExampleType): String {
    if (example is RestResponse.GoodResponse<*>) {
      return "Good"
    }
    if (example is RestResponse.BadResponse<*>) {
      return "Bad"
    }

    return example::class.simpleNameWithEnclosing
  }

  fun build(): OpenApiSimpleBody {
    verifyExamples()
    return OpenApiSimpleBody(type, summary, description, required, relevantMediaType(), examples.build())
  }

  private fun verifyExamples() {
    if (noExamples) return
    if (examples.isNotEmpty()) return

    if (type.isPrimitive()) {
      //We do not provide examples for primitive types
      return
    }

    throw OpenApiException(OpenApiException.Problem.MissingExample)
  }

  private fun relevantMediaType(): ContentType {
    mediaType?.let {
      return it
    }

    if (type == SchemaDescriptor.String) {
      return ContentType.Text.Plain
    }

    return ContentType.Application.Json
  }
}

//TODO add multipart body
//See https://swagger.io/docs/specification/describing-request-body/multipart-requests/ for more info

///**
// * Describes a single request/response body with multipart content.
// * See https://swagger.io/docs/specification/describing-request-body/multipart-requests/ for more info
// */
//class OpenApiMultipartBody : OpenApiBaseBody() {
//
//  private val parts = mutableListOf<OpenApiMultipartPart>()
//
//  fun getParts(): List<OpenApiMultipartPart> = parts
//
//
//  /**
//   * One part of a multipart-body
//   */
//  fun part(name: String, type: BodyTypeDescriptor, block: OpenApiMultipartPart.() -> Unit) {
//    parts.add(OpenApiMultipartPart(name, type).apply(block))
//  }
//
//
//  /**
//   * One part of a multipart-body
//   */
//  fun part(name: String, type: BodyTypeDescriptor) = part(name, type) {}
//
//
//  /**
//   * One part of a multipart-body
//   */
//  fun part(name: String, type: SchemaType, block: OpenApiMultipartPart.() -> Unit) = part(name, BodyTypeDescriptor.typeOf(type), block)
//
//
//  /**
//   * One part of a multipart-body
//   */
//  fun part(name: String, type: KClass<*>) = part(name, type.asSchemaType()) {}
//
//
//  /**
//   * One part of a multipart-body
//   */
//  inline fun <reified TYPE> part(name: String) = part(name, getSchemaType<TYPE>()) {}
//
//
//  /**
//   * One part of a multipart-body
//   */
//  inline fun <reified TYPE> part(name: String, noinline block: OpenApiMultipartPart.() -> Unit) = part(name, getSchemaType<TYPE>(), block)
//
//
//  /**
//   * One part of a multipart-body
//   */
//  fun part(name: String, customSchemaId: String, block: OpenApiMultipartPart.() -> Unit) =
//    part(name, BodyTypeDescriptor.custom(customSchemaId), block)
//
//
//  /**
//   * One part of a multipart-body
//   */
//  fun part(name: String, customSchemaId: String) = part(name, customSchemaId) {}
//
//}


/**
 * Checks whether the schema is a possible invalid type.
 */
private fun requireAvoidPossibleInvalidType(schema: SchemaDescriptor) {
  val classSchema = when (schema) {
    is SchemaDescriptor.ClassSchemaObject -> schema
    is SchemaDescriptor.DiscriminatedClassSchemaWrapper -> schema.baseSchema
    else -> return
  }

  require(classSchema.asClass().isSubclassOf(RestResponse.BadResponse::class).not()) {
    "Cannot use RestResponse.BadResponse as body type. Use the RestResponse type instead"
  }
  require(classSchema.asClass().isSubclassOf(RestResponse.GoodResponse::class).not()) {
    "Cannot use RestResponse.GoodResponse as body type. Use the RestResponse type instead"
  }
}
