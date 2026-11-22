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

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import it.neckar.ktor.health.EventLoopWatchdog
import it.neckar.ktor.health.StallReaction
import it.neckar.ktor.health.installStallReaction
import it.neckar.ktor.health.WatchdogMode
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.annotations.Blocking
import it.neckar.open.environment.ServerEnvironment
import it.neckar.open.health.ServiceState
import it.neckar.open.health.ServiceStateHolder
import it.neckar.open.http.Url
import it.neckar.open.version.VersionInformation
import it.neckar.projects.common.LocalDev
import it.neckar.projects.common.Port
import it.neckar.projects.common.ServiceDescriptor
import it.neckar.projects.common.ServiceName
import it.neckar.runtime.context.RuntimeContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.net.BindException
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Tagging interface for Ktor services (routing + service configuration).
 * Use [KtorRoutes] interface to provide (only) the routes for the service.
 *
 * Services can be tested in unit tests using some helper methods.
 * Example for a service and unit tests can be found in the class `it.neckar.rest.test.example.ExampleServiceTest`
 */
interface KtorService : KtorRoutes {
  /**
   * Launches the service.
   * Sets the service configuration, apply the routes.
   */
  override fun Application.configure()

  /**
   * Returns the corresponding service descriptor for this service
   */
  val serviceDescriptor: ServiceDescriptor

  /**
   * Optional port override for local development and testing.
   * When non-null, [effectivePort] and [baseUrl] use this instead of the descriptor's default.
   */
  val portOverride: Port?
    get() = null

  /**
   * The *effective* base URL for this service that is used to access it.
   * Derived from [serviceDescriptor] and [portOverride].
   */
  val baseUrl: Url.Absolute
    get() = serviceDescriptor.baseUrl(portOverride)

  /**
   * The *effective* port the server binds to.
   * Derived from [portOverride] (if set) or the service descriptor's default port.
   */
  val effectivePort: Port
    get() = portOverride ?: serviceDescriptor.port

  /**
   * Ports the server binds to beside the descriptor's port: the former port of a service while its callers move to the new one.
   * A [portOverride] binds no additional port, so a worktree's local run stays on its own port.
   */
  val additionalPorts: List<Port>
    get() = emptyList()
}

/**
 * Configures the application with the service configuration.
 *
 * This method is a convenience method that calls [KtorService.configure] on the application - useful for testing purposes.
 */
fun KtorService.configureFor(application: Application) {
  application.putKtorService(this)
  application.configure()
}

/**
 * Returns the base URL for the current server environment.
 */
fun ServiceDescriptor.baseUrl(portOverride: Port? = null): Url.Absolute {
  //Check the environment for a server root URL (relevant for reverse proxy)
  ServerEnvironment.findServerRootUrl()?.let { serverRootUrlFromEnvironment ->
    require(portOverride == null) {
      "Cannot override port with [$portOverride] when server root URL is provided by the environment [$serverRootUrlFromEnvironment]"
    }

    return serverRootUrlFromEnvironment
  }

  //running locally or during development - use localhost
  return Url.localhostHttp(portOverride ?: port)
}

/**
 * Launches the provided service using Netty
 *
 * This method blocks until the server is stopped.
 */
@Blocking
fun launchService(
  service: KtorService,
  /**
   * The (effective) port to use - might differ from the one defined in the service descriptor
   */
  port: Port,
) {
  try {
    val stallReaction = StallReaction(processStartWatchdogMode())
    server(service, port, stallReaction).watchEventLoop(port, stallReaction).startNamingFailedBind(wait = true)
  } catch (e: Exception) {
    logger.error("Failed to start service [${service.serviceDescriptor.serviceName}] on port [$port]", e)
    throw e
  }
}

/**
 * [EmbeddedServer.start]; a failed bind of a local run is logged with its addresses, because it fails when the Dev-IP is missing on `lo`
 * or the port is taken.
 */
private fun EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>.startNamingFailedBind(wait: Boolean) {
  try {
    start(wait = wait)
  } catch (e: Exception) {
    bindFailureHint(e, engineConfig.connectors.map { it.host })?.let { logger.error(it) }
    throw e
  }
}

/** The hint for [failure] of a server bound to [boundAddresses]: a text when a bind failed and the Dev-IP is among them, else null. */
internal fun bindFailureHint(failure: Throwable, boundAddresses: List<String>): String? {
  val bindFailed = generateSequence(failure) { it.cause }.any { it is BindException }
  return "The server binds $boundAddresses; `./gradlew verify` names a missing Dev-IP".takeIf { bindFailed && LocalDev.Address in boundAddresses }
}

/**
 * Creates the embedded server for the provided service using Netty.
 *
 * Does *not* start the server.
 */
fun server(
  service: KtorService,
  port: Port,
  /**
   * What the watchdogs of this server do on a stall. Only the process start arms it; a server a test
   * creates keeps [WatchdogMode.LogOnly].
   */
  stallReaction: StallReaction = StallReaction(WatchdogMode.LogOnly),
  /** Every port listens on each of its addresses. */
  bindHost: BindHost = BindHost.current(),
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> = embeddedServer(
  factory = Netty,
  environment = applicationEnvironment {
    log = logger
  },
  configure = {
    (listOf(port) + service.additionalPorts.takeIf { service.portOverride == null }.orEmpty()).forEach { listenedPort ->
      bindHost.addresses.forEach { address ->
        connector {
          this.host = address
          this.port = listenedPort.value
        }
      }
    }
    // Increase from Netty's default of 8192 bytes. OIDC session cookies are ~4.4 KB
    // per service, which together with standard browser headers can exceed 8 KB.
    maxHeaderSize = 32 * 1024
  },
  module = {
    installStallReaction(stallReaction)
    service.configureFor(this)
  })

/**
 * Starts an [EventLoopWatchdog] on [port] once the server is started and stops it when it stops.
 */
internal fun EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>.watchEventLoop(
  port: Port,
  stallReaction: StallReaction,
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
  val watchdog = EventLoopWatchdog(URI("http://127.0.0.1:${port.value}/"), stallReaction)
  //After the bind: a server that fails to bind never stops
  monitor.subscribe(ServerReady) { watchdog.start() }
  monitor.subscribe(ApplicationStopPreparing) { watchdog.stop() }
  return this
}

/**
 * The watchdog mode of a server the process start launches, read once: a stall ends the process,
 * unless a debugger holds threads at a breakpoint.
 */
private fun processStartWatchdogMode(): WatchdogMode = if (RuntimeContext.debugMode) WatchdogMode.DebuggerAttached else WatchdogMode.Exit

/**
 * Launches the service, then runs [startupSequence] with the HTTP server already bound.
 *
 * What the ordering buys: everything the sequence does — a schema migration above all — is visible
 * over `/health` while it happens. Running it first instead answers a caller with `connection
 * refused`, which no probe can tell apart from a crashed service.
 *
 * The service reaches [ServiceState.Ready] when the sequence returns.
 *
 * A sequence that fails decides for itself what happens next. Has it put the service into
 * [ServiceState.Fatal] — a failed migration, an incompatible schema, anything a human has to
 * resolve — the server stays up and says so on `/health/ready` instead of restarting into the same
 * wall. Has it not, the exception ends the process and the container restarts: a database that is
 * not up yet is not a permanent failure, and the next attempt is all it takes.
 *
 * A JVM shutdown while the sequence runs cancels it and waits up to [StartupShutdownGracePeriod], so
 * a migration hands its lock back and takes its apply run back instead of ending like a kill.
 *
 * This method blocks until the server is stopped.
 */
@Blocking
fun KtorService.launchWithStartupSequence(
  /**
   * The holder the lifecycle states are written to.
   */
  serviceStateHolder: ServiceStateHolder,

  /**
   * Runs after the server is bound and before the service is ready.
   */
  startupSequence: suspend () -> Unit,
) {
  logServiceStartup()

  val stallReaction = StallReaction(processStartWatchdogMode())
  val embeddedServer = server(this, effectivePort, stallReaction).watchEventLoop(effectivePort, stallReaction)
  embeddedServer.startNamingFailedBind(wait = false)

  //A failed start ends the process; stopping the server first also stops its watchdogs
  try {
    runStartupSequence(embeddedServer, serviceStateHolder, startupSequence)
  } catch (e: Throwable) {
    //Best effort: a JVM shutdown may be stopping the server at the same time
    runCatching { embeddedServer.stop(gracePeriodMillis = 0, timeoutMillis = StartupShutdownGracePeriod.inWholeMilliseconds) }
      .onFailure { logger.warn("Stopping the server after the failed startup sequence failed", it) }
    throw e
  }
}

/**
 * Runs [startupSequence] against the started [embeddedServer] and waits until the application ends.
 */
private fun KtorService.runStartupSequence(
  embeddedServer: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>,
  serviceStateHolder: ServiceStateHolder,
  startupSequence: suspend () -> Unit,
) {
  runBlocking {
    fun failStartup(e: Exception) {
      logger.error("Startup sequence of [${serviceDescriptor.serviceName}] failed", e)

      // Only a sequence that named its own reason keeps the server up. Everything else — a database
      // that is not up yet, a dependency that flapped — ends the process so the container's
      // `restart: unless-stopped` tries again; staying up as Fatal would make a start two seconds
      // too early a permanent outage, because liveness deliberately answers 200 for Fatal.
      if (serviceStateHolder.state !is ServiceState.Fatal) {
        throw e
      }
    }

    val startup: Job = launch {
      try {
        startupSequence()
        serviceStateHolder.transitionTo(ServiceState.Ready)
      } catch (e: CancellationException) {
        //Only the shutdown hook's cancel is a cancel; the sequence's own one, as from `withTimeout`, is a failure
        if (currentCoroutineContext().isActive.not()) {
          throw e
        }
        failStartup(IllegalStateException("Startup sequence of [${serviceDescriptor.serviceName}] threw a CancellationException", e))
      } catch (e: Exception) {
        failStartup(e)
      }
    }

    val cancelStartupOnShutdown = Thread({
      startup.cancel()
      runBlocking { withTimeoutOrNull(StartupShutdownGracePeriod) { startup.join() } }
    }, "cancel-startup-sequence")
    Runtime.getRuntime().addShutdownHook(cancelStartupOnShutdown)
    try {
      startup.join()
    } finally {
      removeShutdownHook(cancelStartupOnShutdown)
    }

    val serviceStateLog: Job? = (serviceStateHolder.state as? ServiceState.Fatal)?.let {
      launch { logServiceState(serviceDescriptor.serviceName, serviceStateHolder) }
    }
    try {
      embeddedServer.application.coroutineContext.job.join()
    } finally {
      serviceStateLog?.cancel()
    }
  }
}

/**
 * Logs the [ServiceState.Fatal] state every [interval] for as long as it lasts. The process stays up on
 * it, and the single error the startup sequence logged scrolls away under request and background-job lines.
 */
internal suspend fun logServiceState(
  serviceName: ServiceName,
  serviceStateHolder: ServiceStateHolder,
  interval: Duration = ServiceStateLogInterval,
  log: (String) -> Unit = { logger.error(it) },
) {
  while (true) {
    delay(interval)
    val state: ServiceState.Fatal = serviceStateHolder.state as? ServiceState.Fatal ?: error("A fatal state never ends, yet [$serviceName] is ${serviceStateHolder.state}")
    log("Service [$serviceName] is not ready: ${state.reason} — ${state.message}")
  }
}

/**
 * Launches the provided service using Netty.
 *
 * This method blocks until the server is stopped.
 */
@Blocking
fun KtorService.launch() {
  logServiceStartup()
  return launchService(this, effectivePort)
}

/**
 * The banner every start writes: version, description and the addresses and port the server binds to.
 */
private fun KtorService.logServiceStartup() {
  logger.info("Starting Service [${this.serviceDescriptor.serviceName}]: ${VersionInformation.versionAsStringVerbose}")
  serviceDescriptor.description?.let {
    logger.info("\t${it}")
  }
  logger.info("\tAddresses: ${BindHost.current().addresses} Port: [${effectivePort}] API Prefix: [${serviceDescriptor.apiPrefix}]")
}

/** As often as the Docker healthcheck asks, so the state stands near every probe in the log. */
private val ServiceStateLogInterval: Duration = 30.seconds

/** Below the `10s` `docker stop` gives a container before it kills it. */
private val StartupShutdownGracePeriod: Duration = 5.seconds

/** A shutdown that already began runs the hook anyway, which is what the hook is for. */
private fun removeShutdownHook(hook: Thread) {
  try {
    Runtime.getRuntime().removeShutdownHook(hook)
  } catch (_: IllegalStateException) {
  }
}

private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.KtorService")
