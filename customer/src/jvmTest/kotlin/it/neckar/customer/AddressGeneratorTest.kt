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
package it.neckar.customer

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

class AddressGeneratorTest {

  @Test
  fun `same seed produces identical addresses`() {
    val generator1 = AddressGenerator(seed = 42)
    val generator2 = AddressGenerator(seed = 42)

    val addresses1 = generator1.generate(count = 100)
    val addresses2 = generator2.generate(count = 100)

    assertThat(addresses1).isEqualTo(addresses2)
  }

  @Test
  fun `different seeds produce different addresses`() {
    val generator1 = AddressGenerator(seed = 42)
    val generator2 = AddressGenerator(seed = 99)

    val address1 = generator1[0]
    val address2 = generator2[0]

    assertThat(address1).isNotEqualTo(address2)
  }

  @Test
  fun `single index is stable across calls`() {
    val generator = AddressGenerator(seed = 42)

    val first = generator[500]
    val second = generator[500]

    assertThat(first).isEqualTo(second)
  }

  @Test
  fun `single index is independent of prior generation`() {
    val generator = AddressGenerator(seed = 42)

    // Generate index 500 directly
    val direct = generator[500]

    // Generate 1000 addresses, then check index 500
    val bulk = generator.generate(count = 1000)

    assertThat(direct).isEqualTo(bulk[500])
  }

  @Test
  fun `generates large variety of addresses`() {
    val generator = AddressGenerator()
    val addresses = generator.generate(count = 10_000)

    val uniqueStreets = addresses.map { it.street }.toSet()
    val uniqueCities = addresses.map { it.city }.toSet()
    val uniqueZipCodes = addresses.map { it.zipCode }.toSet()

    // With combinatorial generation we expect far more than 32 unique combinations
    assertThat(uniqueStreets.size).isGreaterThan(50)
    assertThat(uniqueCities.size).isGreaterThan(30)
    assertThat(uniqueZipCodes.size).isGreaterThan(50)

    // Full address combinations should be highly varied
    val uniqueAddresses = addresses.map { "${it.street} ${it.houseNumber}, ${it.zipCode} ${it.city}" }.toSet()
    assertThat(uniqueAddresses.size).isGreaterThan(5_000)
  }

  @Test
  fun `all addresses are confirmed`() {
    val generator = AddressGenerator()
    val addresses = generator.generate(count = 100)

    for (address in addresses) {
      assertThat(address.street).isNotEmpty()
      assertThat(address.houseNumber).isNotEmpty()
      assertThat(address.zipCode).isNotEmpty()
      assertThat(address.city).isNotEmpty()
      assertThat(address.country).isNotEmpty()
    }
  }

  @Test
  fun `companion extension works`() {
    val generator = Address.Generator
    val address = generator[0]

    assertThat(address.street).isNotEmpty()
    assertThat(address.city).isNotEmpty()
  }

  @Test
  fun `scales to 100_000 addresses`() {
    val generator = AddressGenerator()
    val addresses = generator.generate(count = 100_000)

    assertThat(addresses).hasSize(100_000)
  }
}
