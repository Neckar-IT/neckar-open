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

import assertk.*
import assertk.assertions.*
import javafx.beans.property.SimpleIntegerProperty
import org.junit.jupiter.api.Test

internal class BindingsHelperTest {
  @Test
  fun testCreateLongBindingPacksThreeBytes() {
    val binding = BindingsHelper.createLongBinding(SimpleIntegerProperty(0x12), SimpleIntegerProperty(0x34), SimpleIntegerProperty(0x56))
    assertThat(binding.get()).isEqualTo(0x123456L)
  }

  @Test
  fun testCreateLongBindingKeepsOnlyTheLowByteOfEachInput() {
    val binding = BindingsHelper.createLongBinding(SimpleIntegerProperty(0x123456), SimpleIntegerProperty(0x1234), SimpleIntegerProperty(0x12))
    assertThat(binding.get()).isEqualTo(0x563412L)
  }

  @Test
  fun testCreateLongBindingIsInvalidatedByItsDependencies() {
    val property0 = SimpleIntegerProperty(0)
    val property1 = SimpleIntegerProperty(0)
    val property2 = SimpleIntegerProperty(0)

    val binding = BindingsHelper.createLongBinding(property0, property1, property2)
    assertThat(binding.get()).isEqualTo(0L)

    property0.set(0xff)
    property1.set(0xff)
    property2.set(0xff)
    assertThat(binding.get()).isEqualTo(0xffffffL)
  }
}
