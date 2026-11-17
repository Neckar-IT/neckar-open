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

import it.neckar.open.file.requireIsDirectory
import it.neckar.open.file.requireIsFile
import java.io.File

/**
 * Represents a python executable
 */
sealed interface PythonExecutable {
  /**
   * What to hand to a `ProcessBuilder` as the command. Either a bare name resolved through the `PATH` or an absolute
   * path — never a path relative to the working directory, which for a Python process is the script's directory.
   */
  val pathToPython: String

  /**
   * Uses the installed python executable on the system.
   * The executable must be installed on the system and available in the PATH.
   */
  data object System : PythonExecutable {
    override val pathToPython: String
      get() = "python3"
  }

  /**
   * Uses the python executable of a virtual environment, laid out the way `venv` does it on Linux and macOS.
   * Both the directory and the executable are checked on construction, so an instance always points at a real
   * interpreter — a missing environment fails here and not when the first process is started.
   */
  data class InVirtualEnvironment(
    /**
     * The path to the virtual environment directory.
     */
    val venvDir: File,
  ) : PythonExecutable {

    val pythonExecutable: File = File(venvDir, "bin/python3")

    init {
      venvDir.requireIsDirectory { "The venv directory could not be found @ ${it.absolutePath}" }
      pythonExecutable.requireIsFile { "The python executable could not be found @ ${it.absolutePath}" }
    }

    override val pathToPython: String
      get() = pythonExecutable.absolutePath

    /**
     * Returns the executable as file
     */
    @Deprecated("Use pythonExecutable instead", ReplaceWith("pythonExecutable"))
    fun asFile(): File {
      return pythonExecutable
    }
  }

  /**
   * The [InVirtualEnvironment] counterpart for Windows, where `venv` puts the interpreter in `Scripts/python.exe`
   * instead of `bin/python3`. Which of the two applies is the caller's decision — nothing here detects the platform.
   */
  data class InVirtualEnvironmentWindows(
    /**
     * The path to the virtual environment directory.
     */
    val venvDir: File,
  ) : PythonExecutable {
    val pythonExecutable: File = File(venvDir, "Scripts/python.exe")

    init {
      venvDir.requireIsDirectory { "The venv directory could not be found @ ${it.absolutePath}" }
      pythonExecutable.requireIsFile { "The python executable could not be found @ ${it.absolutePath}" }
    }

    override val pathToPython: String
      get() = pythonExecutable.absolutePath
  }

  companion object {
    /**
     * Returns a python executable in a virtual environment.
     * The default is relative and therefore resolved against the working directory of the JVM, not against the
     * module — it only fits a process started from the module directory.
     */
    fun inVirtualEnvironment(venvDir: File = File("build/venv")): InVirtualEnvironment {
      return InVirtualEnvironment(venvDir)
    }

    val system: System = System
  }
}
