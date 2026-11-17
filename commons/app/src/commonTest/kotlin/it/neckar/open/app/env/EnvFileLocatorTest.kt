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
import kotlinx.io.files.Path

/**
 * The one upwards search in the repository. Runs on JVM and Kotlin/Native, which is the point:
 * a native binary must find the same `.env` the JVM services find.
 */
class EnvFileLocatorTest {
  private val repositoryDirectories = setOf(
    "/home/user/projects/neckar-hub",
    "/home/user/projects/neckar-hub/.git",
    "/home/user/projects/neckar-hub/internal",
    "/home/user/projects/neckar-hub/internal/open",
    "/home/user/projects/neckar-hub/internal/open/commons",
    "/home/user/projects/neckar-hub/internal/open/commons/app",
    "/home/user/projects",
    "/home/user",
    "/home",
    "/",
  )

  /** The upwards walk ends because [Path.parent] answers null at the root — asserted, not assumed. */
  @Test
  fun `walks up through parents and stops at the root`() {
    assertEquals(Path("/home/user/projects"), Path("/home/user/projects/neckar-hub").parent)
    assertNull(Path("/").parent)
  }

  @Test
  fun `finds the env file in the start directory`() {
    val fileSystem = FakeFileSystemAccess(
      files = mapOf("/home/user/projects/neckar-hub/internal/open/commons/app/.env" to "TOKEN=from-the-module"),
      directories = repositoryDirectories,
    )

    assertEquals(
      Path("/home/user/projects/neckar-hub/internal/open/commons/app/.env"),
      EnvFileLocator.findEnvFile(Path("/home/user/projects/neckar-hub/internal/open/commons/app"), EnvFileName.Default, fileSystem),
    )
  }

  @Test
  fun `walks up to the repository root`() {
    val fileSystem = FakeFileSystemAccess(
      files = mapOf("/home/user/projects/neckar-hub/.env" to "TOKEN=from-the-repository-root"),
      directories = repositoryDirectories,
    )

    assertEquals(
      Path("/home/user/projects/neckar-hub/.env"),
      EnvFileLocator.findEnvFile(Path("/home/user/projects/neckar-hub/internal/open/commons/app"), EnvFileName.Default, fileSystem),
    )
  }

  @Test
  fun `stops at the git root and never reads the home directory`() {
    val fileSystem = FakeFileSystemAccess(
      files = mapOf("/home/user/.env" to "TOKEN=someone-elses-credentials"),
      directories = repositoryDirectories,
    )

    assertNull(EnvFileLocator.findEnvFile(Path("/home/user/projects/neckar-hub/internal"), EnvFileName.Default, fileSystem))
  }

  @Test
  fun `stops at a worktree root where git is a file`() {
    val fileSystem = FakeFileSystemAccess(
      files = mapOf(
        "/home/user/projects/neckar-hub.agent.1/.git" to "gitdir: /home/user/projects/neckar-hub/.git/worktrees/agent-1",
        "/home/user/.env" to "TOKEN=someone-elses-credentials",
      ),
      directories = setOf(
        "/home/user/projects/neckar-hub.agent.1",
        "/home/user/projects/neckar-hub.agent.1/internal",
        "/home/user/projects",
        "/home/user",
        "/home",
        "/",
      ),
    )

    assertNull(EnvFileLocator.findEnvFile(Path("/home/user/projects/neckar-hub.agent.1/internal"), EnvFileName.Default, fileSystem))
  }

  @Test
  fun `picks the requested file name over the one next to it`() {
    val fileSystem = FakeFileSystemAccess(
      files = mapOf(
        "/home/user/projects/neckar-hub/.env" to "TOKEN=the-developers-own",
        "/home/user/projects/neckar-hub/internal/open/commons/app/.env-for-unit-test" to "TOKEN=abc123",
      ),
      directories = repositoryDirectories,
    )

    assertEquals(
      Path("/home/user/projects/neckar-hub/internal/open/commons/app/.env-for-unit-test"),
      EnvFileLocator.findEnvFile(Path("/home/user/projects/neckar-hub/internal/open/commons/app"), EnvFileName.ForUnitTest, fileSystem),
    )
  }

  @Test
  fun `answers null when no env file exists anywhere below the git root`() {
    val fileSystem = FakeFileSystemAccess(files = emptyMap(), directories = repositoryDirectories)

    assertNull(EnvFileLocator.findEnvFile(Path("/home/user/projects/neckar-hub/internal"), EnvFileName.Default, fileSystem))
  }

  @Test
  fun `answers null for a start directory that does not exist`() {
    val fileSystem = FakeFileSystemAccess(files = emptyMap(), directories = repositoryDirectories)

    assertNull(EnvFileLocator.findEnvFile(Path("/home/user/projects/neckar-hub/does-not-exist"), EnvFileName.Default, fileSystem))
  }
}
