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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class CustomerGeneratorTest {

  @Test
  fun `same seed produces identical customers`() {
    val generator1 = CustomerGenerator(seed = 42)
    val generator2 = CustomerGenerator(seed = 42)

    val customers1 = generator1.generate(count = 100)
    val customers2 = generator2.generate(count = 100)

    assertThat(customers1).isEqualTo(customers2)
  }

  @Test
  fun `different seeds produce different customers`() {
    val generator1 = CustomerGenerator(seed = 42)
    val generator2 = CustomerGenerator(seed = 99)

    val customer1 = generator1[0]
    val customer2 = generator2[0]

    assertThat(customer1).isNotEqualTo(customer2)
  }

  @Test
  fun `single index is stable across calls`() {
    val generator = CustomerGenerator(seed = 42)

    val first = generator[500]
    val second = generator[500]

    assertThat(first).isEqualTo(second)
  }

  @Test
  fun `single index is independent of prior generation`() {
    val generator = CustomerGenerator(seed = 42)

    // Generate index 500 directly
    val direct = generator[500]

    // Generate 1000 customers, then check index 500
    val bulk = generator.generate(count = 1000)

    assertThat(direct).isEqualTo(bulk[500])
  }

  @Test
  fun `no duplicate IDs in 10_000 customers`() {
    val generator = CustomerGenerator()
    val customers = generator.generate(count = 10_000)

    val uniqueIds = customers.map { it.id }.toSet()
    assertThat(uniqueIds).hasSize(10_000)
  }

  @Test
  fun `realistic field distribution`() {
    val generator = CustomerGenerator()
    val customers = generator.generate(count = 10_000)

    val withAddress = customers.count { it.address != null }
    val withPhone = customers.count { it.phone != null }
    val withCellphone = customers.count { it.cellphone != null }
    val withEmail = customers.count { it.email != null }
    val withCompany = customers.count { it.company != null }

    // Allow reasonable variance (+-5%) around expected distribution
    assertThat(withAddress).isBetween(8_000, 9_500)     // ~90%
    assertThat(withPhone).isBetween(5_000, 7_000)       // ~60%
    assertThat(withCellphone).isBetween(6_000, 8_000)   // ~70%
    assertThat(withEmail).isBetween(7_000, 9_000)       // ~80%
    assertThat(withCompany).isBetween(1_000, 2_500)     // ~15%
  }

  @Test
  fun `generates realistic data`() {
    val generator = CustomerGenerator()
    val customer = generator[0]

    assertThat(customer.firstName).isNotEmpty()
    assertThat(customer.lastName).isNotEmpty()
    assertThat(customer.id.uuid.toString()).isNotEmpty()
  }

  @Test
  fun `phone numbers have correct format`() {
    val generator = CustomerGenerator(seed = 1)
    val customers = generator.generate(count = 1_000)

    val customersWithPhone = customers.filter { it.phone != null }
    assertThat(customersWithPhone).isNotEmpty()

    for (customer in customersWithPhone) {
      assertThat(customer.phone?.value.orEmpty()).startsWith("+49 ")
    }

    val customersWithCellphone = customers.filter { it.cellphone != null }
    assertThat(customersWithCellphone).isNotEmpty()

    for (customer in customersWithCellphone) {
      assertThat(customer.cellphone?.value.orEmpty()).startsWith("+49 ")
    }
  }

  @Test
  fun `email addresses are plausible`() {
    val generator = CustomerGenerator(seed = 1)
    val customers = generator.generate(count = 1_000)

    val customersWithEmail = customers.filter { it.email != null }
    assertThat(customersWithEmail).isNotEmpty()

    for (customer in customersWithEmail) {
      val email = customer.email?.value.orEmpty()
      assertThat(email).contains("@")
      assertThat(email).contains(".")
    }
  }

  @Test
  fun `companion extension works`() {
    val generator = Customer.Generator
    val customer = generator[0]

    assertThat(customer.firstName).isNotEmpty()
    assertThat(customer.lastName).isNotEmpty()
  }

  @Disabled("This test is more of a benchmark - it can be enabled for performance testing, but is not meant to be run on every build")
  @Test
  fun `scales to 100_000 customers`() {
    val generator = CustomerGenerator()

    val elapsed = kotlin.time.measureTime {
      val customers = generator.generate(count = 100_000)

      assertThat(customers).hasSize(100_000)

      val uniqueIds = customers.map { it.id }.toSet()
      assertThat(uniqueIds).hasSize(100_000)

      customers.forEach { customer ->
        println("Customer: ${customer.firstName} ${customer.lastName}, ID: ${customer.address}")
      }
    }

    println("Generating 100,000 customers took $elapsed")
  }

  @Test
  fun `large variety of names`() {
    val generator = CustomerGenerator()
    val customers = generator.generate(count = 10_000)

    val uniqueFirstNames = customers.map { it.firstName }.toSet()
    val uniqueLastNames = customers.map { it.lastName }.toSet()

    assertThat(uniqueFirstNames.size).isGreaterThan(80)
    assertThat(uniqueLastNames.size).isGreaterThan(80)
  }
}
