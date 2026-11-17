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
package it.neckar.open.test

import eu.mihosoft.vrl.workflow.FlowFactory
import eu.mihosoft.vrl.workflow.fx.FXSkinFactory
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage

/**
 */
class VFlowRunner : Application() {

  override fun start(primaryStage: Stage) {
    val flow = FlowFactory.newFlow()
    flow.isVisible = true
    val n1 = flow.newNode()
    n1.id = "id1"
    val n2 = flow.newNode()
    n1.addInput("data")
    n1.addOutput("data")
    n2.addInput("data")
    n2.addOutput("data")
    val connectionResult = flow.connect(n1, n2, "data")
    println("connectionResult = $connectionResult")
    n1.x = 10.0
    n1.y = 10.0
    n1.width = 160.0
    n1.height = 100.0
    n2.x = 110.0
    n2.y = 10.0
    n2.width = 160.0
    n2.height = 100.0

    //ScalableContentPane canvas = new ScalableContentPane();
    val canvas = Pane()
    canvas.style = "-fx-background-color: linear-gradient(to bottom, rgb(10,32,60), rgb(42,52,120));"
    val fXSkinFactory = FXSkinFactory(canvas)
    flow.setSkinFactories(fXSkinFactory)

    primaryStage.scene = Scene(canvas, 1920.0, 1080.0)
    primaryStage.show()
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      launch(*args)
    }
  }
}
