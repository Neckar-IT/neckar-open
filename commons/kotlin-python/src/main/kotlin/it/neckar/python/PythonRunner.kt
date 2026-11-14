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
package it.neckar.python

import it.neckar.logging.Logger
import it.neckar.logging.LoggerDelegate
import it.neckar.open.file.requireIsDirectory
import it.neckar.open.file.requireIsFile
import it.neckar.open.kotlin.lang.ifInitialized
import it.neckar.open.string.StringShortener
import it.neckar.open.string.shorten
import it.neckar.open.time.nowMillis
import it.neckar.open.unit.si.ms
import it.neckar.python.PythonRunner.PythonProcess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Keeps one long-lived Python process alive and talks to it over stdin/stdout — one command per line.
 * The process is started once so that expensive script startup (loading a model, importing libraries) is paid once
 * instead of per command.
 *
 * The Python script is not an arbitrary program, it has to speak this line protocol:
 * - print a line starting with `[LOADED]` as soon as it accepts commands. [PythonProcess] blocks in its constructor
 *   until that line arrives.
 * - read one command per line from stdin.
 * - answer with any number of lines prefixed `[DATA] `, terminated by a line starting with `[OK]`. Every other line
 *   is logged and dropped, so the script may print progress freely. A script that never prints `[OK]` makes
 *   [executePythonTask] block forever — the read only unblocks when the process dies.
 *
 * ATTENTION: Single-threaded use only! Commands carry no correlation id, so two threads writing to the same stdin
 * would read each other's `[DATA]` lines back.
 */
class PythonRunner(
  /**
   * Python executable to use
   */
  val pythonExecutable: PythonExecutable,
  /**
   * File name of the Python script to run
   */
  val pythonScripFileName: String,
  /**
   * Working directory for the Python script execution
   */
  val workingDirectory: File,
) : AutoCloseable {

  init {
    //Fail here rather than on the first command: a missing interpreter is a setup problem, not a task failure
    val process = ProcessBuilder(pythonExecutable.pathToPython, "--version").also {
      it.redirectError()
      it.redirectOutput()
    }.start()
    process.waitFor().also {
      if (it != 0) {
        throw IllegalStateException("Python executable not found!")
      }
    }

    workingDirectory.requireIsDirectory { "Working dir [${it.absolutePath}] not found" }
  }

  /**
   * The state of the Python process — [PythonProcessState.Uninitialized] until [initPythonRunner] has been called.
   */
  val state: PythonProcessState
    get() = if (this::runningPythonProcess.isInitialized) runningPythonProcess.state else PythonProcessState.Uninitialized

  /**
   * The [PythonProcess] that is running
   */
  private lateinit var runningPythonProcess: PythonProcess

  /**
   * Starts the Python process and blocks until the script has printed `[LOADED]`.
   * Must be called before [executePythonTask]; [parameters] reach the script as command line arguments and can only
   * be changed by restarting it — see [updatePythonRunner].
   */
  fun initPythonRunner(parameters: List<String>) {
    logger.info("Initializing PythonRunner with parameters: ${parameters.joinToString(" ")}")
    runningPythonProcess = PythonProcess(parameters)
  }

  /**
   * Sends one command to the Python process and blocks until it answers `[OK]`, collecting its `[DATA] ` lines.
   */
  fun executePythonTask(
    /**
     * A single line — a command containing a newline would be read by the script as two commands.
     */
    pythonCommand: String,
    /**
     * Whether to run [PythonRunner.restartIfBroken] first. Only a [PythonProcessState.Broken] runner is revived
     * that way; a closed one is [PythonProcessState.Uninitialized] and throws there instead.
     */
    restartIfBroken: Boolean = true,
    /**
     * Whether to log every line the script prints. Off for commands whose answers are large or sensitive.
     */
    debugLogging: Boolean = true,
  ): PythonResult {
    check(this::runningPythonProcess.isInitialized) { "PythonProcess not initialized" }
    if (restartIfBroken) restartIfBroken(runningPythonProcess.parameters)
    return runningPythonProcess.executePythonTask(pythonCommand = pythonCommand, debugLogging = debugLogging)
  }

  /**
   * Makes the process run with [parameters]. They are command line arguments of the script, so changing them means
   * restarting the process — which is why identical parameters are a deliberate no-op.
   * Restarting inherits the precondition of [restartPythonRunner].
   */
  fun updatePythonRunner(parameters: List<String>) {
    if (this::runningPythonProcess.isInitialized) {
      if (runningPythonProcess.isRunningWithTheseParameters(parameters)) logger.info("PythonProcess already running with the same command")
      else {
        logger.info("Updating PythonRunner with new parameters: ${parameters.joinToString(" ")}")
        restartPythonRunner(parameters)
      }
    } else initPythonRunner(parameters)
  }

  /**
   * Kills the process and starts a new one with [parameters].
   * Only allowed while the state is [PythonProcessState.Broken]: every other state throws, a running process is
   * never killed from here. A runner that has been closed is [PythonProcessState.Uninitialized], not broken, and
   * therefore cannot be restarted either — use [initPythonRunner].
   */
  fun restartPythonRunner(parameters: List<String>) {
    check(state == PythonProcessState.Broken) { "Can only kill PythonRunner when state is Broken, but was $state" }
    logger.info("Restarting PythonRunner with parameters: ${parameters.joinToString(" ")}")
    measureTimeMillis {
      this::runningPythonProcess.ifInitialized { it.close() }
      initPythonRunner(parameters)
    }.also { logger.info("Restarted PythonRunner in $it ms") }
  }

  /**
   * Restarts the process unless it is running. "Not running" is wider than [PythonProcessState.Broken] — for a
   * closed runner this reaches [restartPythonRunner] and fails its precondition.
   */
  fun restartIfBroken(parameters: List<String>) {
    if (state.isRunning) return
    restartPythonRunner(parameters)
  }

  /**
   * Close the [PythonRunner]
   */
  override fun close() {
    logger.info("Closing PythonRunner")
    this::runningPythonProcess.ifInitialized { it.close() }
  }


  /**
   * The state of the Python process
   */
  enum class PythonProcessState {
    /**
     * No process — either never started or closed again. Closing does not leave the runner in [Stopped].
     */
    Uninitialized,

    /**
     * Started, waiting for the script's `[LOADED]` line.
     */
    Initializing,
    Ready,

    /**
     * A command has been written and its `[OK]` line is still outstanding.
     */
    Busy,

    /**
     * Set while [PythonProcess.close] destroys the process, immediately replaced by [Uninitialized].
     */
    Stopped,

    /**
     * The process or the protocol failed. The only state [restartPythonRunner] accepts.
     */
    Broken,

    ;

    val isRunning: Boolean
      get() = this == Initializing || this == Ready || this == Busy
  }

  /**
   * Wrapper class for a Python process
   */
  inner class PythonProcess(val parameters: List<String>) : AutoCloseable {

    var state: PythonProcessState = PythonProcessState.Uninitialized
      private set

    /**
     * Carries everything the script prints, stderr included — the process merges both streams.
     */
    private val processStdOut: BufferedReader

    /**
     * Always at end of stream: merging stderr into stdout makes the process' error stream an empty one.
     */
    private val processErrorOut: BufferedReader

    /**
     * The writer to write to the Python process
     */
    private val processStdIn: BufferedWriter

    /**
     * The Python process that is running
     */
    private val runningPythonProcess: Process

    /**
     * Initializes the [PythonProcess]
     */
    init {
      check(state == PythonProcessState.Uninitialized) { "Can only initialize Python process when state is Stopped" }
      state = PythonProcessState.Initializing

      try {
        logger.info("Creating PythonProcess in working dir: ${workingDirectory.absolutePath}")
        //Verify that the Python script file exists
        File(workingDirectory, pythonScripFileName).requireIsFile()

        val processArguments = buildList {
          add(pythonExecutable.pathToPython)
          add(pythonScripFileName)
          addAll(parameters)
        }

        logger.info("Starting PythonProcess with parameters: $parameters in working dir: ${workingDirectory.absolutePath}")

        runningPythonProcess = ProcessBuilder(processArguments)
          .directory(workingDirectory)
          .redirectOutput(ProcessBuilder.Redirect.PIPE)
          .redirectError(ProcessBuilder.Redirect.PIPE)
          .redirectErrorStream(true)
          .start()

        // Create reader and writer for communication with the Python process
        processStdOut = runningPythonProcess.inputReader()
        processErrorOut = runningPythonProcess.errorReader()
        processStdIn = runningPythonProcess.outputWriter()
        logger.info("[Init] Loading Python")

        // Wait for the Python script to load the model and display startup text
        while (runningPythonProcess.isAlive) {
          val line = processStdOut.readLine() ?: throw IllegalStateException("processStdOut provided null. Initialization failed. See output above for details.")
          logger.info("[Init] Read line from Python: $line")

          if (line.startsWith("[LOADED]")) {
            state = PythonProcessState.Ready
            break
          }
        }

        check(state == PythonProcessState.Ready) { "PythonProcess died while waiting for startup" }
      } catch (e: Exception) {
        logger.error("Error while initializing Python process", e)
        state = PythonProcessState.Broken
        throw e
      }

      logger.info("[Init] Initialized PythonProcess")
    }

    /**
     * Writes [pythonCommand] to stdin and reads lines until `[OK]`, keeping the `[DATA] ` ones.
     * Any exception on the way marks the process [PythonProcessState.Broken] — the stream position is then unknown,
     * so the process cannot be reused, only restarted.
     */
    fun executePythonTask(pythonCommand: String, debugLogging: Boolean): PythonResult {
      logger.info("State of PythonProcess: $state")
      check(state == PythonProcessState.Ready) { "Can only extract plate coordinates when state is [Ready] but was [$state]" }
      state = PythonProcessState.Busy

      logger.info("Sending command [Length: ${pythonCommand.length}] <${pythonCommand.shorten(128, StringShortener.TruncateCenterToLength)}> to Python")
      val data = mutableListOf<String>()
      try {
        processStdIn.write("${pythonCommand}\n")
        processStdIn.flush()

        while (true) {
          val line = processStdOut.readLine() ?: throw IllegalStateException("null read! End of stream reached!")
          if (debugLogging) logger.debug("PYTHON: [Length: ${line.length}] ${line.shorten(128, StringShortener.TruncateCenterToLength)}")

          if (line.startsWith(DataPrefix)) {
            data.add(line.removePrefix(DataPrefix))
          } else if (line.startsWith(SuccessMessage)) {
            state = PythonProcessState.Ready
            return PythonResult(pythonCommand, data)
          } else if (debugLogging) {
            logger.debug("Ignoring line from Python: ${line.shorten(128, StringShortener.TruncateCenterToLength)}")
          }
        }
      } catch (e: Exception) {
        state = PythonProcessState.Broken
        logger.error("Error while executing Python task", e)
        throw e
      }
    }

    /**
     * Checks if the [PythonProcess] is running with the given parameters
     */
    fun isRunningWithTheseParameters(parameters: List<String>): Boolean {
      return this.parameters == parameters
    }

    /**
     * Terminates the process and leaves the state at [PythonProcessState.Uninitialized], not
     * [PythonProcessState.Stopped]. Does not wait for the process to actually die.
     */
    override fun close() {
      state = PythonProcessState.Stopped
      runningPythonProcess.destroy()
      state = PythonProcessState.Uninitialized
    }

    /**
     * Checks if two [PythonProcess] are equal
     * Two [PythonProcess] are equal if their parameters are equal
     */
    override fun equals(other: Any?): Boolean {
      return other is PythonProcess && other.parameters == this.parameters
    }

    /**
     * Hashes the [PythonProcess].
     * The hash is based on the parameters — must mirror [equals] (which only compares parameters)
     * to satisfy the equals/hashCode contract.
     */
    override fun hashCode(): Int {
      return parameters.hashCode()
    }
  }

  /**
   * The result of a python run
   */
  @Serializable
  data class PythonResult(
    /**
     * The command that has been executed
     */
    val command: String,
    /**
     * All lines of data that have been returned by the python process.
     */
    val data: List<String>,
    /**
     * The time when this result was created
     */
    val creationDate: @ms Double = nowMillis(),
  )


  companion object {
    /**
     * Marks lines that contain real "data" that is returned by the python process.
     * The trailing blank is part of the prefix and is stripped along with it.
     */
    const val DataPrefix: String = "[DATA] "

    /**
     * Terminates the answer to a command. Without it a command never returns.
     */
    const val SuccessMessage: String = "[OK]"

    /**
     * Starts a new Python process.
     *
     * Call [close] when finished
     */
    fun start(
      pythonExecutable: PythonExecutable = PythonExecutable.system,

      pythonScripFileName: String,
      /**
       * Working directory for the Python script execution
       */
      workingDirectory: File,

      parameters: List<String>,
    ): PythonRunner {
      return PythonRunner(pythonExecutable, pythonScripFileName, workingDirectory).also { it.initPythonRunner(parameters) }
    }

    /**
     * Splits one JSON array line — a Python list printed by the script — into its elements, each rendered back to
     * text rather than decoded: `["a", 1]` yields the two strings `"a"` (quotes included) and `1`.
     * A nested array comes back as Kotlin's rendering of a list rather than as JSON, which is why callers strip the
     * brackets and split on `", "`.
     */
    fun String.parsePythonLineToStringList(): List<String> {
      val pythonLineAsJsonArray = Json.parseToJsonElement(this) as JsonArray

      fun JsonElement.parsePythonLineToList(): String {
        return when (this) {
          is JsonArray -> map { it.parsePythonLineToList() }.toString()
          else -> toString()
        }
      }

      return pythonLineAsJsonArray.map { it.parsePythonLineToList() }
    }

    /**
     * Splits one JSON array line into its objects and hands each to [objectParser].
     * Every element has to be an object — an array of anything else throws instead of being skipped.
     */
    fun <T> String.parsePythonLineToObjectList(objectParser: (JsonObject) -> T): List<T> {
      val pythonLineAsJsonArray = Json.parseToJsonElement(this) as JsonArray

      fun JsonElement.parsePythonLineToList(): T {
        return when (this) {
          is JsonObject -> objectParser(this)
          else -> throw IllegalStateException("Unexpected JsonElement: $this")
        }
      }

      return pythonLineAsJsonArray.map { it.parsePythonLineToList() }
    }

    private val logger: Logger by LoggerDelegate()
  }
}
