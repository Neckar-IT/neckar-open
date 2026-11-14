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
package it.neckar.detekt

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class TempDirectoryOnlyViaJUnitTest {

  private val rule = TempDirectoryOnlyViaJUnit(Config.empty)

  @Test
  fun `flags File createTempFile behind the import`() {
    val findings = rule.lint(
      """
      import java.io.File

      fun input(): File = File.createTempFile("hash-input", ".bin")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("java.io.File.createTempFile")
    assertThat(findings[0].message).contains("@TempDir")
  }

  @Test
  fun `flags File createTempFile behind the star import`() {
    val findings = rule.lint(
      """
      import java.io.*

      fun input(): File = File.createTempFile("hash-input", ".bin")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags the fully qualified File createTempFile`() {
    val findings = rule.lint(
      """
      fun input(): java.io.File = java.io.File.createTempFile("hash-input", ".bin")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags Files createTempFile and createTempDirectory`() {
    val findings = rule.lint(
      """
      import java.nio.file.Files
      import java.nio.file.Path

      fun input(): Path = Files.createTempFile("hash-input", ".bin")
      fun directory(): Path = Files.createTempDirectory("work")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
  }

  @Test
  fun `flags the fully qualified Files createTempDirectory`() {
    val findings = rule.lint(
      """
      fun directory(): java.nio.file.Path = java.nio.file.Files.createTempDirectory("work")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags Files behind an alias`() {
    val findings = rule.lint(
      """
      import java.nio.file.Files as NioFiles
      import java.nio.file.Path

      fun directory(): Path = NioFiles.createTempDirectory("work")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags the imported kotlin io path functions`() {
    val findings = rule.lint(
      """
      import java.nio.file.Path
      import kotlin.io.path.createTempDirectory
      import kotlin.io.path.createTempFile

      fun directory(): Path = createTempDirectory("work")
      fun input(): Path = createTempFile("hash-input", ".bin")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
    assertThat(findings[0].message).contains("kotlin.io.path.createTempDirectory")
  }

  @Test
  fun `flags the kotlin io path function behind an alias`() {
    val findings = rule.lint(
      """
      import java.nio.file.Path
      import kotlin.io.path.createTempDirectory as workDirectory

      fun directory(): Path = workDirectory("work")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags the kotlin io path function behind the star import`() {
    val findings = rule.lint(
      """
      import java.nio.file.Path
      import kotlin.io.path.*

      fun directory(): Path = createTempDirectory("work")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags the fully qualified kotlin io path function`() {
    val findings = rule.lint(
      """
      fun directory(): java.nio.file.Path = kotlin.io.path.createTempDirectory("work")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags the deprecated kotlin io functions`() {
    val findings = rule.lint(
      """
      import java.io.File
      import kotlin.io.createTempDir

      fun directory(): File = createTempDir("work")
      fun input(): File = kotlin.io.createTempFile("hash-input")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
  }

  @Test
  fun `flags the imported static member of Files`() {
    val findings = rule.lint(
      """
      import java.nio.file.Files.createTempDirectory
      import java.nio.file.Path

      fun directory(): Path = createTempDirectory("work")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `ignores an own function createTempDirectory behind the kotlin io path star import`() {
    val findings = rule.lint(
      """
      import java.io.File
      import kotlin.io.path.*

      fun createTempDirectory(parent: File): File = parent.resolve("work").apply { mkdirs() }

      fun directory(parent: File): File = createTempDirectory(parent)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores an own class File behind the star import`() {
    val findings = rule.lint(
      """
      import java.io.*

      class File {
        companion object {
          fun createTempFile(name: String): File = File()
        }
      }

      fun input(): File = File.createTempFile("hash-input")
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores createTempFile on another receiver`() {
    val findings = rule.lint(
      """
      import it.neckar.open.file.TempDirectory

      fun input() = TempDirectory.createTempFile("hash-input")
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
