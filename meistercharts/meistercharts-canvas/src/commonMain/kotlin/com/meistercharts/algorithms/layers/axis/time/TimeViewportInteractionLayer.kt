/*
 * Copyright 2023 Neckar IT GmbH, Mössingen, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meistercharts.algorithms.layers.axis.time

import com.meistercharts.algorithms.layers.AbstractLayer
import com.meistercharts.algorithms.layers.LayerPaintingContext
import com.meistercharts.algorithms.layers.LayerType
import com.meistercharts.canvas.ChartSupport
import com.meistercharts.canvas.DirtyReason
import com.meistercharts.canvas.events.CanvasMouseEventHandler
import com.meistercharts.events.EventConsumption
import com.meistercharts.time.TimeViewport
import it.neckar.events.MouseDownEvent
import it.neckar.events.MouseDragEvent
import it.neckar.events.MouseMoveEvent
import it.neckar.events.MouseUpEvent
import it.neckar.events.MouseWheelEvent
import it.neckar.open.unit.other.px
import kotlin.math.pow

/**
 * Zooms and pans [viewport], which covers the canvas and shows time right of [visibleStartX]. The mouse wheel
 * - zooms the time around the pointer,
 * - with Alt calls [onAltWheel] with the zoom factor of the notches,
 * - with Shift pans the time,
 * - with Ctrl stays with the browser, because a pinch on a trackpad arrives as Ctrl + wheel.
 *
 * Dragging pans the time.
 */
class TimeViewportInteractionLayer(
  private val viewport: TimeViewport,
  private val visibleStartX: @px Double = 0.0,
  private val onAltWheel: (factor: Double) -> Unit = {},
) : AbstractLayer() {
  override val type: LayerType = LayerType.Content

  /** The x of the last drag event, `null` while no button is down. */
  private var lastDragX: Double? = null

  // In layout, so that every layer computes its painting variables with the width of this frame.
  override fun layout(paintingContext: LayerPaintingContext) {
    super.layout(paintingContext)
    viewport.updateWindowWidth(paintingContext.gc.width, visibleStartX)
  }

  override fun paint(paintingContext: LayerPaintingContext) {
  }

  override val mouseEventHandler: CanvasMouseEventHandler = object : CanvasMouseEventHandler {
    override fun onWheel(event: MouseWheelEvent, chartSupport: ChartSupport): EventConsumption {
      val modifiers = event.modifierCombination
      val notches = event.delta / PixelsPerWheelNotch
      when {
        modifiers.control -> return EventConsumption.Ignored
        modifiers.alt -> onAltWheel(ZoomFactorPerWheelNotch.pow(-notches))
        modifiers.shift -> viewport.panBy(-event.delta)
        else -> viewport.zoomAround(event.coordinates.x, ZoomFactorPerWheelNotch.pow(notches))
      }
      chartSupport.markAsDirty(DirtyReason.UserInteraction)
      return EventConsumption.Consumed
    }

    override fun onDown(event: MouseDownEvent, chartSupport: ChartSupport): EventConsumption {
      lastDragX = event.coordinates.x
      return EventConsumption.Ignored
    }

    override fun onDrag(event: MouseDragEvent, chartSupport: ChartSupport): EventConsumption {
      // A button pressed outside the canvas drags into it without a down event here.
      val previousX = lastDragX ?: return EventConsumption.Ignored
      viewport.panBy(event.coordinates.x - previousX)
      lastDragX = event.coordinates.x
      chartSupport.markAsDirty(DirtyReason.UserInteraction)
      return EventConsumption.Consumed
    }

    // A button released outside the canvas sends no up event here; the next move without a button ends the drag.
    override fun onMove(event: MouseMoveEvent, chartSupport: ChartSupport): EventConsumption {
      lastDragX = null
      return EventConsumption.Ignored
    }

    override fun onUp(event: MouseUpEvent, chartSupport: ChartSupport): EventConsumption {
      lastDragX = null
      return EventConsumption.Ignored
    }
  }

  private companion object {
    const val ZoomFactorPerWheelNotch: Double = 1.25

    /** The browser reports one notch of a mouse wheel as 100 px. */
    const val PixelsPerWheelNotch: Double = 100.0
  }
}
