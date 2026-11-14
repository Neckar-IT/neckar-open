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
package it.neckar.open.javafx.combo

import it.neckar.open.javafx.Components
import it.neckar.open.javafx.Components.button
import javafx.application.Application
import javafx.beans.Observable
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Scene
import javafx.scene.control.ComboBox
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import javafx.util.Callback
import java.util.function.Consumer

fun main() {
  Application.launch(ComboBoxWithOptionalDemo::class.java)
}

class ComboBoxWithOptionalDemo() : Application() {
  private val options = CrossBeamConfig.entries.map {
    ItemThatMayBeDisabled(it)
  }.also {
    //Disable two options
    it[1].disabled = true
    it[3].disabled = true
  }

  private val observableOptions: ObservableList<ItemThatMayBeDisabled<CrossBeamConfig>> = FXCollections.observableArrayList(Callback<ItemThatMayBeDisabled<CrossBeamConfig>, Array<Observable>> { param -> arrayOf(param.disabledProperty) }).also {
    it.addAll(options)
  }

  private val property: ObjectProperty<CrossBeamConfig> = SimpleObjectProperty(CrossBeamConfig.Crossbeam7x)

  override fun start(primaryStage: Stage) {
    val root = BorderPane()

    property.addListener { _, oldValue, newValue -> println("Property changed from $oldValue to $newValue") }

    val comboBox: ComboBox<ItemThatMayBeDisabled<CrossBeamConfig>> = Components.comboBoxWithOptionalItems(property, observableOptions) {
      it.name
    }

    root.center = comboBox
    root.bottom = button("toggle") {
      options.forEach(Consumer { item: ItemThatMayBeDisabled<CrossBeamConfig> -> item.disabled = !item.disabled })
    }

    primaryStage.scene = Scene(root, 800.0, 600.0)
    primaryStage.show()

  }
}

enum class CrossBeamConfig {
  Disabled,
  Crossbeam3x,
  Crossbeam5x,
  Crossbeam7x,
  Crossbeam9x,
}
