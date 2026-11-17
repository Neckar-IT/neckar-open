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
package it.neckar.adjustable

import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.serializers.findSerialName
import it.neckar.open.serialization.roundTrip
import it.neckar.open.test.utils.dumpAsJson
import it.neckar.open.test.utils.isJsonEqualTo
import it.neckar.open.unit.si.kWh
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test

/**
 *
 */
class AdjustableValueTest {
  @Test
  fun testJsonAuto() {
    roundTrip(
      AdjustableValue.auto(42.0),
    ) {
      //language=Json
      """
        {
          "type" : "Auto",
          "value" : 42.0
        }
      """.trimIndent()
    }
  }

  @Test
  fun testJsonAdjusted() {
    roundTrip(
      AdjustableValue.adjusted(42.0, 43.9),
    ) {
      //language=Json
      """
        {
          "type" : "Adjusted",
          "calculated" : 42.0,
          "value" : 43.9
        }
      """.trimIndent()
    }
  }

  @Test
  fun testJsonAdjustedValueClass() {
    roundTrip(
      AdjustableValue.adjusted(KilowattHours(42.0), KilowattHours(43.9)),
    ) {
      //language=Json
      """
        {
          "type" : "Adjusted",
          "calculated" : 42.0,
          "value" : 43.9
        }
      """.trimIndent()
    }
  }

  @Test
  fun testJsonManual() {
    roundTrip(
      AdjustableValue.manual(42.0),
    ) {
      //language=Json
      """
        {
          "type" : "Manual",
          "value" : 42.0
        }
      """.trimIndent()
    }
  }

  @Test
  fun testMapKeepsVariant() {
    assertThat(AdjustableValue.auto(42.0).map { it.toInt() }).isEqualTo(AdjustableValue.auto(42))
    assertThat(AdjustableValue.adjusted(42.0, 40.0).map { it.toInt() }).isEqualTo(AdjustableValue.adjusted(42, 40))
    assertThat(AdjustableValue.manual(40.0).map { it.toInt() }).isEqualTo(AdjustableValue.manual(40))
  }

  @Test
  fun testWithCalculatedOnAuto() {
    assertThat(AdjustableValue.auto(42.0).withCalculated(45.0))
      .isEqualTo(AdjustableValue.auto(45.0))
  }

  @Test
  fun testWithCalculatedKeepsAdjustment() {
    assertThat(AdjustableValue.adjusted(42.0, 40.0).withCalculated(45.0))
      .isEqualTo(AdjustableValue.adjusted(45.0, 40.0))
  }

  @Test
  fun testWithCalculatedPromotesManual() {
    //A manual value becomes adjustable as soon as a calculation exists for it
    assertThat(AdjustableValue.manual(40.0).withCalculated(45.0))
      .isEqualTo(AdjustableValue.adjusted(45.0, 40.0))
  }

  @Test
  fun testRecalculatedStartsAtAuto() {
    val nothingStored: AdjustableValue<Double>? = null
    assertThat(nothingStored.recalculated(45.0)).isEqualTo(AdjustableValue.auto(45.0))
  }

  @Test
  fun testRecalculatedDelegatesWithResult() {
    val adjusted: AdjustableValue<Double>? = AdjustableValue.adjusted(42.0, 40.0)
    assertThat(adjusted.recalculated(45.0)).isEqualTo(AdjustableValue.adjusted(45.0, 40.0))
  }

  @Test
  fun testRecalculatedWithoutResultDemotesAdjustedToManual() {
    //The override survives, but there is no longer a calculation it could be an adjustment of
    val adjusted: AdjustableValue<Double>? = AdjustableValue.adjusted(42.0, 40.0)
    assertThat(adjusted.recalculated(null)).isEqualTo(AdjustableValue.manual(40.0))
  }

  @Test
  fun testRecalculatedWithoutResultKeepsManual() {
    val manual: AdjustableValue<Double>? = AdjustableValue.manual(40.0)
    assertThat(manual.recalculated(null)).isEqualTo(AdjustableValue.manual(40.0))
  }

  @Test
  fun testRecalculatedWithoutResultDropsAuto() {
    //Auto carried nothing but the calculation that just disappeared
    val auto: AdjustableValue<Double>? = AdjustableValue.auto(42.0)
    assertThat(auto.recalculated(null)).isNull()
  }

  @Test
  fun testRecalculatedNothingStoredNoResult() {
    val nothingStored: AdjustableValue<Double>? = null
    assertThat(nothingStored.recalculated(null)).isNull()
  }

  @Test
  fun testFindCalculated() {
    assertThat(AdjustableValue.auto(42.0).findCalculated()).isEqualTo(42.0)
    assertThat(AdjustableValue.adjusted(42.0, 40.0).findCalculated()).isEqualTo(42.0)
    assertThat(AdjustableValue.manual(40.0).findCalculated()).isNull()
  }

  @Test
  fun testResetToCalculated() {
    assertThat(AdjustableValue.adjusted(42.0, 40.0).resetToCalculated())
      .isEqualTo(AdjustableValue.auto(42.0))

    //Nothing to reset to: both are already the only value there is
    assertThat(AdjustableValue.auto(42.0).resetToCalculated())
      .isEqualTo(AdjustableValue.auto(42.0))
    assertThat(AdjustableValue.manual(40.0).resetToCalculated())
      .isEqualTo(AdjustableValue.manual(40.0))
  }

  @OptIn(InternalSerializationApi::class)
  @Test
  fun testSerializer() {
    assertThat(AdjustableValue.Auto::class.findSerialName()).isEqualTo("Auto")
    assertThat(AdjustableValue.Adjusted::class.findSerialName()).isEqualTo("Adjusted")
    assertThat(AdjustableValue.Manual::class.findSerialName()).isEqualTo("Manual")

    assertThat(AdjustableValue.serializer(Double.serializer())).isNotNull()

    assertFailure {
      //This method returns null, since there is no "generic" serializer (without T)
      AdjustableValue::class.serializer()
    }.messageContains("Serializer for class 'AdjustableValue' is not found.")
  }

  @OptIn(InternalSerializationApi::class)
  @Test
  fun testSerializationTypes() {
    val serializer = WithAdjustableValue::class.serializer()
    val descriptor = serializer.descriptor

    assertThat(descriptor::class.qualifiedName).isEqualTo("kotlinx.serialization.internal.PluginGeneratedSerialDescriptor")
    assertThat(descriptor.elementsCount).isEqualTo(1)
    assertThat(descriptor.getElementName(0)).isEqualTo("adjustableValue")

    assertThat(dumpAsJson(descriptor)).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "serialName" : "it.neckar.adjustable.AdjustableValueTest.WithAdjustableValue",
          "elementsCount" : 1,
          "annotations" : [ ],
          "kind" : { },
          "isInline" : false,
          "serialNames" : [ "adjustableValue" ],
          "isNullable" : false,
          "typeParameterDescriptors$org_jetbrains_kotlinx_kotlinx_serialization_core" : [ ]
        }
      """.trimIndent()
    )

    val adjustableValueDescriptor = descriptor.getElementDescriptor(0)
    assertThat(adjustableValueDescriptor.serialName).isEqualTo("it.neckar.adjustable.AdjustableValue")
    assertThat(adjustableValueDescriptor.elementsCount).isEqualTo(2)

    assertThat(adjustableValueDescriptor.getElementName(0)).isEqualTo("type")
    assertThat(adjustableValueDescriptor.getElementDescriptor(0).toString()).isEqualTo("PrimitiveDescriptor(kotlin.String)")

    assertThat(adjustableValueDescriptor.getElementName(1)).isEqualTo("value")
    assertThat(adjustableValueDescriptor.getElementDescriptor(1).toString()).isEqualTo("kotlinx.serialization.Sealed<AdjustableValue>(Adjusted: Adjusted, Auto: Auto, Manual: Manual)")
  }

  @Serializable
  @JvmInline
  private value class KilowattHours(val value: @kWh Double)

  @Serializable
  private data class WithAdjustableValue(val adjustableValue: AdjustableValue<String>)
}
