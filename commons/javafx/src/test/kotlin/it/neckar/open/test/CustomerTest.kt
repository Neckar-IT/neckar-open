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
@file:Suppress("RETURN_VALUE_NOT_USED")

package it.neckar.open.test

import assertk.*
import assertk.assertions.*
import javafx.beans.InvalidationListener
import javafx.collections.ListChangeListener
import javafx.collections.ObservableListBase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.Arrays

/**
 */
class CustomerTest {
  private lateinit var customer: Customer

  @BeforeEach
  fun setUp() {
    customer = Customer()
  }

  @Test
  fun testLazyObsList() {
    object : ObservableListBase<String>() {

      override val size: Int
        get() {
          return 10000
        }

      override fun get(index: Int): String {
        return index.toString() + ""
      }
    }
  }

  @Test
  fun testSimpleBinding() {
    val address = Customer.Address()
  }

  @Test
  fun testBasics() {
    assertThat(customer.name).isEqualTo("")
  }

  @Disabled
  @Test
  fun testListeners() {
    customer.nameProperty.addListener(InvalidationListener { println("Invalidated") })
    customer.nameProperty.addListener { observable, oldValue, newValue -> println("Changed from <$oldValue> to <$newValue>") }
    customer.name = "New Name"
  }

  @Disabled
  @Test
  fun testAddressValid() {
    val address = Customer.Address()
    assertThat(address.valid).isFalse()
    address.validProperty.addListener { observable, oldValue, newValue -> println("Changed from <$oldValue> to <$newValue>") }
    address.city = "daCity"
    address.street = "daStreet"
    address.number = 2
  }

  @Disabled
  @Test
  fun testList() {
    customer.addresses.addListener(ListChangeListener { c ->
      println("Change... $c")
      while (c.next()) {
        println("next <" + c.addedSubList + "> and removed <" + c.removed + ">")
      }
    })
    println("add")
    customer.addresses.add(Customer.Address())
    println("add")
    customer.addresses.add(Customer.Address())
    println("addAll")
    customer.addresses.addAll(Arrays.asList(Customer.Address(), Customer.Address(), Customer.Address()))
  }
}
