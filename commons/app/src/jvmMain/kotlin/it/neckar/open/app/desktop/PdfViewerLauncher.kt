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


/**
 *
 * PdfViewerLauncher class.
 *
 */
object PdfViewerLauncher {
  @JvmStatic
  val osName: String
    get() = System.getProperty("os.name")

  /**
   * Opens the given file in the PDF viewer of the current operating system.
   *
   * @param filePath the path of the file that is opened
   */
  @JvmStatic
  fun openFile(filePath: String) {
    val osName = osName

    when {
      osName.startsWith("Mac OS") -> {
        throw UnsupportedOperationException("Not implemented for Mac OS yet")
        //        Class<?> fileMgr = Class.forName( "com.apple.eio.FileManager" );
        //        Method openFile = fileMgr.getDeclaredMethod( "openFile", new Class[]{String.class} );
        //        openFile.invoke( null, file );
      }
      osName.startsWith("Windows") -> {
        // `start` is a cmd.exe builtin, not an executable, so it must run via `cmd /c`. The empty
        // "" is the window-title argument start expects before the file, otherwise start would treat
        // the quoted file as the title and open nothing.
        Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", "", filePath))
      }
      else -> {
        //assume Unix or Linux
        val pdfViewerExecutableNames = arrayOf("acroread", "evince")

        var pdfViewerExecutableName: String? = null

        var index = 0
        while (index < pdfViewerExecutableNames.size && pdfViewerExecutableName == null) {
          if (Runtime.getRuntime().exec(arrayOf("which", pdfViewerExecutableNames[index])).waitFor() == 0) {
            pdfViewerExecutableName = pdfViewerExecutableNames[index]
          }
          index++
        }

        if (pdfViewerExecutableName == null) {
          error("Could not find pdf viewer")
        }

        Runtime.getRuntime().exec(arrayOf(pdfViewerExecutableName, filePath))
      }
    }

  }
}
