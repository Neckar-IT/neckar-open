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
import it.neckar.lifeCycle.HasArchiveState
import it.neckar.lifeCycle.ArchiveState
import it.neckar.open.collections.fastForEach
import it.neckar.open.collections.fastForEachIndexed
import it.neckar.open.http.UrlParameterName
import it.neckar.open.http.UrlPathSegments
import it.neckar.open.kotlin.lang.findSupertype
import it.neckar.open.kotlin.lang.getAllSealedSubclasses
import it.neckar.open.kotlin.lang.hasCommonSealedAncestorWith
import it.neckar.open.kotlin.lang.isNotNothing
import it.neckar.rest.RestResponse
import it.neckar.rest.RestResponseFilteredSorted
import it.neckar.rest.io.HttpMethodSerializer
import it.neckar.rest.ktor.requestHasProbablyBody
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import it.neckar.rest.openapi.documentation.KotlinSourceInfo
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.OpenApiException
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.OpenApiTag

/**
 * Describes one route / endpoint
 */
data class OpenApiRoute(
  /**
   * The path segments for this route.
   */
  val path: UrlPathSegments,

  /**
   * The HTTP method for this route.
   */
  val method: @Serializable(with = HttpMethodSerializer::class) HttpMethod,

  /**
   * A short summary of what the operation does.
   */
  override val summary: String? = null,

  /**
   * A verbose explanation of the operation behavior.
   */
  override val description: String? = null,

  /**
   * The optional operation id for this route
   */
  val operationId: OperationId? = null,

  /**
   * The optional tag for this route. Used for sidebar grouping in Scalar.
   * When null, the generator's tag provider derives a tag from the path.
   */
  val tag: OpenApiTag? = null,

  /**
   * The request
   */
  val request: OpenApiRequest,

  /**
   * The responses per status code
   */
  val responses: List<OpenApiResponse>,

  /**
   * The lifecycle state for this route
   */
  override val archiveState: ArchiveState = ArchiveState.Active,

  /**
   * The category of the route.
   */
  val category: OpenApiMetaData.Category = OpenApiMetaData.Category.Default,

  /**
   * The source information for this route
   */
  val sourceInfo: KotlinSourceInfo?,
) : HasArchiveState, HasDescription {

  constructor(
    /**
     * The path segments for this route.
     */
    path: String,

    /**
     * The HTTP method for this route.
     */
    method: @Serializable(with = HttpMethodSerializer::class) HttpMethod,

    /**
     * A short summary of what the operation does.
     */
    summary: String? = null,

    /**
     * A verbose explanation of the operation behavior.
     */
    description: String? = null,

    /**
     * The optional operation id for this route
     */
    operationId: OperationId? = null,

    /**
     * The request
     */
    request: OpenApiRequest,

    /**
     * The responses per status code
     */
    responses: List<OpenApiResponse>,

    /**
     * The lifecycle state for this route
     */
    archiveState: ArchiveState = ArchiveState.Active,

    /**
     * The category of the route.
     */
    category: OpenApiMetaData.Category = OpenApiMetaData.Category.Default,

    /**
     * The source information for this route
     */
    sourceInfo: KotlinSourceInfo?,
  ) : this(UrlPathSegments.parse(path), method, summary, description, operationId, tag = null, request, responses, archiveState, category, sourceInfo)

  init {
    if (responses.isEmpty()) {
      throw OpenApiException(
        OpenApiException.Problem.MissingResponseBody,
        path = path.format(),
        method = method,
      )
    }

    if (method.requestHasProbablyBody()) {
      requireNotNull(request.body) {
        "Request body required for $method $path"
      }
    }
  }

  /**
   * Returns the best available response for this route.
   * Will return the lowest `2**` response, else null
   */
  val bestResponse: OpenApiResponse?
    get() {
      return responses
        .filter { it.statusCode.value < 300 } //Filter out all error responses
        .minByOrNull { it.statusCode.value } //Return the lowest status code
    }
}

/**
 * Creates a new open api route documentation
 */
fun openApiRoute(
  path: UrlPathSegments,
  method: HttpMethod,
  pathParameterNames: List<UrlParameterName>,
  /**
   * The category of the route
   */
  sourceInformation: KotlinSourceInfo? = null,
  category: OpenApiMetaData.Category = OpenApiMetaData.Category.Default,
  block: OpenApiRouteBuilder.() -> Unit,
): OpenApiRoute {
  return OpenApiRouteBuilder(path = path, method = method, pathParameterNames = pathParameterNames, sourceInformation = sourceInformation, category = category).apply(block).build()
}

fun openApiRoute(
  path: String,
  method: HttpMethod,
  pathParameterNames: List<UrlParameterName>,
  /**
   * The category of the route
   */
  sourceInformation: KotlinSourceInfo? = null,
  category: OpenApiMetaData.Category = OpenApiMetaData.Category.Default,
  block: OpenApiRouteBuilder.() -> Unit,
): OpenApiRoute {
  return OpenApiRouteBuilder(path = path, method = method, pathParameterNames = pathParameterNames, sourceInformation = sourceInformation, category = category).apply(block).build()
}

/**
 * Creates a (single) open api route documentation
 */
@OpenApiDsl
class OpenApiRouteBuilder(
  val path: UrlPathSegments,
  val method: HttpMethod,
  pathParameterNames: List<UrlParameterName>,
  val sourceInformation: KotlinSourceInfo?,
  /**
   * The category of the route.
   */
  var category: OpenApiMetaData.Category = OpenApiMetaData.Category.Default,
) : HasDescription {

  constructor(
    path: String,
    method: HttpMethod,
    pathParameterNames: List<UrlParameterName>,
    sourceInformation: KotlinSourceInfo?,
    /**
     * The category of the route.
     */
    category: OpenApiMetaData.Category = OpenApiMetaData.Category.Default,
  ) : this(UrlPathSegments.parse(path), method, pathParameterNames, sourceInformation, category)

  override var summary: String? = null
  override var description: String? = null

  /**
   * The operation id for this route.
   */
  var operationId: OperationId? = null

  /**
   * The tag for sidebar grouping in Scalar. When null, derived from the route path.
   */
  var tag: OpenApiTag? = null

  /**
   * Contains the request for this route.
   */
  val request: OpenApiRequestBuilder = OpenApiRequestBuilder(pathParameterNames) //default request with path parameter names

  /**
   * Contains the responses for this route.
   */
  private val responses: MutableList<OpenApiResponseBuilder> = mutableListOf()

  /**
   * Marks this request to not have a request body.
   * This is useful for PUT/POST requests that usually have a request body but in some cases don't.
   */
  fun noRequestBody() {
    request {
      //TODO remove body?
      body<Nothing>(SchemaDescriptor.EmptySchema) {
        noExamples()
      }
    }
  }

  /**
   * Marks this response to not have a response body.
   */
  fun noResponseBody() {
    response("Successful request with no content") {
      statusCode = HttpStatusCode.NoContent
      noExamples()
    }
  }

  /**
   * Configures the request for this route.
   */
  fun request(block: OpenApiRequestBuilder.() -> Unit) {
    request.apply(block)
  }

  /**
   * Adds a response to this route.
   */
  fun response(description: String, block: OpenApiResponseBuilder.() -> Unit) {
    val builder = OpenApiResponseBuilder(description).also { response ->
      response.block()
    }

    //Verify no duplicate status codes (default responses are keyed separately and ignored here)
    builder.statusCode.let { statusCode ->
      require(responses.none { !it.isDefault && it.statusCode == statusCode }) {
        "Response with status code $statusCode already exists for path '$path' and method '$method'."
      }
    }

    responses.add(builder)
  }

  /**
   * Adds the `default` (catch-all) response for this route - used for errors that are not tied to a
   * specific status code.
   */
  fun defaultResponse(description: String, block: OpenApiResponseBuilder.() -> Unit) {
    val builder = OpenApiResponseBuilder(description).also { response ->
      response.block()
    }
    builder.isDefault = true

    require(responses.none { it.isDefault }) {
      "A default response already exists for path '$path' and method '$method'."
    }

    responses.add(builder)
  }

  var archiveState: ArchiveState = ArchiveState.Active

  fun build(): OpenApiRoute {
    try {
      return OpenApiRoute(
        path = path,
        method = method,
        summary = summary,
        description = description,
        operationId = operationId,
        tag = tag,
        request = request.build(),
        responses = buildResponses(),
        archiveState = archiveState,
        category = category,
        sourceInfo = sourceInformation,
      )
    } catch (e: OpenApiException) {
      //Throw the exception with the path and method information
      throw OpenApiException(problem = e.problem, path = path.format(), method = method, sourceLocation = e.sourceLocation, cause = e)
    }
  }

  /**
   * Builds the responses for this route.
   */
  private fun buildResponses(): List<OpenApiResponse> {
    //Verify that all responses have a unique key (status code, or "default" for the catch-all)
    return responses.map { it.build() }.also { built ->
      val grouped = built.groupBy { if (it.isDefault) "default" else it.statusCode.value.toString() }

      grouped.forEach { (key, responsesWithSameKey) ->
        require(responsesWithSameKey.size == 1) {
          "Responses must have unique keys but found multiple responses with [$key]: ${responsesWithSameKey.map { it.description }}"
        }
      }
    }
  }

  /**
   * Shortcut to add a request body
   */
  inline fun <reified BodyType : Any> requestBody(noinline block: OpenApiSimpleBodyBuilder<BodyType>.() -> Unit) {
    request {
      body<BodyType>(block)
    }
  }

  /**
   * Sets the response to the generic type. Marks the response as having no examples.
   */
  inline fun <reified ResponseType : RestResponse<*>> respondingNoExample() {
    responding<ResponseType> {
      noExamples()
    }
  }

  inline fun <reified ResponseType : RestResponse<*>> responding(
    /**
     * The success example (required).
     * If no example can be provided, call [responding] with a lambda and call [OpenApiSimpleBodyBuilder.noExamples]
     */
    successExamples: List<ResponseType>,
    /**
     * The optional failure examples.
     */
    failureExamples: List<ResponseType> = emptyList(),

    /**
     * Optional block to configure the response further
     */
    noinline block: OpenApiResponseBuilder.() -> Unit = {}
  ) {
    successExamples.fastForEach { successExample ->
      //Use reflection to find the sealed interface of success and failure - which must be the same!
      require(successExample.isGood) { "Success example must be a success but was ${successExample::class}" }
    }

    failureExamples.fastForEach { failureExample ->
      require(failureExample.isBad) { "Failure example must be a failure but was ${failureExample::class}" }
    }

    //Verify that all success examples have the same sealed interface as the failure examples
    successExamples.fastForEach { successExample ->
      failureExamples.fastForEach { failureExample ->
        require(successExample::class.hasCommonSealedAncestorWith(failureExample::class)) {
          "Good (${successExample::class}) and Bad (${failureExample::class}) must have the same sealed interface."
        }
      }
    }

    response(description = "Responds with Good or Bad types") {
      body<ResponseType> {
        successExamples.fastForEachIndexed { index, value ->
          val exampleIdAsString: String = if (index == 0) "good" else "good-$index"
          example(exampleIdAsString = exampleIdAsString, example = value)
        }

        failureExamples.fastForEachIndexed { index, value ->
          val exampleIdAsString: String = if (index == 0) "bad" else "bad-$index"
          example(exampleIdAsString = exampleIdAsString, example = value)
        }
      }

      block()
    }

    appendSortFilter(ResponseType::class)
    appendPagination(ResponseType::class)
  }

  /**
   * Sets the response to the generic type.
   * Uses two examples to infer the type of the response.
   */
  //do *not* add "T" to avoid having to specify the type everywhere
  inline fun <reified ResponseType : RestResponse<*>> responding(
    /**
     * The success example (required).
     * If no example can be provided, call [responding] with a lambda and call [OpenApiSimpleBodyBuilder.noExamples]
     */
    successExample: ResponseType,
    failureExample: ResponseType? = null,

    /**
     * Optional block to configure the response further
     */
    noinline block: OpenApiResponseBuilder.() -> Unit = {}
  ) {
    responding<ResponseType>(
      successExamples = listOf(successExample),
      failureExamples = listOfNotNull(failureExample),
      block = block
    )
  }

  /**
   * Sets the response to the generic type.
   * Uses two examples to infer the type of the response.
   */
  //do *not* add "T" to avoid having to specify the type everywhere
  inline fun <reified ResponseType : RestResponse<*>> responding(
    /**
     * The success example (required).
     * If no example can be provided, call [responding] with a lambda and call [OpenApiSimpleBodyBuilder.noExamples]
     */
    successExample: ResponseType,
    failureExamples: List<ResponseType>,

    /**
     * Optional block to configure the response further
     */
    noinline block: OpenApiResponseBuilder.() -> Unit = {}
  ) {
    responding<ResponseType>(
      successExamples = listOf(successExample),
      failureExamples = failureExamples,
      block = block
    )
  }

  /**
   * Sets the response to the generic type.
   * This is a simplified version of [responding] that allows the call with just the generic type.
   */
  inline fun <reified ResponseType : RestResponse<*>> responding(
    noinline block: OpenApiSimpleBodyBuilder<ResponseType>.() -> Unit
  ) {
    response(description = "Responds with Good or Bad types") {
      body<ResponseType>(block)
    }

    appendSortFilter(ResponseType::class)
    appendPagination(ResponseType::class)
  }

  fun appendSortFilter(responseType: KClass<out RestResponse<*>>) {
    //Check if there is any information about sorting/filtering
    val restResponseFilteredSortedType = responseType.findSupertype<RestResponseFilteredSorted<*, *, *>>() ?: return

    val filterByType = restResponseFilteredSortedType.arguments[1].type
    val sortByType = restResponseFilteredSortedType.arguments[2].type

    if (filterByType != null && filterByType.isNotNothing()) {
      filteredBy(filterByType)
    }

    if (sortByType != null && sortByType.isNotNothing()) {
      sortedBy(sortByType)
    }
  }

  /**
   * Appends the pagination parameters - if the success message is paginated
   */
  fun <ResponseType : RestResponse<*>> appendPagination(responseType: KClass<ResponseType>) {
    require(responseType.isSealed) {
      "Response type must be sealed"
    }

    val successTypes = responseType.getAllSealedSubclasses().filter {
      it.isSubclassOf(RestResponse.GoodResponse::class)
    }

    val hasPaginatedResponse = successTypes.any {
      it.isSubclassOf(RestResponse.PaginatedGoodResponse::class)
    }

    if (hasPaginatedResponse) {
      paginated()
    }
  }
}
