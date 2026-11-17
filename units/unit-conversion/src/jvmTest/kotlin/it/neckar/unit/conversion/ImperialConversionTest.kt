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
package it.neckar.unit.conversion

import assertk.*
import assertk.assertions.*
import it.neckar.open.unit.si.degC
import it.neckar.open.unit.si.degF
import org.junit.jupiter.api.Test

class ImperialConversionTest {
  @Test
  fun testTempConversion() {
    testTemperatureRoundtrip(5600.0, 10112.0)
    testTemperatureRoundtrip(100.0, 212.0)
    testTemperatureRoundtrip(30.0, 86.0)
    testTemperatureRoundtrip(0.0, 32.0)
    //below the freezing point - and the fixed point where both scales meet
    testTemperatureRoundtrip(-17.5, 0.5)
    testTemperatureRoundtrip(-40.0, -40.0)

    assertRoundtrips(ImperialConversion.celsius2FahrenheitConverter, "°C <-> °F")
  }

  private fun testTemperatureRoundtrip(celsius: @degC Double, fahrenheit: @degF Double) {
    assertThat(ImperialConversion.celsius2fahrenheit(celsius), "converting $celsius °C").isEqualTo(fahrenheit)
    assertThat(ImperialConversion.fahrenheit2celsius(fahrenheit), "converting $fahrenheit °F").isEqualTo(celsius)
  }

  @Test
  fun testUsGAl() {
    assertThat(ImperialConversion.litre2usGallon(1.0)).isCloseTo(0.2641720524, Tolerance)
    assertThat(ImperialConversion.litre2usGallon(0.0)).isEqualTo(0.0)
    assertThat(ImperialConversion.litre2usGallon(-1.0)).isCloseTo(-0.2641720524, Tolerance)

    assertThat(ImperialConversion.usGallon2litre(1.0)).isCloseTo(3.785411784, Tolerance)
    assertThat(ImperialConversion.usGallon2litre(17.0)).isCloseTo(64.352000328, Tolerance)
    assertThat(ImperialConversion.usGallon2litre(0.0)).isEqualTo(0.0)
    assertThat(ImperialConversion.usGallon2litre(-17.0)).isCloseTo(-64.352000328, Tolerance)

    assertRoundtrips(ImperialConversion.litre2UsGallonConverter, "l <-> US gal")
  }

  @Test
  fun testUsGAlFtsBug() {
    assertThat(ImperialConversion.litre2usGallon(9.7)).isCloseTo(2.5624689079, Tolerance)
    assertThat(ImperialConversion.usGallon2litre(2.5624689079)).isCloseTo(9.7, Tolerance)
  }

  @Test
  fun testCubicFeet() {
    assertThat(ImperialConversion.litre2cubicFoot(1.0)).isCloseTo(0.0353147248, Tolerance)
    assertThat(ImperialConversion.litre2cubicFoot(0.0)).isEqualTo(0.0)
    assertThat(ImperialConversion.litre2cubicFoot(-1.0)).isCloseTo(-0.0353147248, Tolerance)

    //LitrePerCuFoot is the rounded 28.3168 - not the exact 28.316846592
    assertThat(ImperialConversion.cubicFoot2litre(1.0)).isCloseTo(28.3168, Tolerance)
    assertThat(ImperialConversion.cubicFoot2litre(0.0)).isEqualTo(0.0)
    assertThat(ImperialConversion.cubicFoot2litre(-1.0)).isCloseTo(-28.3168, Tolerance)

    assertRoundtrips(ImperialConversion.litre2cubicFootConverter, "l <-> cu ft")
  }

  @Test
  fun testMeterFoot() {
    assertThat(ImperialConversion.meter2foot(1.0)).isCloseTo(3.2808398950, Tolerance)
    assertThat(ImperialConversion.meter2foot(17.0)).isCloseTo(55.7742782152, Tolerance)
    assertThat(ImperialConversion.meter2foot(0.0)).isEqualTo(0.0)
    assertThat(ImperialConversion.meter2foot(-17.0)).isCloseTo(-55.7742782152, Tolerance)

    assertThat(ImperialConversion.foot2meter(1.0)).isCloseTo(0.3048, Tolerance)
    assertThat(ImperialConversion.foot2meter(0.0)).isEqualTo(0.0)
    assertThat(ImperialConversion.foot2meter(-1.0)).isCloseTo(-0.3048, Tolerance)

    assertRoundtrips(ImperialConversion.meter2footConverter, "m <-> ft")
  }

  @Test
  fun testInches() {
    assertThat(ImperialConversion.inch2mm(1.0)).isCloseTo(25.4, Tolerance)
    assertThat(ImperialConversion.inch2mm(17.0)).isCloseTo(431.8, Tolerance)
    assertThat(ImperialConversion.inch2mm(0.0)).isEqualTo(0.0)
    assertThat(ImperialConversion.inch2mm(-17.0)).isCloseTo(-431.8, Tolerance)

    assertThat(ImperialConversion.mm2inch(1.0)).isCloseTo(0.0393700787, Tolerance)
    assertThat(ImperialConversion.mm2inch(11.0)).isCloseTo(0.4330708661, Tolerance)
    assertThat(ImperialConversion.mm2inch(0.0)).isEqualTo(0.0)
    assertThat(ImperialConversion.mm2inch(-11.0)).isCloseTo(-0.4330708661, Tolerance)

    assertRoundtrips(ImperialConversion.mm2inchConverter, "mm <-> in")
  }

  /**
   * Asserts that [Converter.reverseValue] undoes [Converter.convertValue] - in both directions and for
   * negative values, zero and positive values.
   */
  private fun assertRoundtrips(converter: Converter, description: String) {
    listOf(-1234.5, -9.7, -1.0, 0.0, 1.0, 9.7, 1234.5).forEach { value ->
      assertThat(converter.reverseValue(converter.convertValue(value)), "$description: convert/reverse of $value").isCloseTo(value, Tolerance)
      assertThat(converter.convertValue(converter.reverseValue(value)), "$description: reverse/convert of $value").isCloseTo(value, Tolerance)
    }
  }

  companion object {
    /**
     * The conversions are plain multiplications/divisions - only the rounding error of the double arithmetic is tolerated
     */
    private const val Tolerance: Double = 1E-9
  }
}
