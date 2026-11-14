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
package it.neckar.open.javafx.busy

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.util.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Contains code that manages the busy state.
 *
 * Listens to changes of the given busy state.
 * To detect cases in which the busy state has already been set to false before [.registerCallback] has
 * been called, an additional check is performed after [.defaultManualCheckDelay].
 *
 * If the busy state is still false after [.defaultTimeoutDelay], the callback is notified about the timeout
 */
class BusySupport @JvmOverloads constructor(
  val busyStatusProperty: ReadOnlyBooleanProperty,
  /**
   * The delay when the first manual check is done.
   */
  val defaultManualCheckDelay: Duration = Duration.millis(500.0),
  val defaultTimeoutDelay: Duration = Duration.seconds(30.0),
) {

  /**
   * Calls the callback as soon as the busy status is false.
   * A manual check is performed after [.defaultManualCheckDelay] to detect cases where the busy state has already been set to false
   */
  @JvmOverloads
  fun registerCallback(callback: FinishedCallback, manualCheckDelay: Duration = defaultManualCheckDelay, timeoutDelay: Duration = defaultTimeoutDelay) {
    //Shared by the listener, the manual check and the timeout - whoever wins notifies, the others stay silent
    val finished = AtomicBoolean()

    //Listener for a change
    val listener: ChangeListener<Boolean> = object : ChangeListener<Boolean> {
      override fun changed(observable: ObservableValue<out Boolean>, oldValue: Boolean, newValue: Boolean) {
        if (oldValue == newValue) {
          //Avoid duplicate notifications
          return
        }
        if (!newValue) {
          //Remove the listener first to avoid multiple calls (if the callback itself sets busy to true again
          observable.removeListener(this)
          if (finished.compareAndSet(false, true)) {
            callback.finished()
          }
        }
      }
    }
    busyStatusProperty.addListener(listener)

    //Check after (default: 500 millis) to get very fast events
    Timeline(KeyFrame(manualCheckDelay, { event: ActionEvent? ->
      if (!busyStatusProperty.get()) {
        //Remove the listener first to avoid multiple calls (if the callback itself sets busy to true again
        busyStatusProperty.removeListener(listener)
        if (finished.compareAndSet(false, true)) {
          callback.finished()
        }
      }
    })).play()

    //Timeout check
    Timeline(KeyFrame(timeoutDelay, EventHandler { event: ActionEvent? ->
      if (finished.get()) {
        //Already notified, just return
        return@EventHandler
      }
      busyStatusProperty.removeListener(listener)

      if (!busyStatusProperty.get()) {
        if (finished.compareAndSet(false, true)) {
          callback.finished()
        }
      } else {
        //Still busy, timeout
        if (finished.compareAndSet(false, true)) {
          callback.timedOut()
        }
      }
    })).play()
  }

  companion object {
    /**
     * Creates a busy support that returns after a given timeout
     */
    @JvmStatic
    fun createTimed(seconds: Int): BusySupport {
      return BusySupport(SimpleBooleanProperty(true), Duration.millis(500.0), Duration.seconds(seconds.toDouble()))
    }
  }
}
