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
package it.neckar.ktor.tracking

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import it.neckar.uuid.HasUuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlin.time.Instant

/**
 * Provides access to the current [Span] for adding attributes or events.
 */
inline fun span(block: Span.() -> Unit) {
  Span.current().apply(block)
}

/**
 * Sets a span attribute from a [HasUuid] value (e.g., value class IDs).
 */
fun Span.set(key: String, id: HasUuid) {
  setAttribute(key, id.uuid.toString())
}

/**
 * Sets a span attribute from an [Instant].
 */
fun Span.set(key: String, instant: Instant) {
  setAttribute(key, instant.toString())
}

/**
 * Sets a span attribute from an [Enum].
 */
fun Span.set(key: String, value: Enum<*>) {
  setAttribute(key, value.name)
}

fun Span.set(key: String, value: Boolean) {
  setAttribute(key, value)
}

fun Span.set(key: String, value: Int) {
  setAttribute(key, value.toLong())
}

fun Span.set(key: String, value: Long) {
  setAttribute(key, value)
}

fun Span.set(key: String, value: String) {
  setAttribute(key, value)
}

/**
 * Creates a child span with an auto-detected name derived from the caller's class and method.
 *
 * Uses [StackWalker] to reliably detect the caller's class and method name,
 * regardless of coroutine transformations. Handles coroutine continuations
 * where the frame shows `invokeSuspend` on a class like `BookSupport$find$1`.
 */
suspend fun <T> Tracer.withSpan(
  kind: SpanKind = SpanKind.INTERNAL,
  block: suspend (Span) -> T,
): T {
  return withSpan(resolveCallerSpanName(), kind, block)
}

private val thisClassName: String = object {}.javaClass.enclosingClass.name
private val stackWalker: StackWalker = StackWalker.getInstance()

/**
 * Resolves the caller's class and method name for automatic span naming.
 *
 * Walks the call stack past tracing infrastructure frames and extracts
 * the business class and method name from the first meaningful frame.
 */
private fun resolveCallerSpanName(): String {
  val frame: StackWalker.StackFrame = stackWalker.walk { frames ->
    frames
      .dropWhile { it.className.startsWith(thisClassName) }
      .findFirst()
      .orElse(null)
  } ?: return "Unknown.unknown"

  val rawClassName: String = frame.className.substringAfterLast('.')
  val className: String = rawClassName.substringBefore('$')

  val rawMethodName: String = if (frame.methodName == "invokeSuspend") {
    // Coroutine continuation: class name is like "BookSupport$find$1"
    rawClassName.substringAfter('$', "").substringBefore('$').ifEmpty { "unknown" }
  } else {
    frame.methodName
  }

  // Strip Kotlin name-mangling suffix (e.g., "getAll-dH0g2zk" → "getAll").
  // Kotlin appends "-<hash>" to JVM method names when value classes are used
  // in parameters or return types to avoid signature clashes after erasure.
  val methodName: String = rawMethodName.substringBefore('-')

  return "$className.$methodName"
}

/**
 * Executes [block] within a new child span named [name], with proper coroutine context propagation.
 *
 * Creates the span, propagates it through the coroutine context (via
 * `opentelemetry-extension-kotlin`), executes the block, and ends the span.
 *
 * ## Why `withContext(asContextElement())` instead of `makeCurrent()`?
 *
 * `makeCurrent()` sets the span as thread-local. When a coroutine suspends and resumes
 * on a different thread, the thread-local is lost — child spans would have no parent.
 *
 * `withContext(otelContext.asContextElement())` stores the OpenTelemetry Context in the
 * Kotlin coroutine context. The `opentelemetry-extension-kotlin` library patches
 * `Context.current()` to check the coroutine context first, so it works correctly
 * across suspension points.
 *
 * ## Example
 *
 * ```kotlin
 * // In a support class:
 * suspend fun find(id: MyEntity.Id, tenantId: Tenant.Id): MyEntity? {
 *   return tracer.withSpan { span ->
 *     span.set("myEntity.id", id)
 *     myEntityAccess.find(id, tenantId)  // suspend call — context propagates correctly
 *   }
 * }
 * ```
 *
 * The span automatically becomes a child of whatever span is currently active
 * in `Context.current()` — typically the HTTP span from `KtorServerTelemetry`.
 */
suspend fun <T> Tracer.withSpan(
  name: String,
  kind: SpanKind = SpanKind.INTERNAL,
  block: suspend (Span) -> T,
): T {
  val span: Span = spanBuilder(name)
    .setSpanKind(kind)
    .setParent(Context.current())
    .startSpan()

  val otelContext = Context.current().with(span)

  return withContext(otelContext.asContextElement()) {
    try {
      block(span)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      // Tracing wrapper must record all uncaught exceptions including Errors before re-throwing
      span.setStatus(StatusCode.ERROR, e.message ?: "error")
      span.recordException(e)
      throw e
    } finally {
      span.end()
    }
  }
}
