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
package it.neckar.open.app.clikt

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import it.neckar.commons.logback.ConsoleTarget
import it.neckar.commons.logback.LogbackConfigurer
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.app.clikt.model.GitlabAccessToken
import it.neckar.open.app.env.DotEnvSupport
import it.neckar.open.app.env.SensitiveEnvironmentKeys
import it.neckar.open.health.error.GlobalExceptionCollector
import it.neckar.open.health.error.recordExceptionsToOpenTelemetry
import it.neckar.open.health.error.registerGlobalExceptionHandler
import it.neckar.open.crypt.Base64CertificatePem
import it.neckar.open.crypt.CustomCaTrustConfiguration
import it.neckar.open.http.Url
import it.neckar.runtime.context.DeploymentStage
import it.neckar.runtime.context.ExecutionEnvironment
import it.neckar.runtime.context.Hostname
import it.neckar.runtime.context.RuntimeContext
import it.neckar.runtime.context.ServiceHost
import it.neckar.runtime.context.ServiceHostRegistry
import it.neckar.runtime.context.guessInUnitTestEnvironment
import it.neckar.runtime.context.initializeFromEnvironment
import it.neckar.runtime.context.RuntimeContextEnv
import org.slf4j.event.Level
import java.io.File
import java.nio.file.Path

/**
 * Base class for all Clikt commands in the application.
 *
 * Automatically configures the command to support `.env` files and environment variables.
 *
 * This class stays in jvmMain: the runtime context, the Logback configuration and the JVM trust
 * store bind it to the JVM. The options that carry no such dependency — the GitLab and Keycloak
 * credentials plus `--http-port` — live in commonMain (`CredentialOptions.kt`) and are declared on
 * Clikt's own `BaseCliktCommand`, so a Kotlin/Native binary gets the same flags. The `.env` reading
 * is common code as well (`it.neckar.open.app.env`).
 *
 * Moving this class up needs a `linuxX64` target on the modules it reads its types from —
 * `kotlin-lang` for `Url` and the runtime context, `descriptors` for the stage and environment
 * enums. Neither has one today.
 */
abstract class BaseCommand<out HostType : ServiceHost>(
  name: String? = null,
  serviceHostRegistry: ServiceHostRegistry<HostType>,
  /** Where the log goes. A command whose stdout carries a machine-readable answer passes [ConsoleTarget.Stderr]. */
  logConsoleTarget: ConsoleTarget = ConsoleTarget.Stdout,
) : SuspendingCliktCommand(name) {
  val deploymentStageOption: DeploymentStage? by deploymentStageOption()
  val serviceHostOption: String? by serviceHostOption()
  val executionEnvironmentOption: ExecutionEnvironment? by executionEnvironment()

  /**
   * The runtime context
   */
  val runtimeContext: RuntimeContext<out HostType> by lazy { //lazy evaluation required, because the runtime context needs to be initialized after the command options are parsed
    RuntimeContext.initializeFromEnvironment(
      serviceHostRegistry = serviceHostRegistry,

      forcedExecutionEnvironment = executionEnvironmentOption,
      forcedDeploymentStage = deploymentStageOption,
      forcedHostName = Hostname.nullable(serviceHostOption),
    )
  }

  /**
   * Returns the execution environment of the current runtime context.
   */
  val executionEnvironment: ExecutionEnvironment
    get() {
      return runtimeContext.executionEnvironment
    }

  /**
   * Returns the deployment stage of the current runtime context.
   */
  val deploymentStage: DeploymentStage
    get() {
      return runtimeContext.stage
    }

  /**
   * Returns the service host of the current runtime context.
   */
  val host: HostType
    get() {
      return runtimeContext.host
    }

  /**
   * Keeps the global-exception-handler registration alive for the lifetime of this command.
   * The registration is never disposed (handler is meant to catch uncaught exceptions for the
   * whole run), but holding the [Disposable] in a field documents the ownership explicitly.
   */
  @Suppress("unused")
  private val globalExceptionHandlerRegistration = GlobalExceptionCollector.registerGlobalExceptionHandler()

  /**
   * Reports every recorded uncaught exception to OpenTelemetry as a trace error, in addition to the
   * ERROR log. Kept for the whole run; resolves to a no-op tracer when no OTel agent is attached.
   */
  @Suppress("unused")
  private val otelExceptionRecorderRegistration = GlobalExceptionCollector.recordExceptionsToOpenTelemetry()

  init {
    //Initialize logging as early as possible - can be overridden later (usually in the run method)
    LogbackConfigurer.configureLoggingConsoleOnly(levelForRoot = Level.INFO, levelForNeckarIt = Level.INFO, consoleTarget = logConsoleTarget)

    //Do not use runtimeContext here to avoid circular dependencies (Catch 22)
    val dotEnv = if (guessInUnitTestEnvironment()) DotEnvSupport.dotEnvForUnitTest else DotEnvSupport.dotEnv

    // Every `envvar` option of every command reads through this, so the debug log sees each
    // credential the command touches — hence the masking.
    context {
      readEnvvar = { key ->
        dotEnv[key].also {
          logger.debug("Reading environment variable [$key] from .env -> ${SensitiveEnvironmentKeys.maskIfSensitive(key, it)}")
        }
      }
    }
  }

  /**
   * Ensure that the runtime context is initialized. Call the abstract [execute] method afterward.
   */
  final override suspend fun run() {
    //Ensure that the runtime context is initialized
    require(runtimeContext.isInitialValue().not()) {
      //Should never happen!
      "Runtime context is not initialized"
    }

    logger.debug("Runtime Context: ")
    logger.debug("\tExecution Environment: {}", executionEnvironment)
    logger.debug("\tDeployment Stage: {}", deploymentStage)
    logger.debug("\tHost: {}", host)

    execute()
  }

  protected abstract suspend fun execute()

  companion object {
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.open.app.clikt.BaseCommand")
  }
}



/**
 * Returns the gitlab server URL.
 * Defaults to `https://git.neckar.it` if not provided via CLI or `GITLAB_SERVER_URL` env var.
 */
fun BaseCommand<*>.gitlabServerUrlOption(): OptionWithValues<Url.Absolute, Url.Absolute, Url.Absolute> {
  return option(
    help = "Gitlab server URL",
    envvar = "GITLAB_SERVER_URL",
    names = arrayOf("--gitlab-server-url"),
  ).convert { value ->
    Url.absolute(value)
  }.default(Url.absolute("https://git.neckar.it"))
}


/**
 * Creates an option for a secret the command reads from a file, as Docker Compose mounts a secret under `/run/secrets/<name>`. The value
 * is the content of the file without surrounding whitespace.
 */
fun BaseCommand<*>.secretFileOption(name: String, envvar: String, help: String): NullableOption<String, String> {
  // convert turns the FileNotFoundException, whose message names the path, into a parse error.
  return option(names = arrayOf(name), envvar = envvar, help = help).convert { path -> File(path).readText().trim() }
}

/**
 * Creates an option for the GitLab access token, read from the file `--gitlab-access-token-file` names.
 *
 * Uses the env var `GITLAB_ACCESS_TOKEN_FILE` when present. Call `.required()` if the token is mandatory.
 */
fun BaseCommand<*>.gitlabAccessTokenFileOption(): NullableOption<GitlabAccessToken, GitlabAccessToken> {
  return secretFileOption(
    name = "--gitlab-access-token-file",
    envvar = "GITLAB_ACCESS_TOKEN_FILE",
    help = "Path to the file (Docker secret) holding the GitLab access token",
  ).convert { value -> GitlabAccessToken(value) }
}


/**
 * Creates an optional option for a custom CA certificate path.
 *
 * This allows connecting to services with self-signed certificates
 * or certificates signed by internal CAs.
 *
 * Uses the env var `CA_CERT_PATH` when present.
 */
fun BaseCommand<*>.caCertOption(): NullableOption<String, String> {
  return option(
    help = "Path to custom CA certificate in PEM format for SSL connections",
    envvar = "CA_CERT_PATH",
    names = arrayOf("--ca-cert"),
  )
}

/**
 * Creates an optional option for a custom CA certificate as Base64-encoded string.
 *
 * This allows connecting to services with self-signed certificates
 * or certificates signed by internal CAs, without needing to mount a file.
 *
 * The certificate can be:
 * - Base64-encoded PEM file content (including headers)
 * - Raw Base64 of the DER-encoded certificate
 *
 * Uses the env var `CA_CERT_BASE64` when present.
 * Takes precedence over `CA_CERT_PATH` when both are set.
 */
fun BaseCommand<*>.caCertBase64Option(): NullableOption<Base64CertificatePem, Base64CertificatePem> {
  return option(
    help = "Base64-encoded CA certificate in PEM format for SSL connections. Takes precedence over --ca-cert if both are set.",
    envvar = "CA_CERT_BASE64",
    names = arrayOf("--ca-cert-base64"),
  ).convert { value ->
    Base64CertificatePem(value)
  }
}

/**
 * Applies a custom CA certificate to the JVM's default SSL context.
 *
 * Configures the JVM to trust both system CAs and the provided custom CA certificate.
 * [caCertBase64] takes precedence over [caCertPath] if both are provided.
 * Does nothing if neither is provided.
 */
fun BaseCommand<*>.applyCustomCaCertificate(
  caCertPath: String?,
  caCertBase64: Base64CertificatePem?,
) {
  when {
    caCertBase64 != null -> CustomCaTrustConfiguration.addToJvmDefaultTrustStore(caCertBase64)
    caCertPath != null -> CustomCaTrustConfiguration.addToJvmDefaultTrustStore(Path.of(caCertPath))
  }
}


/**
 * The execution environment option.
 */
fun BaseCommand<*>.executionEnvironment(): NullableOption<ExecutionEnvironment, ExecutionEnvironment> {
  return option(
    help = "The execution environment. One of ${ExecutionEnvironment.entries.joinToString(", ")}",
    envvar = RuntimeContextEnv.ExecutionEnvironmentKey,
    names = arrayOf("--execution-environment"),
  ).enum<ExecutionEnvironment>(ignoreCase = true)
}

/**
 * The stage option.
 */
fun BaseCommand<*>.deploymentStageOption(): NullableOption<DeploymentStage, DeploymentStage> {
  return option(
    help = "The deployment stage. One of ${DeploymentStage.entries.joinToString(", ")}",
    envvar = RuntimeContextEnv.DeploymentStageKey,
    names = arrayOf("--deployment-stage"),
  ).enum<DeploymentStage>(ignoreCase = true)
}

fun BaseCommand<*>.serviceHostOption(
  help: String = "The service host name.",
): RawOption {
  return option(
    help = help,
    envvar = RuntimeContextEnv.ServiceHostKey,
    names = arrayOf("--service-host"),
  )
}
