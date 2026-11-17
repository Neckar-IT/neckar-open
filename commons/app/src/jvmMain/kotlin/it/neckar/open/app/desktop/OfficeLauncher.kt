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
package it.neckar.open.app.desktop


import java.io.File
import java.io.IOException
import java.util.Collections

/**
 *
 * OfficeLauncher class.
 *
 */
object OfficeLauncher {
  const val OpenOfficeWindows23: String = "c:\\Programme\\Openoffice.org 2.3\\program\\soffice.exe"
  const val OpenOfficeWindows: String = "c:\\Programme\\Openoffice.org 2.4\\program\\soffice.exe"
  const val OpenOfficeLinux: String = "/usr/bin/soffice"
  const val ExcelWindows: String = "c:\\Programme\\Microsoft Office\\Office\\excel.exe"

  /**
   * The absolute paths of the executables that can open a text document, in the order they are tried.
   */
  private val writerExecutablePaths = ArrayList<String>()

  /**
   * The absolute paths of the executables that can open a spreadsheet, in the order they are tried.
   */
  private val spreadsheetExecutablePaths = ArrayList<String>()

  init {
    writerExecutablePaths.add(OpenOfficeLinux)
    writerExecutablePaths.add(OpenOfficeWindows)
    writerExecutablePaths.add(OpenOfficeWindows23)

    spreadsheetExecutablePaths.add(OpenOfficeLinux)
    spreadsheetExecutablePaths.add(OpenOfficeWindows)
    spreadsheetExecutablePaths.add(OpenOfficeWindows23)
    spreadsheetExecutablePaths.add(ExcelWindows)
  }

  @JvmStatic
  fun getSpreadsheetExecutablePaths(): List<String> {
    return Collections.unmodifiableList(spreadsheetExecutablePaths)
  }

  @JvmStatic
  fun getWriterExecutablePaths(): List<String> {
    return Collections.unmodifiableList(writerExecutablePaths)
  }

  /**
   *
   * openWriter
   *
   * @param file a File object.
   * @return a Process object.
   *
   * @throws IOException if any.
   */
  @JvmStatic
  fun openWriter(file: File): Process {
    return openFile(writerExecutablePaths, file)
  }

  /**
   *
   * openSpreadsheet
   *
   * @param file a File object.
   * @return a Process object.
   *
   * @throws IOException if any.
   */
  @JvmStatic
  fun openSpreadsheet(file: File): Process {
    return openFile(spreadsheetExecutablePaths, file)
  }

  private fun openFile(candidateExecutablePaths: Iterable<String>, file: File): Process {
    for (candidateExecutablePath in candidateExecutablePaths) {
      if (File(candidateExecutablePath).exists()) {
        return Runtime.getRuntime().exec(arrayOf(candidateExecutablePath, file.absolutePath))
      }
    }
    error("No Office installation found...")
  }
}
