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

import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Glow
import javafx.scene.effect.Light
import javafx.scene.effect.Lighting
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontSmoothingType
import javafx.scene.text.Text
import javafx.stage.Stage
import org.jfree.fx.FXGraphics2D
import org.tbee.javafx.scene.layout.MigPane
import javax.annotation.Nonnull

/**
 */
class CanvasRunner : Application() {
  @Throws(Exception::class)
  override fun init() {
    super.init()
    val parameters = parameters
    println("-----------")
    for ((key, value) in parameters.named) {
      println("$key: $value")
    }
    println("-----------")
    for (s in parameters.raw) {
      println("s = $s")
    }
    println("-----------")
    for (s in parameters.unnamed) {
      println("s = $s")
    }
  }

  @Throws(Exception::class)
  override fun start(primaryStage: Stage) {
    println("Thread.currentThread().getName() = " + Thread.currentThread().name)
    println("Platform.isFxApplicationThread() = " + Platform.isFxApplicationThread())
    val headLine = Text("Canvas Test")
    headLine.id = "headLine"
    primaryStage.titleProperty().bind(headLine.textProperty())
    val root = Group()
    val canvas = Canvas(1280.0, 720.0)

    //drawShapes(canvas);
    canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED) { event ->
      println("CanvasRunner.handle")
      canvas.graphicsContext2D.clearRect(event.x - 2, event.y - 2, 5.0, 5.0)
    }
    canvas.addEventHandler(MouseEvent.MOUSE_CLICKED) {
      canvas.graphicsContext2D.fill = Color.ORANGE
      canvas.graphicsContext2D.fillRect(0.0, 0.0, canvas.width, canvas.height)
      val g2d = FXGraphics2D(canvas.graphicsContext2D)
      g2d.color = java.awt.Color.DARK_GRAY
      g2d.drawString("Hello World", 10, 10)
    }


    //Layout
    val pane = MigPane("wrap 1, align 50% 50%, fill", "[fill,grow, align center]", "[][fill,grow]")
    pane.padding = Insets(25.0, 25.0, 25.0, 25.0)
    pane.add(headLine, "span")
    root.children.add(canvas)
    pane.add(root)
    val scene = Scene(pane, 1920.0, 1080.0)
    primaryStage.scene = scene
    //scene.getStylesheets().add(getClass().getResource("JavaFxRunner.css").toExternalForm());
    primaryStage.show()
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      launch(*args)
    }
  }
}
