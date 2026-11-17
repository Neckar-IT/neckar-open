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
package it.neckar.open.app.ui.swing

import it.neckar.open.annotations.NonBlocking
import it.neckar.open.annotations.NonUiThread
import it.neckar.open.annotations.UiThread
import java.awt.SplashScreen
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/**
 */
class SwingApplicationRunner(
  val application: Application
) {
  @MainFrame
  @get:MainFrame
  var mainFrame: JFrame? = null
    private set

  init {
    application.prepareExceptionHandling()
    application.prepare()
  }

  @NonUiThread
  @NonBlocking
  fun boot() {
    SwingUtilities.invokeLater(Runnable {
      val splash = SplashScreen.getSplashScreen() ?: return@Runnable

      application.updateSplash(splash)
    })

    application.createFrame().let {
      mainFrame = it
      SwingUtilities.invokeLater {
        showFrame(it)
      }
    }
  }

  @UiThread
  private fun showFrame(frame: JFrame) {
    application.prepareFrameForShowing(frame)
    frame.pack()
    application.setSizeAndLocation(frame)
    frame.isVisible = true
  }


  interface Application {
    /**
     * Prepares the exception handling - e.g. registering a default exception handler
     */
    @NonUiThread
    fun prepareExceptionHandling()

    /**
     * Prepares the application.
     * E.g. should create the injector and return the frame (Guice: Key.get( JFrame.class, MainFrame.class ))
     */
    @NonUiThread
    fun prepare()

    /**
     * Is only called if there is a splash screen
     *
     * @param splash the splash
     */
    @UiThread
    fun updateSplash(splash: SplashScreen)

    /**
     * Creates the frame.
     * This method is called from the background thread. Do *not* call pack() or setVisible()
     *
     * @return the created frame
     */
    @NonUiThread
    fun createFrame(): JFrame

    /**
     * Is called before the frame is shown.
     *
     * @param frame the frame
     */
    @UiThread
    fun prepareFrameForShowing(frame: JFrame)

    /**
     * Sets the size and location for the given frame
     *
     * @param frame the frame
     */
    @UiThread
    fun setSizeAndLocation(frame: JFrame)
  }

  /**
   * Abstract base class for application
   */
  abstract class AbstractApplication : Application {
    override fun prepareFrameForShowing(frame: JFrame) {
      frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    }

    override fun setSizeAndLocation(frame: JFrame) {
      frame.setSize(1024, 768)
      frame.setLocationRelativeTo(null)
    }
  }
}
