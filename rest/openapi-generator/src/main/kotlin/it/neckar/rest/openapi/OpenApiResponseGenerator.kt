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
@file:Suppress("IdentifierGrammar")

package it.neckar.rest.openapi

import it.neckar.open.collections.fastForEach
import it.neckar.open.collections.fastForEachIndexed
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.kotlin.serializers.AnySerializableRawSerializer
import it.neckar.open.kotlin.serializers.findSerialName
import it.neckar.open.kotlin.serializers.putNotNull
import it.neckar.rest.openapi.schema.SchemaDescriptor.ComponentSchema.ReferenceType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import it.neckar.rest.openapi.dsl.OpenApiExample
import it.neckar.rest.openapi.dsl.OpenApiRequest
import it.neckar.rest.openapi.dsl.OpenApiResponse
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.SchemaPropertyConstraints
import it.neckar.rest.openapi.schema.isComponent

/**
 * Helper class for request/response generation in OpenAPI.
 *
 * This class handles:
 * - Response generation for routes
 * - Request body generation
 * - Schema type references with examples
 */
internal class OpenApiResponseGenerator(
  private val schemaGenerator: OpenApiSchemaGenerator,
  private val metadataWriter: OpenApiMetadataWriter,
) {

  /**
   * Generates the responses section for a single route.
   * Creates an entry for each available response code.
   */
  internal fun generateResponses(responses: List<OpenApiResponse>): JsonElement {
    return buildJsonObject {
      responses.fastForEach { response ->
        //Create an entry per response: a status code, or the "default" catch-all key
        val key = if (response.isDefault) "default" else response.statusCode.value.toString()
        put(key, generateResponse(response))
      }
    }
  }

  /**
   * Creates the response section for a single response.
   */
  private fun generateResponse(response: OpenApiResponse): JsonObject {
    return buildJsonObject {
      put("description", response.description)
      putNotNull("summary", response.summary)

      if (response.headers.isNotEmpty()) {
        put("headers", buildJsonObject {
          response.headers.fastForEach { header ->
            put(header.name, buildJsonObject {
              putNotNull("description", header.description)
              put("schema", buildJsonObject {
                val headerSchemaType = header.type
                metadataWriter.putNativeType(headerSchemaType.toNativeType().requireNotNull { "Invalid type: ${header.type}" }, SchemaPropertyConstraints.Required)
                metadataWriter.putKotlinSourceRef(headerSchemaType)
              })

              putExampleObjects(header.examples)
            })
          }
        })
      }

      response.body?.let { body ->
        put("content", buildJsonObject {
          put(body.mediaType.toString(), generateSchemaTypeRef(body.type, response.body.examples))
        })
      }
    }
  }

  /**
   * Is called for the request body or the response body for a single route or response.
   * Adds a reference to the schema type and the examples.
   */
  fun generateSchemaTypeRef(schemaType: SchemaDescriptor, examples: List<OpenApiExample>): JsonObject {
    return buildJsonObject {
      schemaGenerator.putSchemaTypeOrReference(schemaType)

      if (examples.isNotEmpty()) {
        put("examples", buildJsonObject {
          examples.fastForEachIndexed { index, example ->
            put(example.exampleId.value, buildJsonObject {
              putNotNull("summary", example.summary)
              putNotNull("description", example.description)
              metadataWriter.putKotlinSourceRefIfNotNull(example.originalType)

              // The example's own class names the concrete variant and is therefore the better
              // reference — but it is erased. A class with type parameters has no resolvable schema
              // and no resolvable serializer, so those examples fall back to the type the DSL
              // captured at the call site, which may be the declared body type rather than the
              // variant.
              val exampleType = example.exampleType.takeIf { example.example::class.typeParameters.isNotEmpty() }

              val exampleSchema = when (exampleType) {
                null -> example.originalType?.let { SchemaDescriptor.of(it) }
                else -> SchemaDescriptor.of(exampleType)
              }
              if (exampleSchema != null && exampleSchema.isComponent()) {
                putNotNull(AdditionalOpenApiKeys.TypeRef, exampleSchema.reference(ReferenceType.Discriminated).completeReference())
              }

              var exampleJsonElement: JsonElement = when (exampleType) {
                null -> AnySerializableRawSerializer.encodeToJsonElement(example.example, includeOptionals = true)
                else -> AnySerializableRawSerializer.encodeToJsonElement(example.example, exampleType, includeOptionals = true)
              }

              val serialName = example.example::class.findSerialName()
              if (serialName != null) {
                val jsonObject = exampleJsonElement as JsonObject
                val map = jsonObject.toMutableMap()
                map["type"] = JsonPrimitive(serialName)
                exampleJsonElement = JsonObject(map)
              }

              put("value", exampleJsonElement)
            })
          }
        })
      }
    }
  }

  /**
   * Generates the request body for a single route (path + HTTP method)
   */
  internal fun generateRequestBody(request: OpenApiRequest): JsonElement {
    return buildJsonObject {
      val requestBody = request.body
      if (requestBody != null) {
        requestBody.required?.let {
          put("required", it)
        }

        put("content", buildJsonObject {
          put(requestBody.mediaType.toString(), generateSchemaTypeRef(requestBody.type, requestBody.examples))
        })
      }
    }
  }
}
