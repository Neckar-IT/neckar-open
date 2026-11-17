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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.io.files.Path

class DotEnvTest {
  private val repositoryDirectories = setOf(
    "/home/user/projects/neckar-hub",
    "/home/user/projects/neckar-hub/.git",
    "/home/user/projects/neckar-hub/internal",
  )

  /** A process environment that carries nothing — the `.env` file alone answers. */
  private val emptyEnvironment: (String) -> String? = { null }

  @Test
  fun `reads the values of the env file it finds`() {
    val fileSystem = FakeFileSystemAccess(
      files = mapOf(
        "/home/user/projects/neckar-hub/.env" to
          """
          GITLAB_ACCESS_TOKEN=glpat-Ky7mQ2vX8nR4tL9wZ3aB
          GITLAB_PROJECT=neckarit/neckar-hub
          """.trimIndent()
      ),
      directories = repositoryDirectories,
    )

    val dotEnv = DotEnv.load(Path("/home/user/projects/neckar-hub/internal"), EnvFileName.Default, fileSystem, emptyEnvironment)

    assertEquals("glpat-Ky7mQ2vX8nR4tL9wZ3aB", dotEnv["GITLAB_ACCESS_TOKEN"])
    assertEquals("neckarit/neckar-hub", dotEnv["GITLAB_PROJECT"])
  }

  @Test
  fun `a missing env file is not an error`() {
    val fileSystem = FakeFileSystemAccess(files = emptyMap(), directories = repositoryDirectories)

    val dotEnv = DotEnv.load(Path("/home/user/projects/neckar-hub/internal"), EnvFileName.Default, fileSystem, emptyEnvironment)

    assertTrue(dotEnv.valuesFromFile.isEmpty())
    assertNull(dotEnv["GITLAB_ACCESS_TOKEN"])
  }

  @Test
  fun `the process environment wins over the env file`() {
    val dotEnv = DotEnv(mapOf("GITLAB_ACCESS_TOKEN" to "glpat-from-the-file")) { "glpat-from-the-process" }

    assertEquals("glpat-from-the-process", dotEnv["GITLAB_ACCESS_TOKEN"])
    assertEquals("glpat-from-the-file", dotEnv.valuesFromFile.getValue("GITLAB_ACCESS_TOKEN"))
  }

  @Test
  fun `falls back to the env file when the process environment does not carry the value`() {
    val dotEnv = DotEnv(mapOf("GITLAB_ACCESS_TOKEN" to "glpat-from-the-file"), emptyEnvironment)

    assertEquals("glpat-from-the-file", dotEnv["GITLAB_ACCESS_TOKEN"])
  }

  @Test
  fun `an empty dot env carries nothing`() {
    assertTrue(DotEnv.empty(emptyEnvironment).valuesFromFile.isEmpty())
    assertNull(DotEnv.empty(emptyEnvironment)["GITLAB_ACCESS_TOKEN"])
  }

  @Test
  fun `answers from the process environment even without an env file`() {
    val dotEnv = DotEnv.empty { "glpat-from-the-process" }

    assertEquals("glpat-from-the-process", dotEnv["GITLAB_ACCESS_TOKEN"])
  }

  /** `KEY=` is a leftover, not a configured empty string — the same rule the environment side follows. */
  @Test
  fun `a blank value in the file counts as absent`() {
    val dotEnv = DotEnv(mapOf("GITLAB_ACCESS_TOKEN" to "", "GITLAB_PROJECT" to "   "), emptyEnvironment)

    assertNull(dotEnv["GITLAB_ACCESS_TOKEN"])
    assertNull(dotEnv["GITLAB_PROJECT"])
    assertEquals("", dotEnv.valuesFromFile.getValue("GITLAB_ACCESS_TOKEN"))
  }
}
