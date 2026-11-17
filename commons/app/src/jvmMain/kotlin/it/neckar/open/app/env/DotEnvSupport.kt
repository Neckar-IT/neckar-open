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

import kotlinx.io.files.Path

/**
 * The repository's `.env` files as a JVM process sees them, each read once. The one place binding
 * [DotEnv] to the JVM; the Kotlin/Native counterpart is `loadDotEnv` in `linuxX64Main`.
 *
 * ```
 * val token = DotEnvSupport.dotEnv["GITLAB_ACCESS_TOKEN"]
 * ```
 */
object DotEnvSupport {
  /**
   * The checkout a command line tool belongs to, set by its launcher script: the tool finds
   * the `.env` of its checkout from any working directory.
   */
  const val RepositoryRootVariable: String = "NIT_REPOSITORY_ROOT"

  /** The `.env` found from [startDirectory] upwards. */
  val dotEnv: DotEnv by lazy { loadDefault(::environmentVariable) }

  /** The `.env-for-unit-test` found from the working directory upwards: the one next to the module whose tests run. */
  val dotEnvForUnitTest: DotEnv by lazy { load(EnvFileName.ForUnitTest, Path(".")) }

  /** [dotEnv] as [environmentLookup] answers it. */
  internal fun loadDefault(environmentLookup: (String) -> String?): DotEnv = load(EnvFileName.Default, startDirectory(environmentLookup), environmentLookup)

  /** [RepositoryRootVariable] when set, else the working directory. */
  private fun startDirectory(environmentLookup: (String) -> String?): Path {
    return environmentLookup(RepositoryRootVariable)?.takeIf { it.isNotBlank() }?.let { Path(it) } ?: Path(".")
  }

  private fun load(envFileName: EnvFileName, startDirectory: Path, environmentLookup: (String) -> String? = ::environmentVariable): DotEnv =
    DotEnv.load(startDirectory = startDirectory, envFileName = envFileName, fileSystem = SystemFileSystemAccess, environmentLookup = environmentLookup)
}
