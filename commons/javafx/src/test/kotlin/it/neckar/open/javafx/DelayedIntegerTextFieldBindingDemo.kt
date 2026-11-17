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

import it.neckar.open.javafx.Components.label
import it.neckar.open.javafx.Components.textFieldIntegerDelayed
import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage

class DelayedIntegerTextFieldBindingDemo : Application() {
  override fun start(primaryStage: Stage) {
    val integerProperty: IntegerProperty = SimpleIntegerProperty(8000)

    val vBox = VBox(10.0)
    vBox.padding = Insets(20.0)
    vBox.children.add(label(Bindings.createStringBinding({ Integer.toString(integerProperty.get()) }, integerProperty)))

    val randomButton = Button("Random!")
    randomButton.onAction = EventHandler { evt: ActionEvent? -> integerProperty.set((8000 * Math.random()).toInt()) }
    vBox.children.add(randomButton)

    val hBox1 = HBox(10.0)
    hBox1.children.add(label("Delayed"))
    hBox1.children.add(textFieldIntegerDelayed(integerProperty))
    vBox.children.add(hBox1)

    val hBox2 = HBox(10.0)
    hBox2.children.add(label("Delayed, only positive"))
    hBox2.children.add(textFieldIntegerDelayed(integerProperty, { i: Int -> i >= 0 }))
    vBox.children.add(hBox2)

    val hBox3 = HBox(10.0)
    hBox3.children.add(label("Delayed, only even"))
    hBox3.children.add(textFieldIntegerDelayed(integerProperty, { i: Int -> i % 2 == 0 }))
    vBox.children.add(hBox3)

    val hBox4 = HBox(40.0)
    hBox4.children.add(label("Focus catcher"))
    hBox4.children.add(TextField())
    vBox.children.add(hBox4)

    primaryStage.scene = Scene(vBox)
    primaryStage.show()
  }
}
