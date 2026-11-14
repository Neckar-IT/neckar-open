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
package it.neckar.open.exceptions.handling

import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Creates the expandable content of an exception dialog: the stack trace of the given throwable in a read only text area
 */
internal fun createStacktraceExpandableContent(throwable: Throwable): GridPane {
  val stringWriter = StringWriter()
  throwable.printStackTrace(PrintWriter(stringWriter))

  val textArea = TextArea(stringWriter.toString())
  textArea.isEditable = false
  textArea.isWrapText = false

  textArea.maxWidth = Double.MAX_VALUE
  textArea.maxHeight = Double.MAX_VALUE
  GridPane.setVgrow(textArea, Priority.ALWAYS)
  GridPane.setHgrow(textArea, Priority.ALWAYS)

  return GridPane().also {
    it.maxWidth = Double.MAX_VALUE
    it.add(Label("Exception Stacktrace:"), 0, 0)
    it.add(textArea, 0, 1)
  }
}
