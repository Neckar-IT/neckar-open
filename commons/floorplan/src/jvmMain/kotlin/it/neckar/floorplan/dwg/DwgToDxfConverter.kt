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
package it.neckar.floorplan.dwg

import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Converts a DWG to DXF text with the ODA File Converter, run as a process in a temporary directory.
 *
 * The converter is a Qt application that needs an X display; [command] starts it through `xvfb-run`, as the base image
 * `java-service-runner-with-oda` and `docker/fragments/ubuntu/floorplan.dockerfile` install it.
 */
class DwgToDxfConverter(
  private val command: List<String> = DefaultCommand,
  private val timeout: Duration = DefaultTimeout,
) {
  /**
   * The DXF text of [dwg].
   *
   * @throws DwgConversionException when the converter fails, outlasts [timeout] or writes no DXF
   * @throws InterruptedException when the calling thread is interrupted; the converter is ended
   */
  fun convert(dwg: ByteArray): String {
    // xvfb-run -a takes the first display without a lock file; two runs at once can take the same and one fails.
    ConversionLock.lockInterruptibly()
    try {
      return convertAlone(dwg)
    } finally {
      ConversionLock.unlock()
    }
  }

  private fun convertAlone(dwg: ByteArray): String {
    val workDirectory = Files.createTempDirectory("dwg-to-dxf-").toFile()
    try {
      val inputDirectory = File(workDirectory, "in").apply { mkdirs() }
      val outputDirectory = File(workDirectory, "out").apply { mkdirs() }
      // The converter keeps its settings below HOME; a home of its own leaves the service's untouched.
      // Owner only: Qt warns on stderr about a runtime directory others may read.
      val home = Files.createDirectory(workDirectory.toPath().resolve("home"), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))).toFile()
      val stderr = File(workDirectory, "stderr.txt")
      File(inputDirectory, "input.dwg").writeBytes(dwg)

      // Recurse 0, audit 1: the audit repairs structural defects, without it ODA writes a truncated DXF for some drawings.
      val process = ProcessBuilder(command + listOf(inputDirectory.path, outputDirectory.path, "ACAD2018", "DXF", "0", "1", "*.dwg"))
        .directory(workDirectory)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(stderr)
        .apply { environment()["HOME"] = home.path; environment()["XDG_RUNTIME_DIR"] = home.path }
        .start()
      try {
        if (process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS).not()) {
          throw DwgConversionException(DwgConversionException.Reason.TimedOut, converterOutput(stderr, workDirectory))
        }
      } finally {
        end(process)
      }
      if (process.exitValue() != 0) {
        throw DwgConversionException(DwgConversionException.Reason.Failed(process.exitValue()), converterOutput(stderr, workDirectory))
      }
      val dxf = File(outputDirectory, "input.dxf")
      if (dxf.isFile.not()) {
        throw DwgConversionException(DwgConversionException.Reason.NoOutput, converterOutput(stderr, workDirectory))
      }
      return dxf.readText()
    } finally {
      workDirectory.deleteRecursively()
    }
  }

  /**
   * Ends [process] and the Xvfb and converter xvfb-run started: SIGTERM first, so xvfb-run removes its display lock.
   */
  private fun end(process: Process) {
    if (process.isAlive.not()) {
      return
    }
    val handles = process.descendants().toList() + process.toHandle()
    handles.forEach { it.destroy() }
    val deadline = System.nanoTime() + TerminationGrace.inWholeNanoseconds
    for (handle in handles) {
      try {
        handle.onExit().get(maxOf(0L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)
      } catch (_: TimeoutException) {
        handle.destroyForcibly()
      } catch (_: InterruptedException) {
        // A second interrupt ends the grace; the caller learns of it through the flag.
        Thread.currentThread().interrupt()
        handles.forEach { it.destroyForcibly() }
        return
      }
    }
  }

  /**
   * The last lines the converter wrote to stderr, with the temporary directory left out of every path.
   */
  private fun converterOutput(stderr: File, workDirectory: File): String {
    if (stderr.isFile.not()) {
      return ""
    }
    val tail = RandomAccessFile(stderr, "r").use { file ->
      val start = maxOf(0L, file.length() - OutputReadBytes)
      ByteArray((file.length() - start).toInt()).also { file.seek(start); file.readFully(it) }
    }
    return tail.decodeToString()
      .replace(workDirectory.path + File.separator, "")
      .replace(workDirectory.path, ".")
      .lines()
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .takeLast(OutputTailLines)
      .joinToString(" | ")
      .takeLast(OutputTailChars)
  }

  companion object {
    val DefaultCommand: List<String> = listOf("xvfb-run", "-a", "ODAFileConverter")

    /**
     * The example DWGs of up to 4.1 MB convert in 0.7 to 1.4 s.
     */
    val DefaultTimeout: Duration = 30.seconds

    private const val OutputTailLines: Int = 5
    private const val OutputTailChars: Int = 400

    /**
     * The end of stderr that holds the tail; the converter may write much more.
     */
    private const val OutputReadBytes: Long = 16 * 1024

    /**
     * The time Xvfb and the converter get to end on SIGTERM before SIGKILL.
     */
    private val TerminationGrace: Duration = 2.seconds

    /**
     * Shared by every converter of this process, as they share the X displays.
     */
    private val ConversionLock: ReentrantLock = ReentrantLock()
  }
}
