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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThan
import assertk.assertions.prop
import it.neckar.open.time.monotonicTimeSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A shell script stands in for `xvfb-run -a ODAFileConverter`; it receives the converter's arguments as $1 (input
 * directory) and $2 (output directory) onwards.
 */
class DwgToDxfConverterTest {
  private fun converter(script: String, timeout: Duration = 10.seconds) = DwgToDxfConverter(listOf("sh", "-c", script, "converter"), timeout)

  @Test
  fun returnsTheDxfTheConverterWrites() {
    val dxf = converter("""cp "$1/input.dwg" "$2/input.dxf"""").convert("0\nEOF\n".encodeToByteArray())
    assertThat(dxf).isEqualTo("0\nEOF\n")
  }

  @Test
  fun aFailingConverterNamesItsExitCodeAndItsLastLinesWithoutTemporaryPaths() {
    assertFailure { converter("""for i in 1 2 3 4 5 6 7; do echo "line ${'$'}i in $1" >&2; done; exit 3""").convert(ByteArray(1)) }
      .isInstanceOf(DwgConversionException::class)
      .given { exception ->
        assertThat(exception.reason).isEqualTo(DwgConversionException.Reason.Failed(3))
        assertThat(exception.converterOutput).isEqualTo("line 3 in in | line 4 in in | line 5 in in | line 6 in in | line 7 in in")
        assertThat(exception.converterOutput).doesNotContain("dwg-to-dxf-")
      }
  }

  @Test
  fun aConverterBeyondItsTimeoutIsEndedWithItsChildren(@TempDir directory: File) {
    val pidFile = directory.resolve("converter-child.pid")
    val started = monotonicTimeSource.markNow()
    assertFailure { converter("sleep 30 & echo $! > ${pidFile.path}; wait", timeout = 300.milliseconds).convert(ByteArray(1)) }
      .isInstanceOf(DwgConversionException::class)
      .prop(DwgConversionException::reason).isEqualTo(DwgConversionException.Reason.TimedOut)
    assertThat(started.elapsedNow()).isLessThan(5.seconds)
    assertThat(ProcessHandle.of(pidFile.readText().trim().toLong()).map { it.isAlive }.orElse(false), "the child of the converter is alive").isEqualTo(false)
  }

  @Test
  fun anInterruptedCallerEndsTheConverter(@TempDir directory: File) {
    val executor = Executors.newSingleThreadExecutor()
    try {
      val pidFile = directory.resolve("converter-child.pid")
      val conversion: Future<String> = executor.submit<String> { converter("sleep 30 & echo $! > ${pidFile.path}; wait").convert(ByteArray(1)) }
      Thread.sleep(300.milliseconds.inWholeMilliseconds)
      val started = monotonicTimeSource.markNow()
      conversion.cancel(true)
      executor.shutdown()
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isEqualTo(true)
      assertThat(started.elapsedNow()).isLessThan(5.seconds)
      assertThat(ProcessHandle.of(pidFile.readText().trim().toLong()).map { it.isAlive }.orElse(false), "the child of the converter is alive").isEqualTo(false)
    } finally {
      executor.shutdownNow()
    }
  }

  @Test
  fun twoConversionsRunOneAfterTheOther(@TempDir directory: File) {
    // The script fails when another run holds the marker, as two xvfb-run -a would on one display.
    val marker = directory.resolve("converter.marker")
    val script = """mkdir "${marker.path}" || exit 9; sleep 0.3; rmdir "${marker.path}"; cp "$1/input.dwg" "$2/input.dxf""""
    val executor = Executors.newFixedThreadPool(2)
    try {
      val results = List(2) { executor.submit<String> { converter(script).convert("ok".encodeToByteArray()) } }.map { it.get() }
      assertThat(results).isEqualTo(listOf("ok", "ok"))
    } finally {
      executor.shutdownNow()
    }
  }

  @Test
  fun aConverterWithoutOutputReportsIt() {
    assertFailure { converter("echo unsupported version >&2").convert(ByteArray(1)) }
      .isInstanceOf(DwgConversionException::class)
      .given { exception ->
        assertThat(exception.reason).isEqualTo(DwgConversionException.Reason.NoOutput)
        assertThat(exception.message ?: "").contains("unsupported version")
      }
  }
}
