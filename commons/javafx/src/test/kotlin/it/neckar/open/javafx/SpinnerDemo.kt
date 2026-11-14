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
@file:Suppress("RETURN_VALUE_NOT_USED")

package it.neckar.open.javafx

import it.neckar.open.formatting.decimalFormat
import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.Scene
import javafx.scene.control.Slider
import javafx.stage.Stage
import org.tbee.javafx.scene.layout.MigPane

/**
 *
 */
fun main() {
  Application.launch(SpinnerDemo::class.java)
}

class SpinnerDemo : Application() {
  override fun start(primaryStage: Stage) {
    val root = MigPane()

    val valueProperty = SimpleIntegerProperty(17)
    valueProperty.consume {
      println("Value changed to $it")
    }

    val slider = Components.spinner(valueProperty)

    root.add(Components.label("Spinner:"))
    root.add(slider, "wrap")
    root.add(Components.label("Value:"))
    root.add(Components.label(valueProperty.map { decimalFormat.format(it.toDouble()) }), "wrap")

    //root.add(Components.label("Min:"))
    //root.add(Components.label(slider.minProperty().map { decimalFormat.format(it.toDouble()) }), "wrap")
    //root.add(Components.label("Max:"))
    //root.add(Components.label(slider.maxProperty().map { decimalFormat.format(it.toDouble()) }), "wrap")

    root.add(Components.button("Set prop to 10.0") {
      valueProperty.value = 10
    }, "")
    root.add(Components.button("Set prop to 90.0") {
      valueProperty.value = 90
    }, "wrap")

    //root.add(Components.button("Set max to 50.0") {
    //  slider.max = 50.0
    //}, "")
    //root.add(Components.button("Set max to 100.0") {
    //  slider.max = 100.0
    //}, "wrap")

    primaryStage.scene = Scene(root, 800.0, 600.0)
    primaryStage.show()
  }
}
