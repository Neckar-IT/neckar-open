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
package it.neckar.rest.test

import assertk.*
import assertk.assertions.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.*
import io.ktor.http.*
import it.neckar.open.test.utils.isEqualToUsingJson
import it.neckar.rest.NoContentResponse
import it.neckar.rest.RestResponse
import it.neckar.rest.pagination.PageIndex
import it.neckar.rest.pagination.PageSize
import kotlin.reflect.KClass


fun Assert<HttpResponse>.isHttpStatusSuccess(): Unit = given { current ->
  if (current.status.isSuccess()) {
    return@given
  }

  fail("Expected success response, but got ${current.status}")
}

fun Assert<HttpResponse>.isOk(): Unit = given { current ->
  if (current.status == HttpStatusCode.OK) {
    return@given
  }

  fail("Expected 200 OK, but got ${current.status}")
}

fun Assert<HttpResponse>.isCreated(): Unit = given { current ->
  if (current.status == HttpStatusCode.Created) {
    return@given
  }

  fail("Expected 201 Created, but got ${current.status}")
}

fun Assert<HttpResponse>.isForbidden(): Unit = given { current ->
  if (current.status == HttpStatusCode.Forbidden) {
    return@given
  }

  fail("Expected 403 Forbidden, but got ${current.status}")
}

fun Assert<HttpResponse>.isNotFound(): Unit = given { current ->
  if (current.status == HttpStatusCode.NotFound) {
    return@given
  }

  fail("Expected 404 Not Found, but got ${current.status}")
}

fun Assert<HttpResponse>.isInternalServerError(): Unit = given { current ->
  if (current.status == HttpStatusCode.InternalServerError) {
    return@given
  }

  fail("Expected 500 Internal Server Error, but got ${current.status}")
}

fun Assert<HttpResponse>.isBadRequest(): Unit = given { current ->
  if (current.status == HttpStatusCode.BadRequest) {
    return@given
  }

  fail("Expected bad request, but got ${current.status}")
}

fun Assert<HttpResponse>.isUnauthorized(): Unit = given { current ->
  if (current.status == HttpStatusCode.Unauthorized) {
    return@given
  }

  fail("Expected 401 Unauthorized, but got ${current.status}")
}

/**
 * Asserts that [request] is turned away with 401, for a client that throws on a failed status —
 * the `expectSuccess` default of the test clients. Use [isUnauthorized] on a client that returns
 * the response instead.
 */
inline fun assertUnauthorized(request: () -> HttpResponse) {
  // A client that returns rather than throws has a status worth naming - "no exception" would not say which.
  val response = runCatching { request() }
    .getOrElse { failure ->
      if (failure !is ClientRequestException) throw failure
      failure.response
    }

  assertThat(response).isUnauthorized()
}

/**
 * Asserts that the response is a [RestResponse.BadResponse] of the specified subtype.
 */
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified ResponseType : RestResponse<DataType>, DataType, reified ResponseBadType>

  Assert<ResponseType>.isBad(): Assert<ResponseBadType>

  where ResponseBadType : RestResponse.BadResponse<DataType>,
        ResponseBadType : ResponseType {
  return transform(name) { current ->
    assertThat(current).isInstanceOf(RestResponse.BadResponse::class)
    return assertThat(current).isInstanceOf<ResponseBadType>()
  }
}

@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified ResponseType : RestResponse<DataType>, DataType, reified ResponseBadType>

  Assert<ResponseType>.isBadOfType(expectedType: KClass<ResponseBadType>): Assert<ResponseBadType>

  where ResponseBadType : RestResponse.BadResponse<DataType>,
        ResponseBadType : ResponseType {
  return transform(name) { current ->
    assertThat(current).isInstanceOf(RestResponse.BadResponse::class)
    assertThat(current).isInstanceOf(expectedType)
    return assertThat(current).isInstanceOf<ResponseBadType>()
  }
}

/**
 * Asserts that the response is a [RestResponse.GoodResponse].
 * Returns an assertion for [RestResponse.GoodResponse] to allow further assertions on the data.
 */
inline fun <reified ResponseType : RestResponse<DataType>, DataType>
  Assert<ResponseType>.isGood(): Assert<RestResponse.GoodResponse<DataType>> {
  return transform(name) { current ->
    assertThat(current).isInstanceOf(RestResponse.GoodResponse::class)
    @Suppress("UNCHECKED_CAST")
    return assertThat(current as RestResponse.GoodResponse<DataType>)
  }
}

@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified ResponseType : RestResponse<DataType>, DataType, reified ResponseGoodType>
  Assert<ResponseType>.isGoodPaginated(): Assert<ResponseGoodType>

  where ResponseGoodType : RestResponse.PaginatedGoodResponse<DataType>,
        ResponseGoodType : ResponseType {
  return transform(name) { current ->
    assertThat(current).isInstanceOf(RestResponse.PaginatedGoodResponse::class)
    return assertThat(current).isInstanceOf<ResponseGoodType>()
  }
}

/**
 * Verifies the pages size of a paged response
 */
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified ResponseGoodType, DataType>
  Assert<ResponseGoodType>.hasPageInfo(expectedPageIndex: PageIndex, expectedPageSize: PageSize, filteredElementsCount: Int, expectedTotalElementsCount: Int): Assert<ResponseGoodType>
  where ResponseGoodType : RestResponse.PaginatedGoodResponse<DataType> {
  return transform(name) { current ->
    assertThat(current) { "Response should be a PaginatedGoodResponse" }.isInstanceOf(RestResponse.PaginatedGoodResponse::class)
    val paginationInfo = current.paginationInfo
    assertThat(paginationInfo).isNotNull()

    requireNotNull(paginationInfo)

    assertThat(paginationInfo.pageIndex.value, "page index") { "Page index should be $expectedPageIndex but was ${paginationInfo.pageIndex}" }.isEqualTo(expectedPageIndex.value)
    assertThat(paginationInfo.pageSize.value, "page size") { "Page size should be $expectedPageSize but was ${paginationInfo.pageSize}" }.isEqualTo(expectedPageSize.value)
    assertThat(paginationInfo.filteredElementsCount, "filtered elements count") { "Filtered elements count should be $filteredElementsCount but was ${paginationInfo.filteredElementsCount}" }.isEqualTo(filteredElementsCount)
    assertThat(paginationInfo.totalElementsCount, "total elements count") { "Total elements count should be $expectedTotalElementsCount but was ${paginationInfo.totalElementsCount}" }.isEqualTo(expectedTotalElementsCount)

    return this
  }
}

/**
 * Asserts that the response's pagination info reports the given [expected] page count.
 */
@IgnorableReturnValue
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
fun <ResponseGoodType : RestResponse.PaginatedGoodResponse<*>>
  Assert<ResponseGoodType>.hasPageCount(expected: Int): Assert<ResponseGoodType> {
  given { current ->
    assertThat(current.paginationInfo.pageCount, "page count").isEqualTo(expected)
  }
  return this
}

/**
 * Asserts that the response's pagination info reports the given [expected] filtered elements count
 * (the total across all pages, not the size of the current page).
 */
@IgnorableReturnValue
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
fun <ResponseGoodType : RestResponse.PaginatedGoodResponse<*>>
  Assert<ResponseGoodType>.hasFilteredElementsCount(expected: Int): Assert<ResponseGoodType> {
  given { current ->
    assertThat(current.paginationInfo.filteredElementsCount, "filtered elements count").isEqualTo(expected)
  }
  return this
}

/**
 * Asserts that the response's pagination info reports the given [expected] total elements count
 * (including elements filtered out by the query).
 */
@IgnorableReturnValue
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
fun <ResponseGoodType : RestResponse.PaginatedGoodResponse<*>>
  Assert<ResponseGoodType>.hasTotalElementsCount(expected: Int): Assert<ResponseGoodType> {
  given { current ->
    assertThat(current.paginationInfo.totalElementsCount, "total elements count").isEqualTo(expected)
  }
  return this
}

/**
 * Asserts that the response's pagination info reports the given [expected] page index.
 */
@IgnorableReturnValue
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
fun <ResponseGoodType : RestResponse.PaginatedGoodResponse<*>>
  Assert<ResponseGoodType>.hasPageIndex(expected: PageIndex): Assert<ResponseGoodType> {
  given { current ->
    assertThat(current.paginationInfo.pageIndex, "page index").isEqualTo(expected)
  }
  return this
}

/**
 * Asserts that the response's pagination info reports the given [expected] page size.
 */
@IgnorableReturnValue
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
fun <ResponseGoodType : RestResponse.PaginatedGoodResponse<*>>
  Assert<ResponseGoodType>.hasPageSize(expected: PageSize): Assert<ResponseGoodType> {
  given { current ->
    assertThat(current.paginationInfo.pageSize, "page size").isEqualTo(expected)
  }
  return this
}

fun <DataType> Assert<RestResponse<DataType>>.isGoodWithData(expectedData: DataType) {
  given { current ->
    assertThat(current).isInstanceOf(RestResponse.GoodResponse::class)

    val data = current.asGood().data
    return assertThat(data, "data").isEqualToUsingJson(expectedData)
  }
}

@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified ResponseType : RestResponse<*>, reified ResponseBadType> Assert<ResponseType>.isBad(
  badObject: ResponseBadType,
): Assert<ResponseBadType> where ResponseBadType : RestResponse.BadResponse<*>,
                                 ResponseBadType : ResponseType = transform(name) { current ->
  assertThat(current).isInstanceOf(RestResponse.BadResponse::class)
  assertThat(current).isEqualTo(badObject)
  return assertThat(current).isInstanceOf<ResponseBadType>()
}

/**
 * Returns an assertion for the data object of a good response
 */
inline fun <DataType> Assert<RestResponse.GoodResponse<DataType>>.assertData(): Assert<DataType> {
  return transform(name) { current ->
    assertThat(current).isInstanceOf(RestResponse.GoodResponse::class)

    val data = current.data
    return assertThat(data)
  }
}

/**
 * Returns an assertion for the data object of a good response. Checks the type of the data object
 */
@Deprecated("No longer necessary, use assertData()")
inline fun <reified DataType : Any>
  Assert<RestResponse.GoodResponse<DataType>>.assertDataTyped(): Assert<DataType> {
  return transform(name) { current ->
    assertThat(current).isInstanceOf(RestResponse.GoodResponse::class)

    assertThat(current.data).isNotNull().isInstanceOf<DataType>()
    return assertThat(current.data)
  }
}

/**
 * Asserts that the response is a good response and the data is null
 */
@Deprecated("Inline!")
fun Assert<RestResponse<*>>.isEmptyGood(): Assert<NoContentResponse> {
  return isNoContent()
}

fun Assert<RestResponse<*>>.isNoContent(): Assert<NoContentResponse> {
  return transform(name) { current ->
    assertThat(current).isInstanceOf(NoContentResponse::class)
    require(current is NoContentResponse) { "Should be a empty good response" }
    return assertThat(current)
  }
}

@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
fun <ResponseGoodType : RestResponse.GoodResponse<*>>
  Assert<ResponseGoodType>.isDataEqualTo(expectedData: Any?) {
  given { current ->
    assertThat(current).isInstanceOf(RestResponse.GoodResponse::class)

    val data = current.data
    return assertThat(data).isEqualTo(expectedData)
  }
}
