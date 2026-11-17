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

import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.util.Duration
import org.tbee.javafx.scene.layout.MigPane
import java.util.concurrent.ThreadLocalRandom

class ProgressIndicatorBarDemo : Application() {
  @Throws(Exception::class)
  override fun start(primaryStage: Stage) {
    val root = MigPane("", "[grow, fill]", "[grow, fill]")

    val progressIndicatorBar1 = ProgressIndicatorBar(20.0, "%.1f", "mA")
    root.add(progressIndicatorBar1, "alignx center, aligny center, wrap")

    val progressIndicatorBar2 = ProgressIndicatorBar(100.0, "%.0f", "%")
    root.add(progressIndicatorBar2, "alignx center, aligny center, wrap")

    val progressIndicatorBar3 = ProgressIndicatorBar(-1.0, "%.0f", "%")
    root.add(progressIndicatorBar3, "alignx center, aligny center")

    val timeLine = Timeline(KeyFrame(Duration.millis(50.0), { event: ActionEvent? ->
      val workDone = progressIndicatorBar1.workDone
      if (workDone < 20.0) {
        progressIndicatorBar1.workDone = workDone + 0.1
      } else {
        progressIndicatorBar1.workDone = 4.0
      }
      progressIndicatorBar2.workDone = (ThreadLocalRandom.current().nextInt(0, 100 + 1)).toDouble()
    }))
    timeLine.cycleCount = Animation.INDEFINITE
    timeLine.play()

    primaryStage.scene = Scene(root, 200.0, 100.0)
    primaryStage.show()
  }
}
