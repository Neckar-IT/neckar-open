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
package it.neckar.floorplan

import it.neckar.geometry.Coordinates
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

/**
 * The expected values are those of `floor_frame` and `coordinates_json` in
 * `internal/open/commons/floorplan/tools/floorplan_pipeline.py` for the same input.
 */
class FloorFrameTest {
  @Test
  fun theFrameAroundTheBoundsOfAFloorMatchesThePythonFrame() {
    val bounds = ModelBounds(-99.60000000000025, 0.3999999999996362, -89.89999999999998, 8.600000000000477)
    val frame = FloorFrame.around(bounds, marginMeters = 1.0, pixelsPerMeter = 200.0)

    assertThat(frame.windowMeters).isEqualTo(ModelBounds(-100.60000000000025, -0.6000000000003638, -88.89999999999998, 9.600000000000477))
    assertThat(frame.pixelsPerMeter).isEqualTo(200.0)
    assertThat(frame.widthPx to frame.heightPx).isEqualTo(2340 to 2040)
    assertCloseTo(Coordinates.of(200.0, 200.0), frame.toPixel(-99.60000000000025, 8.60000000000025))
    assertCloseTo(Coordinates.of(200.0, 1540.0), frame.toPixel(-99.59999999999991, 1.9000000000002037))
  }

  @Test
  fun aWindowLongerThanTheLimitLowersThePixelsPerMeter() {
    val frame = FloorFrame.around(ModelBounds(0.0, 0.0, 100.0, 40.0), marginMeters = 1.0, pixelsPerMeter = 200.0)
    assertThat(frame.pixelsPerMeter).isEqualTo(6000.0 / 102.0)
    assertThat(frame.widthPx to frame.heightPx).isEqualTo(FloorFrame.MaxLongSidePx to 2471)
  }

  @Test
  fun aHalfPixelRoundsToEven() {
    val frame = FloorFrame.around(ModelBounds(0.0, 0.0, 10.0025, 5.0), marginMeters = 0.0, pixelsPerMeter = 200.0)
    assertThat(frame.widthPx to frame.heightPx).isEqualTo(2000 to 1000)
  }

  private fun assertCloseTo(expected: Coordinates, actual: Coordinates) {
    assertThat(actual.x).isCloseTo(expected.x, 0.01)
    assertThat(actual.y).isCloseTo(expected.y, 0.01)
  }
}
