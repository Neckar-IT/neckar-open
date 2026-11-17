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

import javafx.application.Application
import javafx.application.Application.launch
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.stage.Stage

/**
 */
fun main() {
  launch(CanvasMultipleTabsVisibleDemo::class.java)
}

class CanvasMultipleTabsVisibleDemo : Application() {
  override fun start(primaryStage: Stage) {

    val root = BorderPane()

    val tabPane = TabPane()
    root.center = tabPane

    tabPane.tabs.add(Tab("Tab 1", MyNode("1")))
    tabPane.tabs.add(Tab("Tab 2", MyNode("2")))

    primaryStage.scene = Scene(root, 800.0, 600.0)
    primaryStage.show()
  }
}

class MyNode(val label: String) : Pane() {
  init {
    children.add(Label("Hello $label"))

    println("initial visibility for $label: <$isVisible> for parent $parent")
    println("initial focus isFocusTraversable for $label: <$isFocusTraversable>")

    parentProperty().addListener { _, oldValue, newValue ->
      println("parent changed for $label from $oldValue to $newValue")
    }

    visibleProperty().addListener { _, oldValue, newValue ->
      println("Visibility changed for $label from <$oldValue> to <$newValue>")
    }
    focusTraversableProperty().addListener { _, oldValue, newValue ->
      println("focus traversable changed for $label from <$oldValue> to <$newValue>")
    }

  }
}
