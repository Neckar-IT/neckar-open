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
package it.neckar.open.app.env

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.writeString

/**
 * [SystemFileSystemAccess] against the real file system. One test for every target: what a JVM
 * service sees and what a Kotlin/Native binary sees must not differ.
 */
class SystemFileSystemAccessTest {
  // Random, because the jvm and the linuxX64 test binary may run at the same time.
  private val directory: Path = Path(SystemTemporaryDirectory, "neckar-it-env-test-${Random.nextInt(Int.MAX_VALUE)}")
  private val envFile: Path = Path(directory, ".env")

  init {
    SystemFileSystem.createDirectories(directory)
  }

  @AfterTest
  fun cleanUp() {
    SystemFileSystem.delete(envFile, mustExist = false)
    SystemFileSystem.delete(directory, mustExist = false)
  }

  @Test
  fun `reads a file it wrote`() {
    writeEnvFile("GITLAB_ACCESS_TOKEN=glpat-Ky7mQ2vX8nR4tL9wZ3aB\n")

    assertEquals("GITLAB_ACCESS_TOKEN=glpat-Ky7mQ2vX8nR4tL9wZ3aB\n", SystemFileSystemAccess.readFileOrNull(envFile))
    assertTrue(SystemFileSystemAccess.fileExists(envFile))
  }

  /** The `.env` files this reads are UTF-8: a value may carry a non-ASCII character. */
  @Test
  fun `reads a value beyond ASCII`() {
    writeEnvFile("GREETING=Grüße aus Mössingen\n")

    assertEquals("GREETING=Grüße aus Mössingen\n", SystemFileSystemAccess.readFileOrNull(envFile))
  }

  @Test
  fun `reads an empty file as an empty string`() {
    writeEnvFile("")

    assertEquals("", SystemFileSystemAccess.readFileOrNull(envFile))
  }

  @Test
  fun `answers null for a file that is not there`() {
    assertNull(SystemFileSystemAccess.readFileOrNull(envFile))
    assertTrue(SystemFileSystemAccess.fileExists(envFile).not())
  }

  @Test
  fun `answers null for a directory read as a file`() {
    assertNull(SystemFileSystemAccess.readFileOrNull(directory))
  }

  @Test
  fun `tells a directory from a file`() {
    assertTrue(SystemFileSystemAccess.directoryExists(directory))
    assertTrue(SystemFileSystemAccess.fileExists(directory).not())
  }

  @Test
  fun `resolves a relative path against the working directory`() {
    assertTrue(SystemFileSystemAccess.absolutePath(Path(".")).toString().startsWith("/"))
  }

  @Test
  fun `keeps a path it cannot resolve`() {
    val missing = Path(directory, "does-not-exist")

    assertEquals(missing, SystemFileSystemAccess.absolutePath(missing))
  }

  @Test
  fun `finds the env file it wrote and reads its values`() {
    writeEnvFile("GITLAB_ACCESS_TOKEN=glpat-Ky7mQ2vX8nR4tL9wZ3aB\n")

    val dotEnv = DotEnv.load(directory, EnvFileName.Default, SystemFileSystemAccess) { null }

    assertEquals("glpat-Ky7mQ2vX8nR4tL9wZ3aB", dotEnv["GITLAB_ACCESS_TOKEN"])
  }

  private fun writeEnvFile(content: String) {
    SystemFileSystem.sink(envFile).buffered().use { it.writeString(content) }
  }
}
