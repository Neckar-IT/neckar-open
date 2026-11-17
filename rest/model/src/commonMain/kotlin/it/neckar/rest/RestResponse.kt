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
package it.neckar.rest

import it.neckar.rest.RestResponse.GoodResponse
import it.neckar.rest.pagination.PaginationInfo
import kotlinx.serialization.Transient
import kotlin.contracts.contract

/**
 * Represents a REST response
 *
 * @param DataType the type that is returned by the [GoodResponse].
 * Might be nullable in some cases (e.g., no image available because none has been stored, but everything works)
 *
 * ## Discriminator identity rule
 * The `@SerialName` discriminator of every variant follows an identity rule:
 * - [GoodResponse] variants declare exactly `@SerialName("good")`
 * - [BadResponse] variants declare exactly `"bad/" + simpleName`, e.g. `data object NotFound` declares `@SerialName("bad/NotFound")`.
 *   Plain `@SerialName("bad")` is forbidden.
 *
 * The rule is enforced by the OpenAPI schema generator (`OpenApiSchemaGenerator.validateRestResponseDiscriminator`):
 * a violating variant fails every `*OpenApiTest` at build time.
 *
 * For sorting and filtering, use [RestResponseFilteredSorted]
 */
interface RestResponse<out DataType> {
  /**
   * Returns true if the result is a [GoodResponse]
   */
  @Transient
  val isGood: Boolean
    get() = this is GoodResponse

  fun asGood(): GoodResponse<DataType> {
    return this as GoodResponse<DataType>
  }

  /**
   * Returns true if the result is a [BadResponse]
   */
  @Transient
  val isBad: Boolean
    get() = this is BadResponse

  companion object;

  /**
   * Represents a good (fachlich erfolgreich) response.
   *
   * The `@SerialName` discriminator value is `"good"`. This is intentionally distinct from the HTTP
   * transport layer: a `GoodResponse` describes that the operation itself produced a valid result,
   * independent of the HTTP status code.
   */
  interface GoodResponse<out DataType> : RestResponse<DataType> {

    /**
     * The data that is provided by a good response
     */
    val data: DataType
  }

  /**
   * Represents a good response including pagination information
   */
  interface PaginatedGoodResponse<out DataType> : GoodResponse<DataType> {
    /**
     * The pagination information.
     * Never returns null, since all endpoints *must* support pagination
     */
    val paginationInfo: PaginationInfo
  }

  /**
   * Tagging interface for bad (fachlich negativ) responses.
   *
   * The `@SerialName` discriminator value is exactly `"bad/" + simpleName` of the variant class,
   * e.g. `data object NotFound` declares `@SerialName("bad/NotFound")`. A `BadResponse` is still a valid
   * business-level outcome (e.g. `NotFound`, `ValidationFailure`) and typically arrives with an
   * HTTP 2xx status — it is not a transport-level error.
   */
  interface BadResponse<out DataType> : RestResponse<DataType>
}

/**
 * Executes the provided block if this is a bad response
 */
fun <T : RestResponse<*>> T.ifBad(block: (response: RestResponse.BadResponse<*>?) -> Unit): T {
  contract {
    callsInPlace(block, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
  }

  if (isBad) {
    block(this as RestResponse.BadResponse<*>)
  }

  return this
}

/**
 * Returns the data if the response is a good response, otherwise null
 */
fun <T> RestResponse<T>.dataOrNull(): T? {
  return when (this) {
    is GoodResponse -> this.data
    else -> return null
  }
}
