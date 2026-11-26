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
package com.meistercharts.demo.descriptors.zeitstrahl

import com.meistercharts.algorithms.layers.AbstractLayer
import com.meistercharts.algorithms.layers.LayerPaintingContext
import com.meistercharts.algorithms.layers.LayerType
import com.meistercharts.canvas.fill
import com.meistercharts.font.FontDescriptorFragment
import com.meistercharts.font.FontSize
import com.meistercharts.font.FontWeight
import it.neckar.geometry.Direction

/**
 * The tree column beside the time axis, painted above it: the label of the tick before the window start reaches into the column.
 */
class ZeitstrahlPocAxisColumnLayer : AbstractLayer() {
  override val type: LayerType = LayerType.Content

  override fun paint(paintingContext: LayerPaintingContext) {
    val gc = paintingContext.gc
    val axisTop = gc.height - ZeitstrahlPocLayout.StripHeight - ZeitstrahlPocLayout.TimeAxisHeight

    gc.fill(SignalColors.ground)
    gc.fillRect(0.0, axisTop, ZeitstrahlPocLayout.TreeWidth, ZeitstrahlPocLayout.TimeAxisHeight)

    gc.font(FontDescriptorFragment(size = FontSize(10.5), weight = FontWeight.Normal))
    gc.fill(SignalColors.muted)
    gc.fillText("STRUKTUR", 14.0, axisTop + 8.0, Direction.TopLeft)
  }
}
