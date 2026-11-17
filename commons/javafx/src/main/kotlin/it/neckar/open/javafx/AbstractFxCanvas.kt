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

import it.neckar.open.javafx.properties.getValue
import it.neckar.open.dispose.Disposable
import it.neckar.open.dispose.DisposeSupport
import it.neckar.open.unit.other.px
import it.neckar.open.unit.si.ns
import javafx.animation.AnimationTimer
import javafx.beans.Observable
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment

/**
 * Abstract base class for a canvas that is automatically repainted if necessary
 *
 *
 * Subclasses must call [.paint] whenever the model is updated
 *
 */
@Deprecated("Use classes from algorithms instead")
abstract class AbstractFxCanvas protected constructor() : Canvas(), Disposable {
  /**
   * Is set to true if a repaint is required
   */
  private var repaintRequired: Boolean = false

  /**
   * If set to true, the repaint is disabled.
   * This can be used to avoid unnecessary paintings - e.g. if the canvas is currently not shown
   */
  val repaintDisabledProperty: BooleanProperty = SimpleBooleanProperty()

  /**
   * If set to true the canvas does *not* repaint itself
   */
  val repaintDisabled: Boolean by repaintDisabledProperty

  /**
   * If set to true the x values are snapped to pixel.
   * Should be set to false while the x axis is animated
   */
  private val snapXValuesToPixel = SimpleBooleanProperty(true)

  /**
   * Should be disabled when the y axis is animated
   */
  private val snapYValuesToPixel = SimpleBooleanProperty(true)

  protected val disposeSupport: DisposeSupport = DisposeSupport()

  val isSnapXValuesToPixel: Boolean
    get() = snapXValuesToPixel.get()

  val isSnapYValuesToPixel: Boolean
    get() = snapYValuesToPixel.get()

  init {
    //Automatically mark as dirty on resize
    registerDirtyListener(widthProperty())
    registerDirtyListener(heightProperty())

    //Repaint when snap has changed
    registerDirtyListener(snapYValuesToPixel)
    registerDirtyListener(snapXValuesToPixel)

    repaintDisabledProperty.addListener { _, _, newValue ->
      if (newValue!!) {
        //Paint painting disabled text
        paintRepaintDisabled(graphicsContext2D)
      } else {
        //A initial repaint is necessary
        repaintRequired = true
      }
    }

    object : AnimationTimer() {
      override fun handle(@ns now: Long) {
        if (scene == null) {
          return
        }
        if (!isVisible) {
          return
        }
        if (repaintDisabled) {
          return
        }

        if (repaintRequired) {
          repaintRequired = false
          paint(graphicsContext2D)
        }
      }
    }.also {
      disposeSupport.onDispose {
        it.stop()
      }
      it.start()
    }
  }

  override fun dispose() {
    disposeSupport.dispose()
  }

  fun registerDirtyListener(property: Observable) {
    registerDirtyListener(this, property)
  }

  /**
   * Marks the canvas as dirty. It will be repainted on the next occasion
   */
  fun markAsDirty() {
    repaintRequired = true
  }

  /**
   * Paints a template text when paining is disabled.
   * This way it becomes obvious if somebody forgot to set [.isRepaintDisabled] back to false
   */
  protected fun paintRepaintDisabled(g2d: GraphicsContext) {
    clearBackground(g2d)
    g2d.fill = Color.LIGHTGRAY
    g2d.fillRect(0.0, 0.0, width, height)

    //Paint a text
    g2d.textAlign = TextAlignment.LEFT
    g2d.textBaseline = VPos.TOP

    g2d.fill = Color.DARKGRAY
    g2d.fillText("Repainting is disabled", 0.0, 0.0)
  }

  /**
   * Clears the background in a way that avoids memory leaks.
   */
  protected fun clearBackground(gc: GraphicsContext) {
    // workaround: dependency=libs:javafx:25.0.4 observed-on=2026-10-07
    // A clearRect limited to the canvas bounds leaks memory on JavaFX; the oversized rectangle does not.
    gc.clearRect(-1000.0, -1000.0, width + 2000, height + 2000)
  }

  /**
   * Paint the content.
   *
   *
   * Implementations probably want to call [.clearBackground] first
   */
  protected abstract fun paint(gc: GraphicsContext)

  fun snapXValuesToPixelProperty(): BooleanProperty {
    return snapXValuesToPixel
  }

  fun snapYValuesToPixelProperty(): BooleanProperty {
    return snapYValuesToPixel
  }

  @px
  protected fun snapXValue(@px value: Double): Double {
    return FxPaintingUtils.snapPosition(value, isSnapXValuesToPixel)
  }

  @px
  protected fun snapXSize(@px value: Double): Double {
    return FxPaintingUtils.snapSize(value, isSnapXValuesToPixel)
  }

  @px
  protected fun snapYValue(@px value: Double): Double {
    return FxPaintingUtils.snapPosition(value, isSnapYValuesToPixel)
  }

  @px
  protected fun snapYSize(@px value: Double): Double {
    return FxPaintingUtils.snapSize(value, isSnapYValuesToPixel)
  }

  protected fun paintDebugInfo(gc: GraphicsContext) {
    //Around
    gc.beginPath()
    gc.rect(snapXValue(0.0), snapYValue(0.0), snapXSize(width), snapYSize(height))
    gc.closePath()
    gc.stroke = Color.ORANGE
    gc.lineWidth = 3.0
    gc.stroke()

    //cross wire to mark the center
    gc.beginPath()
    gc.moveTo(snapXValue(0.0), snapYValue(height / 2.0))
    gc.lineTo(snapXValue(width), snapYValue(height / 2.0))
    gc.moveTo(snapXValue(width / 2.0), 0.0)
    gc.lineTo(snapXValue(width / 2.0), snapYValue(height))

    gc.closePath()
    gc.lineWidth = 2.0
    gc.stroke()

    //oval in the corners
    //Bottom right
    gc.strokeOval(snapXValue(width - 10), snapYValue(height - 10), snapXSize(20.0), snapYSize(20.0))
    //Top right
    gc.strokeOval(snapXValue(width - 10), snapYValue((0 - 10).toDouble()), snapXSize(20.0), snapYSize(20.0))
    //Bottom left
    gc.strokeOval(snapXValue((0 - 10).toDouble()), snapYValue(height - 10), snapXSize(20.0), snapYSize(20.0))
    //top left
    gc.strokeOval(snapXValue((0 - 10).toDouble()), snapYValue((0 - 10).toDouble()), snapXSize(20.0), snapYSize(20.0))
  }

  companion object {
    @px
    @JvmStatic
    protected fun snapSize(@px value: Double): Double {
      return FxPaintingUtils.snapSize(value, false)
    }

    @px
    @JvmStatic
    protected fun snapPosition(@px value: Double): Double {
      return FxPaintingUtils.snapPosition(value)
    }

    /**
     * Can be used to enable debug output
     */
    protected const val DebugEnabled: Boolean = false

    @JvmStatic
    fun registerDirtyListener(canvas: AbstractFxCanvas, property: Observable) {
      property.addListener { canvas.markAsDirty() }
    }
  }
}
