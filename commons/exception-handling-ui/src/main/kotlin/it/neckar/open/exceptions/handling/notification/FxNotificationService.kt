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
package it.neckar.open.exceptions.handling.notification

import it.neckar.open.annotations.UiThread
import it.neckar.open.javafx.FxUtils
import it.neckar.open.unit.si.ms
import javafx.beans.InvalidationListener
import javafx.scene.control.ButtonType
import javafx.stage.Stage
import net.java.balloontip.styles.ToolTipBalloonStyle
import java.awt.Color
import java.util.LinkedList
import javax.annotation.concurrent.ThreadSafe
import javax.swing.Timer

/**
 * Shows notifications in a popup
 */
@ThreadSafe
class FxNotificationService {
  /**
   * The notification queue that contains the notifications that should be shown
   */
  @UiThread
  private val notificationQueue = LinkedList<Notification>()
  /**
   * Whether there is currently a notification visible
   */
  @UiThread
  private var notificationVisible = false

  /**
   * Show the given notification as soon as possible.
   * Will add the notification to the queue if another notification is currently visible
   */
  @UiThread
  fun showNotification(notification: Notification) {
    if (notificationVisible) {
      notificationQueue.add(notification)
      return
    }

    val dialog = object : FxBalloonDialog(notification) {
      override fun closeBalloon() {
        result = ButtonType.OK
        close()

        notificationVisible = false

        //Look if there is a notification available
        val nextNotification = notificationQueue.poll() ?: return

        //Wait for 1200 ms until the notification is shown
        //TODO Convert to JavaFX
        val timer = Timer(1200) { _ -> showNotification(nextNotification) }
        timer.isRepeats = false
        timer.start()
      }
    }

    notificationVisible = true


    val stage = FxUtils.stage
    dialog.initOwner(stage)
    position(dialog, stage)
    dialog.show()

    position(dialog, stage)
    val updatePositionListener = InvalidationListener {
      position(dialog, stage)
    }
    dialog.widthProperty().addListener(updatePositionListener)
    dialog.heightProperty().addListener(updatePositionListener)
    stage.xProperty().addListener(updatePositionListener)
    stage.yProperty().addListener(updatePositionListener)
    stage.widthProperty().addListener(updatePositionListener)
    stage.heightProperty().addListener(updatePositionListener)

    //TimingUtils.showTimedBalloon(balloonTip, VisibilityTime);
    //TODO add timing to hide it
  }

  private fun position(dialog: FxBalloonDialog, stage: Stage) {
    dialog.x = stage.x + stage.width - dialog.width - 15
    dialog.y = stage.y + stage.height - dialog.height - 15
  }

  companion object {
    const val VisibilityTime: @ms Int = 15000

    val STYLE: ToolTipBalloonStyle = ToolTipBalloonStyle(Color(255, 251, 192), Color.GRAY)
  }
}
