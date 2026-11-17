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
package it.neckar.react.common.loading

import it.neckar.commons.kotlin.js.safeGet
import it.neckar.open.unit.other.pct
import it.neckar.react.common.*
import kotlinx.css.*
import kotlinx.html.classes
import react.*
import react.dom.*
import styled.*
import kotlin.math.roundToInt


fun RBuilder.bootstrapProgressBar(
  title: String,
  progressType: ProgressType,
  currentPercentage: @pct Double,
  style: ProgressBarStyle = ProgressBarStyle.Default,
): Unit = child(bootstrapProgressBar) {
  attrs {
    this.title = title
    this.progressType = progressType
    this.currentPercentage = currentPercentage
    this.style = style
  }
}

val bootstrapProgressBar: FC<ProgressProps> = fc("bootstrapToast") { props ->
  @pct val currentPercentage = props::currentPercentage.safeGet()
  val percent100 = (currentPercentage * 100.0).roundToInt()

  val title = props::title.safeGet()
  val progressType = props::progressType.safeGet()
  val style = props::style.safeGet()

  styledDiv {
    attrs {
      classes = setOf("progress")
    }
    css {
      if (style == ProgressBarStyle.Small) height = 5.px
    }
    styledDiv {
      attrs {
        classes = setOf("progress-bar")
        when (progressType) {
          ProgressType.Info -> addClass("bg-info")
          ProgressType.Warning -> addClass("bg-warning")
          ProgressType.Danger -> addClass("bg-danger")
          ProgressType.Success -> addClass("bg-success")
        }
        roleHTML = "progressbar"
        ariaValueRange = IntRange(0, 100)
        ariaValueNow = percent100
        ariaLabel = title
      }
      css {
        width = percent100.pct
        if (style == ProgressBarStyle.Small) height = 5.px
      }
    }
  }
}

enum class ProgressType {
  Info,
  Warning,
  Danger,
  Success,
}

enum class ProgressBarStyle {
  /**
   * Default size
   */
  Default,

  /**
   * Small size - e.g. for the header
   */
  Small,
}


external interface ProgressProps : Props {
  var title: String
  var progressType: ProgressType
  var currentPercentage: @pct Double
  var style: ProgressBarStyle
}
