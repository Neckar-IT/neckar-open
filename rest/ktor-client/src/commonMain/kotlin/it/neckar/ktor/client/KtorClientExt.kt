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
package it.neckar.ktor.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import it.neckar.open.http.Url
import it.neckar.open.http.UrlParameterName
import it.neckar.open.kotlin.serializers.verifyPlausibleForSerialization
import kotlin.coroutines.cancellation.CancellationException


/**
 * Extension methods for Ktor client
 */

/**
 * Posts the given object using JSON
 */
@IgnorableReturnValue
@Deprecated("Use absolute Url instead", level = DeprecationLevel.ERROR)
suspend inline fun <reified S : Any> HttpClient.postJson(url: Url, objectToSend: S, config: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return postJson(url as Url.Absolute, objectToSend, config)
}

@IgnorableReturnValue
suspend inline fun <reified S : Any> HttpClient.postJson(url: Url.Absolute, objectToSend: S, config: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  S::class.verifyPlausibleForSerialization()

  val response = post(url) {
    accept(ContentType.Application.Json)
    contentType(ContentType.Application.Json)

    setBody<S>(objectToSend)

    //Additional configuration
    config()
  }
  return response
}

@IgnorableReturnValue
suspend inline fun <reified S : Any> HttpClient.postJson(server: Url.Absolute, path: Url.Relative, objectToSend: S, config: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return postJson(server + path, objectToSend, config)
}

/**
 * Puts the given object using JSON
 */
@IgnorableReturnValue
suspend inline fun <reified S : Any> HttpClient.putJson(url: Url.Absolute, objectToSend: S, config: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  S::class.verifyPlausibleForSerialization()

  val response = put(url) {
    accept(ContentType.Application.Json)
    contentType(ContentType.Application.Json)

    setBody<S>(objectToSend)

    //Additional configuration
    config()
  }

  return response
}

@IgnorableReturnValue
suspend fun HttpClient.putJson(server: Url.Absolute, path: Url.Relative, objectToSend: Any, config: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return putJson(server + path, objectToSend, config)
}

/**
 * Gets an object from the given URL
 */
suspend inline fun <reified R> HttpClient.getJson(url: Url.Absolute, config: HttpRequestBuilder.() -> Unit = {}): R {
  val response = get(url) {
    accept(ContentType.Application.Json)

    //Additional configuration
    config()
  }

  return response.body<R>()
}

suspend inline fun HttpClient.getJsonString(url: Url.Absolute, config: HttpRequestBuilder.() -> Unit = {}): String {
  return getJson<String>(url, config)
}

suspend inline fun <reified R> HttpClient.getJson(server: Url.Absolute, path: Url.Relative, config: HttpRequestBuilder.() -> Unit = {}): R {
  return getJson(server + path, config)
}

/**
 * Runs [fetchFunction] and tells a delivered response apart from an unreachable server and a 401.
 * Every other exception propagates.
 */
suspend fun <T> tryFetch(fetchFunction: suspend () -> T): FetchOutcome<T> {
  return try {
    FetchOutcome.Delivered(fetchFunction())
  } catch (e: ClientRequestException) {
    when (e.response.status) {
      HttpStatusCode.Unauthorized -> FetchOutcome.Unauthorized
      else -> throw e
    }
  } catch (e: CancellationException) {
    throw e
  } catch (e: Throwable) {
    if (e.isNetworkFailure().not()) throw e
    FetchOutcome.Unreachable(e)
  }
}

/**
 * Runs [fetchFunction] and returns [onFailure] for an unreachable server and for a 401.
 */
suspend fun <T> safeTryFetch(onFailure: T, fetchFunction: suspend () -> T): T {
  return when (val outcome = tryFetch(fetchFunction)) {
    is FetchOutcome.Delivered -> outcome.value
    is FetchOutcome.Unreachable, FetchOutcome.Unauthorized -> onFailure
  }
}

@Deprecated("Use Url.Absolute instead", level = DeprecationLevel.ERROR)
suspend inline fun HttpClient.request(url: Url, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return request(url.value, block)
}

suspend inline fun HttpClient.request(url: Url.Absolute, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return request(url.value, block)
}

/**
 * Extension method that takes an url object
 */
@Deprecated("Use Url.Absolute instead", level = DeprecationLevel.ERROR)
suspend inline fun HttpClient.get(url: Url, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return get(url.value, block)
}

suspend inline fun HttpClient.get(url: Url.Absolute, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return get(url.value, block)
}

suspend inline fun HttpClient.get(server: Url.Absolute, path: Url.Relative, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return get(server + path, block)
}

@Deprecated("Use Url.Absolute instead", level = DeprecationLevel.ERROR)
suspend inline fun HttpClient.post(url: Url, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return post(url.value, block)
}

suspend inline fun HttpClient.post(url: Url.Absolute, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return post(url.value, block)
}

suspend inline fun HttpClient.post(server: Url.Absolute, path: Url.Relative, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return post(server + path, block)
}

@Deprecated("Use Url.Absolute instead", level = DeprecationLevel.ERROR)
suspend inline fun HttpClient.put(url: Url, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return put(url.value, block)
}

suspend inline fun HttpClient.put(url: Url.Absolute, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return put(url.value, block)
}

suspend inline fun HttpClient.put(server: Url.Absolute, path: Url.Relative, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return put(server + path, block)
}

@Deprecated("Use Url.Absolute instead", level = DeprecationLevel.ERROR)
suspend inline fun HttpClient.delete(url: Url, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return delete(url.value, block)
}

suspend inline fun HttpClient.delete(url: Url.Absolute, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return delete(url.value, block)
}

suspend inline fun HttpClient.delete(server: Url.Absolute, path: Url.Relative, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return delete(server + path, block)
}

@Deprecated("Use Url.Absolute instead", level = DeprecationLevel.ERROR)
suspend inline fun HttpClient.head(url: Url, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return head(url.value, block)
}

suspend inline fun HttpClient.head(url: Url.Absolute, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return head(url.value, block)
}

suspend inline fun HttpClient.head(server: Url.Absolute, path: Url.Relative, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return head(server + path, block)
}

@Deprecated("Use Url.Absolute instead", level = DeprecationLevel.ERROR)
suspend inline fun HttpClient.options(url: Url, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return options(url.value, block)
}

suspend inline fun HttpClient.options(url: Url.Absolute, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return options(url.value, block)
}

suspend inline fun HttpClient.options(server: Url.Absolute, path: Url.Relative, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
  return options(server + path, block)
}

/**
 * The cookies the [HttpCookies] plugin stored for the url.
 */
suspend fun HttpClient.cookies(url: Url.Absolute): List<Cookie> {
  return cookies(url.value)
}

/**
 * Appends a parameter to the request
 */
inline fun HttpRequestBuilder.parameter(key: UrlParameterName, value: Any?) {
  parameter(key.value, value)
}

/**
 * Returns the body as string - only if the response was successful.
 * Throws an [IllegalStateException] if the response was not successful.
 */
suspend fun HttpResponse.bodyAsTextSuccess(fallbackCharset: Charset = Charsets.UTF_8): String {
  if (status.isSuccess().not()) {
    throw IllegalStateException("Response was not successful: $status\n${bodyAsText(fallbackCharset)}")
  }
  return bodyAsText(fallbackCharset)
}
