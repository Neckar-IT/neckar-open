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
package it.neckar.commons.logback

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import ch.qos.logback.core.spi.FilterReply
import ch.qos.logback.core.status.Status
import ch.qos.logback.core.util.FileSize
import ch.qos.logback.core.util.StatusPrinter
import com.github.loki4j.logback.JsonLayout
import com.github.loki4j.logback.Loki4jAppender
import com.github.loki4j.logback.PipelineConfigAppenderBase
import com.github.loki4j.logback.PipelineConfigAppenderBase.HttpCfg
import it.neckar.commons.logback.LogbackConfigurer.configureLoggingConsoleOnly
import it.neckar.runtime.context.Hostname
import it.neckar.runtime.context.RuntimeContext
import net.logstash.logback.encoder.LogstashEncoder
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.io.OutputStream
import java.io.PrintStream

/**
 * A configurer for logback logging.<br></br>
 *
 * @see [http://logback.qos.ch/](http://logback.qos.ch/)
 */
object LogbackConfigurer {
  /**
   * Configures the logger for console output only - with a special level for Neckar IT classes.
   */
  fun configureLoggingConsoleOnly(
    levelForRoot: org.slf4j.event.Level = Level.INFO,
    levelForNeckarIt: org.slf4j.event.Level,
    consoleTarget: ConsoleTarget = ConsoleTarget.Stdout,
  ) {
    configureLoggingConsoleOnly(levelForRoot, consoleTarget = consoleTarget)
    setLevelForNeckarIt(levelForNeckarIt)
  }

  /**
   * Sets the log level for Neckar IT classes
   */
  fun setLevelForNeckarIt(levelForNeckarIt: Level) {
    setLoggerLevel("it.neckar", levelForNeckarIt)
  }

  /**
   * Configures the logger for console output only.
   */
  fun configureLoggingConsoleOnly(levelForRoot: org.slf4j.event.Level, consoleTarget: ConsoleTarget = ConsoleTarget.Stdout) {
    configureLoggingConsoleOnly(levelForRoot.toLogback(), consoleTarget = consoleTarget)
  }

  /**
   * Configures the logging to use only the provided output stream
   */
  fun configureLoggingToStreamOnly(out: OutputStream, levelForRoot: org.slf4j.event.Level) {
    clearExistingAppenders()
    addStreamAppender(out)
    setRootLoggerLevel(levelForRoot)
  }

  /**
   * Configures the logger for console output only.
   */
  fun configureLoggingConsoleOnly(levelForRoot: ch.qos.logback.classic.Level = ch.qos.logback.classic.Level.INFO, consoleTarget: ConsoleTarget = ConsoleTarget.Stdout) {
    clearExistingAppenders()
    addConsoleAppender(consoleTarget)

    setRootLoggerLevel(levelForRoot)
  }

  /**
   * Configures the logger for console output only, using a JSON encoder ([LogstashEncoder]).
   *
   * **Use this for Docker-hosted services** behind an `otel-agent` `filelog` receiver.
   * Each log event is written as one JSON line on stdout, including all MDC fields (notably
   * `trace_id`/`span_id` from the Java agent's `logback-mdc-1.0` instrumentation), `level`, `logger_name`,
   * `thread_name`, `message`, and `stack_trace`. The `otel-agent` parses the JSON and exposes
   * `severity_text`, trace correlation, and structured attributes to Loki.
   *
   * For standalone JVM tools / non-Docker deployments without a local OTel collector, use
   * [configureLoggingConsoleAndLoki] instead.
   *
   * See issue #1708 for the full pipeline description.
   */
  fun configureLoggingConsoleOnlyJson(
    levelForRoot: org.slf4j.event.Level = Level.INFO,
    levelForNeckarIt: org.slf4j.event.Level = Level.INFO,
    consoleTarget: ConsoleTarget = ConsoleTarget.Stdout,
  ) {
    clearExistingAppenders()
    addJsonConsoleAppender(consoleTarget)

    setRootLoggerLevel(levelForRoot)
    setLevelForNeckarIt(levelForNeckarIt)
  }

  /**
   * Configure logging
   */
  fun configureLoggingConsoleAndFile(logFile: File, levelForRoot: org.slf4j.event.Level) {
    configureLoggingConsoleAndFile(logFile, levelForRoot.toLogback())
  }

  fun configureLoggingConsoleAndFile(
    logFile: File,
    levelForRoot: org.slf4j.event.Level,
    levelForNeckarIt: org.slf4j.event.Level,
  ) {
    configureLoggingConsoleAndFile(logFile, levelForRoot.toLogback())
    setLevelForNeckarIt(levelForNeckarIt)
  }

  /**
   * Adds the console appender and the Loki appender.
   * Removes all other appenders.
   *
   * **Use this only for standalone JVM tools / non-Docker deployments.**
   *
   * Typical use case: CLI tools and native installations running on end-user machines without
   * a local OTel collector reachable (for example `atlas-connect`). Such tools cannot rely on
   * the Docker/otel-agent log pipeline and must push to Loki directly.
   *
   * **Do not use for Docker-hosted services.** There the standard log pipeline is:
   * Logback writes JSON to stdout → Docker JSON log file → `otel-agent` filelog receiver →
   * Loki. Using `configureLoggingConsoleAndLoki` in such a container produces duplicate Loki
   * entries (one via direct push, one via the otel-agent). For docker-hosted services use
   * [configureLoggingConsoleOnly] together with a JSON encoder (see issue #1708).
   *
   * Attention: If necessary provide a value for [lokiEnabled] - by default, the Loki appender is only enabled in production environments.
   */
  fun configureLoggingConsoleAndLoki(
    lokiServerConfiguration: LokiServerConfiguration,
    app: String,
    hostname: Hostname = RuntimeContext.host.hostname,
    levelForRoot: org.slf4j.event.Level = Level.INFO,
    levelForNeckarIt: org.slf4j.event.Level = Level.INFO,
    /**
     * Whether the loki appender should be enabled.
     * By default, the loki appender is only enabled in production environments
     */
    lokiEnabled: Boolean = RuntimeContext.executionEnvironment.isProduction(),
    lokiAppenderConfig: Loki4jAppender.() -> Unit = {},
  ) {
    clearExistingAppenders()
    addConsoleAppender()

    if (lokiEnabled) {
      addLokiAppender(lokiServerConfiguration = lokiServerConfiguration, app = app, hostname = hostname, lokiAppenderConfig = lokiAppenderConfig)
    }

    setRootLoggerLevel(levelForRoot)
    setLevelForNeckarIt(levelForNeckarIt)
  }

  /**
   * Configure (default) logging - for console and file
   */
  fun configureLoggingConsoleAndFile(logFile: File, levelForRoot: ch.qos.logback.classic.Level = ch.qos.logback.classic.Level.INFO) {
    clearExistingAppenders()
    addConsoleAppender()
    addFileAppender(logFile)

    setRootLoggerLevel(levelForRoot)
  }

  /**
   * Removes all existing appenders (e.g. from the BasicConfiguration)
   */
  fun clearExistingAppenders() {
    val rootLogger: Logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
    rootLogger.detachAndStopAllAppenders()
  }

  /**
   * Print the logger status to the given print stream
   */
  fun printStatus(out: PrintStream) {
    val lc = LoggerFactory.getILoggerFactory() as LoggerContext
    StatusPrinter.setPrintStream(out)
    StatusPrinter.print(lc)
  }

  private const val EncoderPattern: String = "[%d{ISO8601}]-[%thread] %-5level %logger - %msg%n"
  const val LogFileName: String = "application.log"

  /**
   * Suffix for logger that are only logged to file
   */
  const val LoggerSuffixFileOnly: String = "_fileOnly"
  const val FileAppenderName: String = "FILE"

  /**
   * Returns the logback classic Logger Context
   */
  val loggerContext: LoggerContext
    get() = LoggerFactory.getILoggerFactory() as LoggerContext

  /**
   * Returns the logback logger for the given name
   */
  private fun getLogbackLogger(loggerName: String): Logger = loggerContext.getLogger(loggerName)

  /**
   * Returns the corresponding logback logger from a slf4j logger
   */
  fun getLogbackLogger(logger: org.slf4j.Logger): Logger {
    return getLogbackLogger(logger.name)
  }

  /**
   * Sets the level for the root logger
   */
  fun setRootLoggerLevel(level: org.slf4j.event.Level) {
    setRootLoggerLevel(level.toLogback())
  }

  fun setRootLoggerLevel(level: ch.qos.logback.classic.Level) {
    val rootLogger = getLogbackLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    rootLogger.level = level
  }

  /**
   * Sets the logger leve for an slf4j logger
   */
  fun setLoggerLevel(logger: org.slf4j.Logger, level: org.slf4j.event.Level) {
    setLoggerLevel(logger.name, level)
  }

  /**
   * Sets the logger level for the logger with the given name
   */
  fun setLoggerLevel(loggerName: String, level: org.slf4j.event.Level) {
    val logger: Logger = getLogbackLogger(loggerName)
    setLoggerLevel(logger, level)
  }

  /**
   * Sets the logger level for a logback logger
   */
  fun setLoggerLevel(logger: Logger, level: org.slf4j.event.Level) {
    logger.level = level.toLogback()
  }

  fun getLoggerLevel(logger: Logger): org.slf4j.event.Level {
    return logger.effectiveLevel.toSlf4j()
  }

  /**
   * Returns the logger level - if there is one
   */
  fun getLoggerLevel(logger: org.slf4j.Logger): org.slf4j.event.Level {
    return logger.toLogback().effectiveLevel.toSlf4j()
  }

  /**
   * Creates and adds a file appender to the root logger.
   *
   * @param logFile the target file where to write the log messages
   */
  fun addFileAppender(logFile: File) {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

    val fileAppender = RollingFileAppender<ILoggingEvent>()
    fileAppender.context = loggerContext
    fileAppender.name = FileAppenderName

    val rollingPolicy = FixedWindowRollingPolicy()
    rollingPolicy.minIndex = 1
    rollingPolicy.maxIndex = 3
    rollingPolicy.fileNamePattern = logFile.absolutePath + ".%i"
    rollingPolicy.context = loggerContext
    rollingPolicy.setParent(fileAppender)

    val triggeringPolicy = SizeBasedTriggeringPolicy<ILoggingEvent>()
    triggeringPolicy.maxFileSize = FileSize.valueOf("5 mb")
    triggeringPolicy.context = loggerContext

    fileAppender.file = logFile.absolutePath
    fileAppender.isAppend = true // mandatory for RollingFileAppender
    fileAppender.rollingPolicy = rollingPolicy
    fileAppender.triggeringPolicy = triggeringPolicy

    rollingPolicy.start()
    triggeringPolicy.start()

    startAppenderAndAddToRoot(loggerContext, fileAppender, target = logFile.absolutePath)
  }

  /**
   * Creates and adds a console appender to the root logger.
   *
   * In most cases [configureLoggingConsoleOnly] should be used instead.
   *
   * @param consoleTarget where to write log output ([ConsoleTarget.Stdout] or [ConsoleTarget.Stderr]).
   *   Use [ConsoleTarget.Stderr] for CLI tools that output structured data on stdout.
   */
  fun addConsoleAppender(consoleTarget: ConsoleTarget = ConsoleTarget.Stdout) {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val consoleAppender = newConsoleAppender(loggerContext, consoleTarget, "")
    startAppenderAndAddToRoot(loggerContext, consoleAppender, target = consoleTarget.name)
  }

  /**
   * Creates a [ConsoleAppender] with the standard "file-only logger" filter, but without an
   * encoder or start. The caller decides which encoder to attach via
   * [startAppenderAndAddToRoot].
   */
  private fun newConsoleAppender(
    loggerContext: LoggerContext,
    consoleTarget: ConsoleTarget,
    nameSuffix: String,
  ): ConsoleAppender<ILoggingEvent> {
    val consoleAppender = ConsoleAppender<ILoggingEvent>()
    consoleAppender.context = loggerContext
    consoleAppender.target = consoleTarget.logbackTarget
    consoleAppender.name = when (consoleTarget) {
      ConsoleTarget.Stdout -> "STDOUT$nameSuffix"
      ConsoleTarget.Stderr -> "STDERR$nameSuffix"
    }
    consoleAppender.addFilter(object : Filter<ILoggingEvent>() {
      override fun decide(event: ILoggingEvent): FilterReply {
        return if (event.loggerName.endsWith(LoggerSuffixFileOnly)) {
          FilterReply.DENY
        } else FilterReply.NEUTRAL
      }
    })
    return consoleAppender
  }

  /**
   * Creates and adds a console appender that writes each log event as one JSON line, using
   * [LogstashEncoder]. Intended for Docker-hosted services where the `otel-agent`
   * `filelog` receiver parses the JSON.
   *
   * In most cases [configureLoggingConsoleOnlyJson] should be used instead of calling this
   * directly.
   */
  fun addJsonConsoleAppender(consoleTarget: ConsoleTarget = ConsoleTarget.Stdout) {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val consoleAppender = newConsoleAppender(loggerContext, consoleTarget, "_JSON")
    startAppenderAndAddToRoot(loggerContext, consoleAppender, target = consoleTarget.name, encoder = LogstashEncoder())
  }

  /**
   * Configure logging for the given output stream.
   * This method is especially useful for unit tests of logging.
   */
  fun addStreamAppender(sink: OutputStream) {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

    val appender: OutputStreamAppender<ILoggingEvent> = OutputStreamAppender<ILoggingEvent>()
    appender.context = loggerContext //set the context first!
    appender.name = "STREAM_APPENDER"
    appender.outputStream = sink

    appender.addFilter(object : Filter<ILoggingEvent>() {
      override fun decide(event: ILoggingEvent): FilterReply {
        return if (event.loggerName.endsWith(LoggerSuffixFileOnly)) {
          FilterReply.DENY
        } else FilterReply.NEUTRAL
      }
    })

    startAppenderAndAddToRoot(loggerContext, appender, target = sink.toString())
  }

  /**
   * Adds a loki appender
   */
  fun addLokiAppender(lokiServerConfiguration: LokiServerConfiguration, app: String, hostname: Hostname, lokiAppenderConfig: Loki4jAppender.() -> Unit) {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

    val loki4jAppender = Loki4jAppender()
    loki4jAppender.name = "LOKI"
    loki4jAppender.context = loggerContext
    loki4jAppender.lokiAppenderConfig()

    if (false) {
      loki4jAppender.setMetricsEnabled(true) //TODO check?
    }

    loki4jAppender.setHttp(HttpCfg().also { cfg ->
      cfg.setAuth(PipelineConfigAppenderBase.BasicAuth().also {auth ->
        auth.setUsername(lokiServerConfiguration.username)
        auth.setPassword(lokiServerConfiguration.password)
      })
      cfg.setUrl(lokiServerConfiguration.lokiServerUrl.toString())
    })

    loki4jAppender.setMessage(JsonLayout())

    loki4jAppender.setLabels("service_name=${app}\nservice_instance_id=${hostname}\nlogger=%logger")
    loki4jAppender.setVerbose(true)

    loki4jAppender.start()
    reportIfNotStarted(loki4jAppender, lokiServerConfiguration.lokiServerUrl.toString())

    val rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    rootLogger.addAppender(loki4jAppender)
  }

  private fun startAppenderAndAddToRoot(
    loggerContext: LoggerContext,
    appender: OutputStreamAppender<ILoggingEvent>,
    target: String,
    encoder: Encoder<ILoggingEvent> = defaultPatternLayoutEncoder(loggerContext),
  ) {
    encoder.context = loggerContext
    val lifeCycleEncoder = encoder as? ch.qos.logback.core.spi.LifeCycle
    if (lifeCycleEncoder != null && lifeCycleEncoder.isStarted.not()) {
      lifeCycleEncoder.start()
    }

    appender.encoder = encoder
    appender.start()
    reportIfNotStarted(appender, target)

    val rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    rootLogger.addAppender(appender)
  }

  /**
   * Reports an appender that did not start - on [System.err], together with the reason logback recorded.
   *
   * An appender that failed to start is added to the root logger like any other, but discards every event
   * it is handed. Logback puts the reason (unwritable log file, malformed Loki URL, ...) into the logger
   * context's status manager and nowhere else, so without this report a dead log destination is
   * indistinguishable from a working one - both simply produce no output.
   *
   * [System.err] rather than a logger: this is the logging configuration itself failing, so it cannot
   * report through the very appenders it is setting up.
   *
   * @param target where the appender was supposed to write, e.g. the log file path or the Loki URL
   */
  private fun reportIfNotStarted(appender: Appender<ILoggingEvent>, target: String) {
    if (appender.isStarted) {
      return
    }

    val reasons = loggerContext.statusManager.copyOfStatusList
      .filter { it.effectiveLevel >= Status.WARN }
      .joinToString(separator = "\n") { status -> "  ${status.message}${status.throwable?.let { throwable -> " - $throwable" }.orEmpty()}" }

    System.err.println("Log appender <${appender.name}> for <$target> failed to start - it discards every log event:\n$reasons")
  }

  private fun defaultPatternLayoutEncoder(loggerContext: LoggerContext): PatternLayoutEncoder {
    val encoder = PatternLayoutEncoder()
    encoder.context = loggerContext
    encoder.pattern = EncoderPattern
    return encoder
  }
}
