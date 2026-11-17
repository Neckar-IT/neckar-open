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
import it.neckar.rest.io.HttpStatusCodeSerializer
import kotlinx.serialization.Serializable
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.OpenApiException


/**
 * Represents the documentation for a response
 */
data class OpenApiResponse(
  /**
   * The (optional) body of the response
   */
  val body: OpenApiSimpleBody? = null,

  override val summary: String? = null,
  /**
   * Description is required
   */
  override val description: String,

  /**
   * The HTTP status code of the response
   */
  val statusCode: @Serializable(with = HttpStatusCodeSerializer::class) HttpStatusCode = HttpStatusCode.OK,

  /**
   * The headers
   */
  val headers: List<OpenApiHeader> = emptyList(),

  /**
   * When true this response is emitted under the `default` key (catch-all) instead of [statusCode].
   */
  val isDefault: Boolean = false,
) : HasDescription {

  companion object {
    val empty: OpenApiResponse = OpenApiResponse(description = "Empty Response")
  }
}

@OpenApiDsl
class OpenApiResponseBuilder(var description: String) {
  var body: OpenApiSimpleBodyBuilder<*>? = null
  var summary: String? = null
  var statusCode: HttpStatusCode = HttpStatusCode.OK

  /**
   * When true this response is emitted under the `default` key (catch-all) rather than a status code.
   */
  var isDefault: Boolean = false

  private val headers = mutableListOf<OpenApiHeaderBuilder<*>>()

  @IgnorableReturnValue
  fun <ExampleType : Any> body(body: OpenApiSimpleBodyBuilder<ExampleType>?): OpenApiResponseBuilder = apply { this.body = body }

  @IgnorableReturnValue
  fun <ExampleType : Any> body(type: SchemaDescriptor, block: OpenApiSimpleBodyBuilder<ExampleType>.() -> Unit): OpenApiResponseBuilder {
    return apply {
      this.body = OpenApiSimpleBodyBuilder<ExampleType>(type)
        .apply(block)
    }
  }

  @IgnorableReturnValue
  inline fun <reified T : Any> body(noinline block: OpenApiSimpleBodyBuilder<T>.() -> Unit = {}): OpenApiResponseBuilder {
    return body(SchemaDescriptor.of<T>(), block)
  }

  fun summary(summary: String?): OpenApiResponseBuilder = apply { this.summary = summary }

  fun description(description: String): OpenApiResponseBuilder = apply { this.description = description }

  fun statusCode(statusCode: HttpStatusCode): OpenApiResponseBuilder = apply { this.statusCode = statusCode }

  /**
   * Adds multiple headers to the response.
   */
  fun header(header: OpenApiHeaderBuilder<*>): OpenApiResponseBuilder {
    this.headers.add(header)
    return this
  }

  /**
   * Adds a header to the response.
   */
  fun <ExampleType : Any> header(name: String, type: SchemaDescriptor.NativePrimitiveSchema, block: OpenApiHeaderBuilder<ExampleType>.() -> Unit): OpenApiResponseBuilder {
    val builder = OpenApiHeaderBuilder<ExampleType>(name, type)
    builder.block()
    this.headers.add(builder)
    return this
  }

  /**
   * Adds multiple headers to the response.
   */
  fun headers(headers: List<OpenApiHeaderBuilder<*>>): OpenApiResponseBuilder {
    this.headers.addAll(headers)
    return this
  }

  /**
   * If set to true, no examples must be provided
   */
  private var noExamples: Boolean = false

  fun noExamples() {
    require(body == null) { "body already configured" }
    noExamples = true
  }

  private fun verifyBody() {
    if (noExamples) return
    if (body != null) return

    throw OpenApiException(OpenApiException.Problem.MissingExample)
  }

  fun build(): OpenApiResponse {
    verifyBody()
    return OpenApiResponse(body?.build(), summary, description, statusCode, headers.build(), isDefault)
  }
}

fun List<OpenApiHeaderBuilder<*>>.build(): List<OpenApiHeader> {
  return this.map { it -> it.build() }
}

fun List<OpenApiHeader>.toBuilders(): List<OpenApiHeaderBuilder<*>> {
  return this.map {
    it.toBuilder()
  }
}

private fun OpenApiHeader.toBuilder(): OpenApiHeaderBuilder<Any> = OpenApiHeaderBuilder(this)


