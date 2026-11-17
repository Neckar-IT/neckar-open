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

import it.neckar.open.javafx.Components.vbox5
import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ChangeListener
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.TextField
import javafx.stage.Stage
import javafx.util.Duration

class AdvancedTextFieldDemo : Application() {
  @Throws(Exception::class)
  override fun start(primaryStage: Stage) {
    val property: StringProperty = SimpleStringProperty()

    val advancedTextField = AdvancedTextField()
    advancedTextField.bindTextBidirectional(property)
    advancedTextField.textField.textProperty().addListener(ChangeListener { observable, oldValue, newValue ->
      if (newValue.isEmpty()) {
        return@ChangeListener
      }
      println("Text changed from <$oldValue> to <$newValue>")
      try {
        val parsedValue = newValue.toInt()
        if (parsedValue < 0) {
          advancedTextField.textField.text = "0"
        }
        if (parsedValue > 255) {
          advancedTextField.textField.text = "255"
        }
      } catch (e: NumberFormatException) {
        println("Could not parse")
        advancedTextField.textField.text = oldValue
      }
    })

    val advancedTextField2 = AdvancedTextField()
    advancedTextField2.addCommitHandler { advancedTextField1: AdvancedTextField?, text: String? -> property.value = text }

    val textField = TextField()
    textField.textProperty().bindBidirectional(property)

    val scene = Scene(vbox5(textField, advancedTextField, advancedTextField2), 800.0, 600.0)
    primaryStage.scene = scene
    primaryStage.show()

    val timeline = Timeline(KeyFrame(Duration.millis(500.0), object : EventHandler<ActionEvent?> {
      var counter = 0
      override fun handle(event: ActionEvent?) {
        counter++
        property.set(counter.toString())
      }
    }))
    timeline.cycleCount = Animation.INDEFINITE
    timeline.play()
  }
}
