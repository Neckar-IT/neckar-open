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

import it.neckar.open.unit.other.px
import it.neckar.open.unit.si.ns
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.Node
import javafx.scene.input.MouseEvent

/**
 * Helper class that offers dragging related calculations
 */
class FXDragSupport<T : Node>(
  private val node: T,
) {

  var isDragging: Boolean = false
    private set

  /**
   * The current location of the mouse pointer.
   * Is updated on every drag
   */
  private var mouseX: @px Double = 0.0
  private var mouseY: @px Double = 0.0

  /**
   * The time when the mouse coordinates have been updated
   */
  private var mouseTime: @ns Long = 0

  /**
   * If set to false, dragging is not allowed.
   * Only relevant when dragging is started
   */
  private val draggingAllowed: BooleanProperty = SimpleBooleanProperty(true)

  /**
   * Used to calculate the mouse speeds
   */
  private val mouseDragSpeedCalculator = MouseDragSpeedCalculator()

  fun install(handler: Handler<T>) {
    //Listen for dragging event
    node.addEventHandler(MouseEvent.MOUSE_DRAGGED) { event: MouseEvent ->
      if (!isDragging) {
        if (!draggingAllowed.get()) {
          //dragging currently not allowed
          return@addEventHandler
        }
        isDragging = true
        updateMouseLocation(event)
        handler.dragStartDetected(node, mouseX, mouseY)
      }
      val deltaX: @px Double = event.sceneX - mouseX
      val deltaY: @px Double = event.sceneY - mouseY
      val deltaTime: @ns Long = updateMouseLocation(event)
      mouseDragSpeedCalculator.add(deltaTime, deltaX, deltaY)
      handler.dragged(node, deltaX, deltaY)
      event.consume()
    }

    //Disable dragging on mouse released
    node.addEventHandler(MouseEvent.MOUSE_RELEASED) { event: MouseEvent ->
      if (!isDragging) {
        return@addEventHandler
      }
      isDragging = false
      handler.dragFinished(node, event.x, event.y)
      event.consume()
    }
  }

  @IgnorableReturnValue
  private fun updateMouseLocation(event: MouseEvent): @ns Long {
    mouseX = event.sceneX
    mouseY = event.sceneY
    val now: @ns Long = System.nanoTime()
    val delta: @ns Long = now - mouseTime
    mouseTime = now
    return delta
  }

  fun isDraggingAllowed(): Boolean {
    return draggingAllowed.get()
  }

  fun draggingAllowedProperty(): BooleanProperty {
    return draggingAllowed
  }

  /**
   * Callback that is registered at the drag support
   */
  interface Handler<T> {
    /**
     * The start of drag has been detected
     *
     * @param mouseX the x location within the dragged node
     * @param mouseY the y location within the dragged node
     */
    fun dragStartDetected(draggedNode: T, mouseX: @px Double, mouseY: @px Double)

    /**
     * Is called if a drag has been detected
     *
     * @param draggedNode the node that has been detected
     */
    fun dragged(draggedNode: T, deltaX: @px Double, deltaY: @px Double)

    /**
     * Is called if a drag has been finished
     */
    fun dragFinished(draggedNode: T, x: @px Double, y: @px Double)
  }
}
