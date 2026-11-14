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
package it.neckar.open.test

import it.neckar.open.javafx.properties.*
import javafx.beans.binding.Bindings
import javafx.beans.binding.StringBinding
import javafx.beans.binding.When
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.util.Collections

/**
 */
class Customer {
  val nameProperty: StringProperty = SimpleStringProperty(this, "name", "")
  var name: String by nameProperty

  val addresses: ObservableList<Address> = FXCollections.observableArrayList<Address>()

  val addressesEmpty: ObservableBooleanValue = Bindings.isEmpty(addresses)
  val stringEmpty: StringBinding = When(addressesEmpty).then("No addresses").otherwise("dada")

  fun getAddresses(): List<Address> {
    return Collections.unmodifiableList(addresses)
  }

  class Address {
    val streetProperty: StringProperty = SimpleStringProperty(this, "street", "")
    var street: String by streetProperty

    val numberProperty: IntegerProperty = SimpleIntegerProperty(this, "number", 1)
    var number: Int by numberProperty

    val cityProperty: StringProperty = SimpleStringProperty(this, "city", "")
    var city: String by cityProperty

    val validProperty: ReadOnlyBooleanProperty = SimpleBooleanProperty().also {
      it.bind(
        numberProperty
          .greaterThan(0)
          .and(streetProperty.isNotEmpty)
          .and(cityProperty.isNotEmpty)
      )
    }
    val valid: Boolean by validProperty

    //init {
    //InvalidationListener validationUpdateListener = new InvalidationListener() {
    //  @Override
    //  public void invalidated(Observable observable) {
    //    updateValidation();
    //  }
    //
    //  private void updateValidation() {
    //    valid.set(!street.getValue().isEmpty() && !city.getValue().isEmpty() && number.get() > 0);
    //  }
    //};
    //street.addListener(validationUpdateListener);
    //number.addListener(validationUpdateListener);
    //city.addListener(validationUpdateListener);
    //}
  }
}
