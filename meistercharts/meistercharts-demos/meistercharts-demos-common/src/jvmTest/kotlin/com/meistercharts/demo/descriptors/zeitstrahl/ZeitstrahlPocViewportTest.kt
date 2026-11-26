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

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ZeitstrahlPocViewportTest {
  private val canvasWidth: Double = 1500.0 + ZeitstrahlPocLayout.TreeWidth

  @Test
  fun `x counts from the left of the lanes region`() {
    val viewport = ZeitstrahlPocViewport(presentSeconds = 1.8e9)
    viewport.timeViewport.updateWindowWidth(canvasWidth, ZeitstrahlPocLayout.TreeWidth)
    viewport.showWindow(0.0, 1500.0)

    assertThat(viewport.windowWidth).isEqualTo(1500.0)
    assertThat(viewport.xOf(0.0)).isCloseTo(0.0, 1e-9)
    assertThat(viewport.xOf(1500.0)).isCloseTo(1500.0, 1e-9)
    assertThat(viewport.secondsAt(750.0)).isCloseTo(750.0, 1e-9)
    assertThat(viewport.centerSeconds).isCloseTo(750.0, 1e-9)
    assertThat(viewport.timeViewport.time2windowX(0.0)).isCloseTo(ZeitstrahlPocLayout.TreeWidth, 1e-9)
  }

  @Test
  fun `the window before the first width applies once the width is known`() {
    val viewport = ZeitstrahlPocViewport(presentSeconds = 1.8e9)
    viewport.applyPendingWindow()
    assertThat(viewport.windowWidth).isEqualTo(0.0)

    viewport.timeViewport.updateWindowWidth(canvasWidth, ZeitstrahlPocLayout.TreeWidth)
    viewport.applyPendingWindow()

    assertThat(viewport.visibleFrom).isCloseTo(ZeitstrahlPocViewport.BigBangSeconds, 1e6)
    assertThat(viewport.visibleTo).isCloseTo(1.8e9, 1e6)
  }

  @Test
  fun `a window set before the first width replaces the initial window`() {
    val viewport = ZeitstrahlPocViewport(presentSeconds = 1.8e9)
    viewport.showWindow(1000.0, 2000.0)
    viewport.timeViewport.updateWindowWidth(canvasWidth, ZeitstrahlPocLayout.TreeWidth)
    viewport.applyPendingWindow()

    assertThat(viewport.visibleFrom).isCloseTo(1000.0, 1e-6)
    assertThat(viewport.visibleTo).isCloseTo(2000.0, 1e-6)
  }
}
