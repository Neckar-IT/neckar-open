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
package it.neckar.open.javafx

import it.neckar.open.javafx.properties.*
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ObservableValue
import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import java.util.Locale

/**
 * Displays a progress bar with a text indicator inside
 */
class ProgressIndicatorBar(
  val totalWork: Double,
  val labelFormatSpecifier: String,
  val unitLabel: String,
) : StackPane() {

  val workDoneProperty: DoubleProperty = SimpleDoubleProperty()
  var workDone: Double by workDoneProperty

  private val progressBar = ProgressBar()

  private val text = Text()

  init {
    progressBar.maxWidth = Double.MAX_VALUE
    text.fill = Color.BLACK
    text.font = Font.font(text.font.family, FontWeight.BOLD, 14.0)
    syncProgress()
    workDoneProperty.addListener { observable: ObservableValue<out Number?>?, oldValue: Number?, newValue: Number? -> syncProgress() }
    children.setAll(progressBar, text)
  }

  // synchronizes the progress indicated with the work done.
  private fun syncProgress() {
    if (totalWork < 0) {
      text.text = ""
      progressBar.progress = ProgressIndicator.INDETERMINATE_PROGRESS
    } else {
      text.text = String.format(Locale.ROOT, labelFormatSpecifier, workDone) + " " + unitLabel
      progressBar.progress = workDone / totalWork
    }
    progressBar.minHeight = text.boundsInLocal.height + DefaultLabelPadding * 2
    progressBar.minWidth = text.boundsInLocal.width + DefaultLabelPadding * 2
  }

  companion object {
    private const val DefaultLabelPadding = 2
  }
}
