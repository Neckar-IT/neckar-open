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

import com.meistercharts.time.TimeViewport

/**
 * Where a [TimeAxisLayer] takes its [TimeViewport] from
 */
sealed interface TimeAxisViewportSource {
  /**
   * Follows the content area time range and the zoom and translation of the chart state.
   */
  data object ContentAreaTimeRange : TimeAxisViewportSource

  /**
   * A viewport that its owner zooms and pans, for example through `TimeViewportInteractionLayer`.
   */
  data class Explicit(val viewport: TimeViewport) : TimeAxisViewportSource
}
