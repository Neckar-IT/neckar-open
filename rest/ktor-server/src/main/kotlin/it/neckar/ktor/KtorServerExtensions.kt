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
package it.neckar.ktor

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.getOrFail
import io.ktor.util.pipeline.*
import io.opentelemetry.api.trace.Span
import it.neckar.common.featureflags.FeatureFlag
import it.neckar.common.featureflags.FeatureFlags
import it.neckar.ktor.tracking.MDCKeys
import it.neckar.ktor.tracking.callIdMdc
import it.neckar.ktor.tracking.mdc
import it.neckar.open.collections.fastForEach
import it.neckar.open.environment.ServerEnvironment
import it.neckar.open.exceptions.PermissionDeniedException
import it.neckar.open.http.Url
import it.neckar.open.http.UrlParameterName
import it.neckar.open.http.UrlPathSegment
import it.neckar.open.http.UrlPathSegments
import it.neckar.open.http.UrlPattern
import it.neckar.ktor.proxy.toUrl
import it.neckar.open.kotlin.lang.get
import it.neckar.open.kotlin.lang.random
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.rest.tracking.XClientId
import it.neckar.rest.tracking.XClientRole
import it.neckar.uuid.NanoId
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Extension methods for the Ktor server
 */
@Deprecated("Use HttpParameterName instead")
fun Parameters.getParamSafe(paramName: String): String {
  return getParamSafe(UrlParameterName(paramName))
}

/**
 * Returns the value for [paramName], throwing Ktor's [io.ktor.server.plugins.MissingRequestParameterException]
 * (a [BadRequestException]) if the parameter is missing. Type-safe overload of Ktor's `getOrFail` that
 * accepts the strongly-typed [UrlParameterName] used throughout this codebase.
 */
fun Parameters.getOrFail(paramName: UrlParameterName): String = getOrFail(paramName.value)

@Deprecated("Use getOrFail instead", ReplaceWith("getOrFail(paramName)"))
fun Parameters.getParamSafe(paramName: UrlParameterName): String = getOrFail(paramName)

fun Parameters.getParam(paramName: UrlParameterName): String? {
  return this[paramName.value]
}

fun UrlParameterName.get(parameters: Parameters): String? {
  return parameters[this.value]
}

/**
 * A missing or unparseable value throws a [BadRequestException] (StatusPages: 400), where a bare
 * `Uuid.parse` throws an `IllegalArgumentException` (500).
 */
fun Parameters.getUuidOrFail(paramName: UrlParameterName): Uuid {
  val raw = getOrFail(paramName)
  return Uuid.parseOrNull(raw) ?: throw ParameterConversionException(paramName.value, "Uuid")
}

/** Throws like [getUuidOrFail]. */
fun <Id> Parameters.idFromPath(name: UrlParameterName, factory: (Uuid) -> Id): Id = factory(getUuidOrFail(name))

/** Throws like [getUuidOrFail], where a bare `toLong()` throws a `NumberFormatException` (500). */
fun Parameters.getLongOrFail(paramName: UrlParameterName): Long {
  val raw = getOrFail(paramName)
  return raw.toLongOrNull() ?: throw ParameterConversionException(paramName.value, "Long")
}

/** Throws like [getUuidOrFail], where a bare `Instant.parse` throws an `IllegalArgumentException` (500). */
fun Parameters.getInstantOrFail(paramName: UrlParameterName): Instant {
  val raw = getOrFail(paramName)
  return Instant.parseOrNull(raw) ?: throw ParameterConversionException(paramName.value, "Instant")
}

/**
 * Extracts the UUID parameter ("uuid")
 */
fun Parameters.uuid(): Uuid = getUuidOrFail(UrlParameterName.uuid)

/**
 * Returns the value of the parameter with the given name from the call's parameters.
 */
fun UrlParameterName.fromCall(call: RoutingCall): String {
  return call.parameters.getParamSafe(this)
}


@Deprecated("required?")
fun <T> T?.ensureNotNull(messageProvider: () -> String): T {
  if (this == null) {
    throw NotFoundException(messageProvider())
  }

  return this
}

/**
 * Helper method to create route from a [Url]
 */
@Deprecated("Use Url.RelativeTemplate", level = DeprecationLevel.ERROR)
inline fun Route.route(path: Url, noinline build: Route.() -> Unit): Route {
  return route(path.value, build)
}

@IgnorableReturnValue
inline fun Route.route(path: UrlPattern, noinline build: Route.() -> Unit): Route {
  return route(path.value, build)
}

@IgnorableReturnValue
@Deprecated("Use Url.RelativeTemplate", level = DeprecationLevel.ERROR)
inline fun Route.get(path: Url, noinline body: suspend RoutingContext.() -> Unit): Route {
  return get(path.value, body)
}

@IgnorableReturnValue
inline fun Route.get(path: UrlPattern, noinline body: suspend RoutingContext.() -> Unit): Route {
  return get(path.value, body)
}

@IgnorableReturnValue
@Deprecated("Use Url.RelativeTemplate", level = DeprecationLevel.ERROR)
inline fun Route.put(path: Url, noinline body: suspend RoutingContext.() -> Unit): Route {
  return put(path.value, body)
}

@IgnorableReturnValue
inline fun Route.put(path: UrlPattern, noinline body: suspend RoutingContext.() -> Unit): Route {
  return put(path.value, body)
}

@IgnorableReturnValue
@Deprecated("Use Url.RelativeTemplate", level = DeprecationLevel.ERROR)
inline fun Route.post(path: Url, noinline body: suspend RoutingContext.() -> Unit): Route {
  return post(path.value, body)
}

@IgnorableReturnValue
inline fun Route.post(path: UrlPattern, noinline body: suspend RoutingContext.() -> Unit): Route {
  return post(path.value, body)
}

@IgnorableReturnValue
@Deprecated("Use Url.RelativeTemplate", level = DeprecationLevel.ERROR)
inline fun Route.delete(path: Url, noinline body: suspend RoutingContext.() -> Unit): Route {
  return delete(path.value, body)
}

@IgnorableReturnValue
inline fun Route.delete(path: UrlPattern, noinline body: suspend RoutingContext.() -> Unit): Route {
  return delete(path.value, body)
}

/**
 * Collects all routes
 */
fun Pipeline<*, PipelineCall>.collectRoutes(): List<Route> {
  return plugin(RoutingRoot).collectRoutes()
}

/**
 * Collects all routes - including the root and the intermediate routes
 */
fun Pipeline<*, PipelineCall>.collectAllRoutes(): List<Route> {
  return plugin(RoutingRoot).collectAllRoutes()
}

/**
 * Collects all routes - including the root and the intermediate routes
 */
fun RoutingRoot.collectAllRoutes(): List<Route> {
  fun allRoutes(root: RoutingNode): List<Route> {
    return listOf(root) + root.children.flatMap { allRoutes(it) }
  }

  return allRoutes(this as RoutingNode)
}

/**
 * Returns all routes that have an endpoint - not the intermediate routes
 */
fun RoutingRoot.collectRoutes(): List<Route> {
  return collectAllRoutes()
    .filterIsInstance<RoutingNode>()
    .filter { route ->
      route.selector is HttpMethodRouteSelector
    }
}

/**
 * Dumps all routes as string. This method uses the [KtorService] associated with the application to determine the base URL.
 *
 * Use `dumpRoutesWithDoc` if you want to include the documentation (module `ktor-server-openapi`)
 */
fun Application.dumpRoutes(
  /**
   * If true, the routes are sorted alphabetically.
   */
  sortedAlphabetically: Boolean = false
): String {
  val serverUrl = ktorService.baseUrl
  return dumpRoutes(serverUrl = serverUrl, sortedAlphabetically = sortedAlphabetically)
}

/**
 * Dumps all routes as string. Extracts the base URL from the [call] to support reverse proxies.
 * Use `dumpRoutesWithDoc` if you want to include the documentation (module `ktor-server-openapi`).
 */
fun Application.dumpRoutes(
  call: ApplicationCall,
  sortedAlphabetically: Boolean = false
): String {
  // origin uses X-Forwarded-* headers when ForwardedHeaders plugin is installed,
  // otherwise returns direct connection values (scheme, host, port from the actual request)
  val serverUrl = call.request.origin.toUrl()

  return dumpRoutes(serverUrl = serverUrl, sortedAlphabetically = sortedAlphabetically)
}

context(ktorService: KtorService)
fun Pipeline<Unit, PipelineCall>.dumpRoutes(
  /**
   * If true, the routes are sorted alphabetically.
   */
  sortedAlphabetically: Boolean = false
): String {
  val serverUrl = ktorService.baseUrl

  return dumpRoutes(serverUrl = serverUrl, sortedAlphabetically = sortedAlphabetically)
}

/**
 * Returns the routes using the given [serverUrl].
 * Usually the method with the [KtorService] context is used.
 */
fun Pipeline<Unit, PipelineCall>.dumpRoutes(serverUrl: Url.Absolute, sortedAlphabetically: Boolean = false): String {
  val routing = plugin(RoutingRoot)

  return buildString {
    routing.collectRoutes()
      .map { it.toString() }
      .let { if (sortedAlphabetically) it.sorted() else it }
      .fastForEach { route ->
        appendLine(serverUrl + route)
      }
  }
}

/**
 * Installs default content negotiation with JSON
 */
fun Application.installJson(configuration: JsonBuilder.() -> Unit = {}) {
  install(ContentNegotiation) {
    json(
      Json {
        configuration()
      }
    )
  }
}


/**
 * Installs call ID handling on [HttpHeaders.XRequestId] (`X-Request-Id`): reuses an incoming id,
 * echoes it in the response, and generates a 10-char alphanumeric NanoID when the request has none.
 *
 * The [verify] alphabet deliberately also accepts `-` so an upstream-issued **UUIDv4** or **ULID**
 * (e.g. a request id injected by the Traefik edge) passes validation and is reused as the
 * call id. Without the hyphen a UUID would be rejected and Ktor would generate a *different* id,
 * breaking Traefik↔backend log correlation. Self-generated ids stay alphanumeric NanoIDs.
 */
fun Application.installCallId(nanoIdGenerator: NanoId.NanoIdGenerator = NanoId.generator(10, NanoId.AlphaNumeric)) {
  install(CallId) {
    // header() calls both retrieveFromHeader() and replyToHeader() internally
    header(HttpHeaders.XRequestId)

    // Generator alphabet plus '-' so an upstream UUID/ULID request id validates and is reused.
    verify(nanoIdGenerator.alphabet.concatToString() + "-")
    generate {
      nanoIdGenerator.generate().value.requireNotNull()
    }
  }
}

/**
 * Installs call logging
 */
fun Application.installCallLogging(
  level: Level = Level.INFO,
  logger: Logger = LoggerFactory.getLogger("Requests"),
  filter: (ApplicationCall) -> Boolean = { call ->
    call.request.uri.substringBefore('?').endsWith("heartbeat").not()
  },
) {
  install(CallLogging) {
    this.level = level
    this.logger = logger
    this.filter { call -> filter(call) }

    //Adds the call id to the MDC - is extracted from the Call attributes (see `ApplicationCall.callId`)
    callIdMdc(MDCKeys.CallId)

    mdc(MDCKeys.ClientId) { call ->
      call.request.header(HttpHeaders.XClientId)?.also { clientId ->
        val span = Span.current()
        if (span.spanContext.isValid) {
          span.setAttribute("client.id", clientId)
        }
      }
    }
    mdc(MDCKeys.ClientRole) { call ->
      call.request.header(HttpHeaders.XClientRole)?.also { clientRole ->
        val span = Span.current()
        if (span.spanContext.isValid) {
          span.setAttribute("client.role", clientRole)
        }
      }
    }

    format { call ->
      val status = call.response.status()
      val location = call.response.headers[HttpHeaders.Location]
      val locationString = if (location != null) " -> $location" else ""

      val callId = MDCKeys.CallId.get()
      val clientId = MDCKeys.ClientId.get()
      val clientRole = MDCKeys.ClientRole.get()
      // OpenTelemetry trace ID — present when tracing is configured.
      // Appended to the log line so the trace ID is visible in console output,
      // not just in Loki JSON (where loki4j includes it automatically via MDC).
      val traceId = MDCKeys.TraceId.get()
      val traceString = if (traceId != null) " trace=$traceId" else ""

      "[$clientId<$clientRole>:$callId] ${status as HttpStatusCode}: ${call.request.httpMethod.value} - ${call.request.path()} in ${call.request.call.processingTimeMillis()}ms$locationString$traceString"
    }
  }
}

fun Application.installCompression() {
  install(Compression) {
    default()
  }
}

/**
 * Installs the [IgnoreTrailingSlash] feature
 */
fun Application.installIgnoreTailingSlash() {
  install(IgnoreTrailingSlash)
}

/**
 * Returns true if this request is a CORS preflight check
 */
val ApplicationRequest.isCorsRequest: Boolean
  get() {
    return httpMethod == HttpMethod.Options && header("Access-Control-Request-Method") != null
  }

@Deprecated("No longer required")
fun PipelineContext<Unit, ApplicationCall>.extractFeatureFlags(): FeatureFlags {
  return this.context.extractFeatureFlags()
}

/**
 * Extracts the feature flags from the routing context.
 */
fun RoutingContext.extractFeatureFlags(): FeatureFlags {
  return call.extractFeatureFlags()
}

/**
 * Extracts the feature flags from the request
 */
fun ApplicationCall.extractFeatureFlags(): FeatureFlags {
  return request.extractFeatureFlags()
}

/**
 * Extracts the feature flags from the current request.
 *
 * Uses header *and* url parameter, plus the default flags [FeatureFlagsPlugin] keeps for the application. The results are then merged
 */
fun ApplicationRequest.extractFeatureFlags(): FeatureFlags {
  val flagsFromUrlParameter = FeatureFlags.decodeFromString(queryParameters[FeatureFlags.QueryParamName])
  val flagsFromHeader = FeatureFlags.decodeFromString(header(FeatureFlags.HeaderKey))
  val defaultFlags = call.application.attributes.getOrNull(DefaultFeatureFlagsAttribute) ?: FeatureFlags.empty

  return defaultFlags + flagsFromHeader + flagsFromUrlParameter
}

@Deprecated("Use handle() instead", replaceWith = ReplaceWith("handle()"))
context(_: RoutingContext)
suspend fun FeatureFlags.handleFeatureFlags() {
  handle()
}

/**
 * An artificial delay in milliseconds for DevTools diagnostics, e.g. `42ms`; one value per delay of [FeatureFlagsPlugin] or [handle].
 */
const val SimulatedDelayHeader: String = "NeckarIT-Simulated-Delay"

fun ApplicationCall.appendSimulatedDelay(delay: Duration) {
  if (delay.isPositive()) {
    response.headers.append(SimulatedDelayHeader, "${delay.inWholeMilliseconds}ms")
  }
}

/**
 * Applies the simulation effects in an application without [FeatureFlagsPlugin]: delays the response if [FeatureFlag.slowServer]
 * or [FeatureFlag.realisticLatency] is enabled, forces an internal server error if [FeatureFlag.forceServerError] is enabled.
 * With [FeatureFlagsPlugin] installed it applies nothing, because the plugin applies every effect itself.
 */
context(routingContext: RoutingContext)
@IgnorableReturnValue
suspend fun FeatureFlags.handle(): FeatureFlags {
  val call = routingContext.call
  if (call.application.pluginOrNull(FeatureFlagsPlugin) != null) {
    return this
  }
  call.appendSimulatedDelay(delayIfSlowServerEnabled())
  call.appendSimulatedDelay(delayIfRealisticLatencyEnabled())
  forceInternalErrorIfEnabled()
  return this
}

/**
 * Delays the response by 1500–2500 ms if [FeatureFlag.slowServer] is enabled.
 */
suspend fun FeatureFlags.delayIfSlowServerEnabled(): Duration {
  if (contains(FeatureFlag.slowServer)) {
    val delay = random[LongRange(1500, 2500)].milliseconds
    logger.debug("Delaying response by $delay because of FeatureFlag [${FeatureFlag.slowServer}]")
    delay(delay)
    return delay
  }
  return Duration.ZERO
}

/**
 * Adds 20–60 ms if [FeatureFlag.realisticLatency] is enabled: a DSL `ping` to `patterns.neckar.it` (17.7 ms average,
 * 49.2 ms maximum, 2026-10-13) plus TLS, proxies and the server.
 */
suspend fun FeatureFlags.delayIfRealisticLatencyEnabled(): Duration {
  if (contains(FeatureFlag.realisticLatency)) {
    val delay = random[LongRange(20, 60)].milliseconds
    logger.debug("Delaying response by $delay because of FeatureFlag [${FeatureFlag.realisticLatency}]")
    delay(delay)
    return delay
  }
  return Duration.ZERO
}

/**
 * This method simulates an internal server error if the corresponding feature flag is enabled
 */
fun FeatureFlags.forceInternalErrorIfEnabled() {
  if (contains(FeatureFlag.forceServerError)) {
    logger.info("Forcing Client Request to fail Internal on server side because of [${FeatureFlag.forceServerError}]")
    //Message text is asserted verbatim by RefreshAccessTokenIT / AuthServiceErrorHandlingTest in the
    //maintenance-only Lizergy auth-service - keep it stable, the type is what carries the meaning here.
    throw SimulatedServerErrorException("Simulated Error caused by forceInternalError FeatureFlag")
  }
}

/**
 * Responds with OK and sets the ETag using [hashCode] for the provided result.
 */
suspend inline fun <reified T : Any> ApplicationCall.withEtagByHashCode(result: T, statusCode: HttpStatusCode = HttpStatusCode.OK) {
  response.headers.append(HttpHeaders.ETag, result.hashCode().toString())
  respond<T>(statusCode, result)
}

/**
 * Returns the authorization header.
 * Throws a [PermissionDeniedException] if no authorization header is found.
 *
 * Attention: Use the [io.ktor.server.auth.Authentication] plugin if possible!
 *
 * Attention: Returns the raw header value. You might want to use `jwtToken()` instead!
 */
fun ApplicationRequest.authorizationHeader(): String {
  return headers[HttpHeaders.Authorization] ?: throw PermissionDeniedException("No authorization header found")
}

/**
 * Returns all selectors for this route, starting from the root
 */
fun Route.collectSelectors(): List<RouteSelector> {
  // Every Route created by routing is a RoutingNode, which carries the selector since Ktor 3.0.1.
  this as RoutingNode

  val selectors = mutableListOf<RouteSelector>()

  var current: RoutingNode? = this
  while (current != null) {
    selectors.add(0, current.selector)
    current = current.parent
  }
  return selectors
}

/**
 * Extracts all route selectors
 */
fun List<RouteSelector>.collectPath(): List<String> {
  return mapNotNull {
    when (it) {
      is RootRouteSelector -> {
        "/"
      }

      is PathSegmentConstantRouteSelector -> {
        it.value
      }

      is ParameterRouteSelector -> {
        "{${it.name}}"
      }

      is PathSegmentParameterRouteSelector -> {
        "{${it.name}}"
      }

      else -> {
        null
      }
    }
  }
}

/**
 * Collects the path pattern for this route.
 */
fun List<RouteSelector>.collectPathSegments(): UrlPathSegments {
  val pathSegments: List<UrlPathSegment> = this.mapNotNull {
    when (it) {
      is RootRouteSelector -> {
        null
      }

      is AuthenticationRouteSelector,
      is HttpMultiAcceptRouteSelector,
      is TransparentRouteSelector,
      is HttpMethodRouteSelector -> {
        null
      }

      is PathSegmentConstantRouteSelector -> {
        UrlPathSegment.Segment(it.value)
      }

      is ParameterRouteSelector -> {
        // Query parameters do not appear in the path pattern.
        null
      }

      is PathSegmentParameterRouteSelector -> {
        UrlParameterName(it.name)
      }

      else -> {
        throw UnsupportedOperationException("Unsupported RouteSelector: $it of type ${it::class.simpleName}.")
      }
    }
  }

  return UrlPathSegments(elements = pathSegments)
}

/**
 * Returns the HTTP method for this route.
 */
fun List<RouteSelector>.httpMethod(): HttpMethod {
  val httpMethod = this.filterIsInstance<HttpMethodRouteSelector>().firstOrNull()?.method
    ?: throw IllegalStateException("No HttpMethodRouteSelector found for route: $this.")

  return httpMethod
}

/**
 * Returns a list of all path parameter names within this route
 */
fun List<RouteSelector>.collectPathParameterNames(): List<UrlParameterName> {
  return mapNotNull {
    when (it) {
      is ParameterRouteSelector -> {
        // Query parameters are not path parameters and must be ignored here.
        null
      }

      is PathSegmentParameterRouteSelector -> {
        UrlParameterName(it.name)
      }

      else -> {
        null
      }
    }
  }
}


/**
 * Returns the port the server is running on
 */
@Deprecated("Does not work with configuration by code")
fun ApplicationCallPipeline.port(): Int {
  return environment.config.port
}

fun ApplicationCallPipeline.host(): String {
  return environment.config.host
}

/**
 * Returns the base URL the server is bound to
 */
@Deprecated("Does not work with configuration by code")
fun ApplicationCallPipeline.guessBaseUrl(): Url.Absolute {
  return ServerEnvironment.findServerRootUrl() ?: Url.absolute("https://localhost:${port()}")
}

/**
 * Responds with the given string as JSON.
 * Uses [ContentType.Application.Json] as content type.
 */
suspend fun RoutingCall.respondJson(jsonString: String) {
  respondText(jsonString, ContentType.Application.Json)
}

/**
 * Responds with a 307 Temporary Redirect to the URL returned by [urlProvider].
 * If [urlProvider] returns null, responds with 404 Not Found.
 *
 * Useful for endpoints that redirect to externally hosted resources
 * (e.g., presigned S3 URLs, CDN links).
 */
suspend fun RoutingCall.respondTemporaryRedirect(
  cacheMaxAge: Duration = Duration.ZERO,
  urlProvider: suspend () -> Url?,
) {
  val url = urlProvider() ?: run {
    // A browser is the caller here and reads only the status; this endpoint answers no body at all.
    @Suppress("Status4xxIsAProtocolError")
    respond<HttpStatusCode>(HttpStatusCode.NotFound)
    return
  }

  if (cacheMaxAge.isPositive()) {
    response.headers.append(HttpHeaders.CacheControl, "max-age=${cacheMaxAge.inWholeSeconds}")
  }
  response.headers.append(HttpHeaders.Location, url.value)
  respond<HttpStatusCode>(HttpStatusCode.TemporaryRedirect)
}


private val logger: it.neckar.logging.Logger = it.neckar.logging.LoggerFactory.getLogger("it.neckar.ktor.KtorServerExtensions")
