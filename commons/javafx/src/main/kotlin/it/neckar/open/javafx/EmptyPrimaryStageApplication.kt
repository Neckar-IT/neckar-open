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
import javafx.application.Platform
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.*

/**
 * Represents an empty primary stage
 */
class EmptyPrimaryStageApplication : Application() {
  override fun start(primaryStage: Stage) {
    primaryStage.initStyle(StageStyle.UNDECORATED)
    primaryStage.title = javaClass.simpleName

    (instance as CompletableDeferred).complete(this)
    (primaryStageReference as CompletableDeferred).complete(primaryStage)
  }

  companion object {
    /**
     * Provides the instance to the application
     */
    val instance: Deferred<EmptyPrimaryStageApplication> = CompletableDeferred()

    /**
     * A deferred that is completed as soon as the primary stage has been initialized
     */
    val primaryStageReference: Deferred<Stage> = CompletableDeferred()

    /**
     * Stops the empty primary stage application and returns as soon as the application has been stopped
     *
     */
    suspend fun stop() {
      if (!isFxApplicationThreadRunning()) {
        //Not running
        return
      }

      withContext(Dispatchers.Main) {
        primaryStageReference.getCompleted().hide()
      }

      Platform.exit()
    }
  }
}

/**
 * Launches the empty primary stage application.
 *
 * This method creates a single thread context and launches the JavaFX application
 */
fun CoroutineScope.launchEmptyPrimaryStageApplication(): Job {
  return this.launch(newSingleThreadContext("Empty Primary Stage Application Launcher")) {
    Application.launch(EmptyPrimaryStageApplication::class.java)
  }.also {
    runBlocking {
      EmptyPrimaryStageApplication.primaryStageReference.await()
    }
  }
}
