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

import it.neckar.open.resources.getResourceSafe
import javafx.application.Application
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.text.Text
import javafx.stage.Stage
import net.miginfocom.layout.LC
import org.tbee.javafx.scene.layout.MigPane

/**
 */
class JavaFxRunner : Application() {
  @Throws(Exception::class)
  override fun start(primaryStage: Stage) {
    println("Thread.currentThread().getName() = " + Thread.currentThread().name)
    println("Platform.isFxApplicationThread() = " + Platform.isFxApplicationThread())
    primaryStage.title = "Da Title"
    val headLine = Text("Login")
    headLine.id = "headLine"
    val userNameLabel = Label("User Name")
    val passwordLabel = Label("Password")
    val userTextField = TextField()
    val passwordField = PasswordField()
    val actionTarget = Text()
    actionTarget.id = "actionTarget"
    val loginButton = Button("Login")
    loginButton.onAction = EventHandler { event: ActionEvent? -> actionTarget.text = "Login failed - check user name and password" }

    //StackPane root = new StackPane();
    //root.getChildren().addAll(button);
    //root.getChildren().addAll(new Button("S"));
    val layoutConstraints = LC()
    layoutConstraints.alignX("center")
    val pane = MigPane("wrap 2, align 50% 50%", "[][fill]", "")
    //GridPane pane = new GridPane();
    //pane.setAlignment(Pos.CENTER);
    //pane.setHgap(10);
    //pane.setVgap(10);
    pane.padding = Insets(25.0, 25.0, 25.0, 25.0)
    pane.add(headLine, "span")
    pane.add(userNameLabel, "")
    pane.add(userTextField, "")
    pane.add(passwordLabel, "")
    pane.add(passwordField, "")
    pane.add(loginButton, "span, alignx right")
    pane.add(actionTarget, "span")
    val scene = Scene(pane, 1920.0, 1080.0)
    primaryStage.scene = scene
    scene.stylesheets.add(javaClass.getResourceSafe("JavaFxRunner.css").toExternalForm())
    primaryStage.show()
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      launch(*args)
    }
  }
}
