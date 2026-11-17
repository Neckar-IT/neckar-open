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
package it.neckar.open.javafx.threed

import it.neckar.open.unit.other.deg
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.SubScene
import javafx.scene.input.GestureEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.transform.Rotate
import javafx.scene.transform.Translate

/**
 * A utility class that converts [MouseEvent] events and [ScrollEvent] events
 * into rotations of a [Node] which must be a child of a [SubScene].
 */
class SubSceneInputEventSupport(subScene: SubScene) {
  private var mousePosX = 0.0
  private var mousePosY = 0.0
  private var mouseOldX = 0.0
  private var mouseOldY = 0.0

  private val rotateX: Rotate = Rotate(0.0, Rotate.X_AXIS)
  private val rotateY: Rotate = Rotate(0.0, Rotate.Y_AXIS)
  private val rotateZ: Rotate = Rotate(0.0, Rotate.Z_AXIS)

  private val cameraTranslate: Translate

  init {
    val camera = subScene.camera
    cameraTranslate = Translate(0.0, 0.0, 0.0)
    camera.transforms.add(cameraTranslate)
    subScene.onScroll = EventHandler { event: ScrollEvent ->
      val scale = getTranslateZScale(event)
      val oldZ = cameraTranslate.z
      val newZ = oldZ + event.deltaY * scale
      cameraTranslate.z = newZ
    }
    subScene.onMousePressed = EventHandler { event: MouseEvent ->
      mousePosX = event.sceneX
      mousePosY = event.sceneY
      mouseOldX = mousePosX
      mouseOldY = mousePosY
      if (event.clickCount == 2) {
        resetAll()
      }
    }
    subScene.onMouseDragged = EventHandler { event: MouseEvent ->
      mouseOldX = mousePosX
      mouseOldY = mousePosY
      mousePosX = event.sceneX
      mousePosY = event.sceneY
      val mouseDeltaX = mousePosX - mouseOldX
      val mouseDeltaY = mousePosY - mouseOldY
      if (event.isPrimaryButtonDown) {
        if (event.isAltDown) { //roll
          val mouseDelta = if (Math.abs(mouseDeltaX) > Math.abs(mouseDeltaY)) mouseDeltaX else mouseDeltaY
          rotateZ.angle = computeNewAngle(rotateZ.angle, mouseDelta * getRotateZScale(event))
        } else {
          val scale = getRotateXYScale(event)
          rotateY.angle = computeNewAngle(rotateY.angle, mouseDeltaX * scale)
          rotateX.angle = computeNewAngle(rotateX.angle, -mouseDeltaY * scale)
        }
      } else {
        val scale = getTranslateXYScale(event)
        cameraTranslate.x -= mouseDeltaX * scale
        cameraTranslate.y -= mouseDeltaY * scale
      }
    }
  }

  fun install(node: Node) {
    node.transforms.addAll(rotateX, rotateY, rotateZ)
  }

  fun resetRotation() {
    rotateX.angle = 0.0
    rotateY.angle = 0.0
    rotateZ.angle = 0.0
  }

  fun resetTranslation() {
    cameraTranslate.x = 0.0
    cameraTranslate.y = 0.0
    cameraTranslate.z = 0.0
  }

  fun resetAll() {
    resetTranslation()
    resetRotation()
  }

  companion object {
    private const val AngleFullCircle: @deg Int = 360

    private fun computeNewAngle(currentAngle: Double, offset: Double): Double {
      return (currentAngle + offset) % AngleFullCircle
    }

    private fun getRotateXYScale(event: MouseEvent): Double {
      return selectScale(event.isControlDown, event.isShiftDown, plain = 0.75, control = 0.1, shift = 2.0)
    }

    private fun getRotateZScale(event: MouseEvent): Double {
      return selectScale(event.isControlDown, event.isShiftDown, plain = 2.0, control = 1.0, shift = 5.0)
    }

    private fun getTranslateXYScale(event: MouseEvent): Double {
      return selectScale(event.isControlDown, event.isShiftDown, plain = 2.0, control = 0.5, shift = 5.0)
    }

    private fun getTranslateZScale(event: GestureEvent): Double {
      return selectScale(event.isControlDown, event.isShiftDown, plain = 25.0, control = 2.5, shift = 75.0)
    }

    /**
     * Returns the scale for the currently pressed modifier keys. Shift wins over control if both are pressed.
     */
    private fun selectScale(controlDown: Boolean, shiftDown: Boolean, plain: Double, control: Double, shift: Double): Double {
      return when {
        shiftDown -> shift
        controlDown -> control
        else -> plain
      }
    }
  }
}
