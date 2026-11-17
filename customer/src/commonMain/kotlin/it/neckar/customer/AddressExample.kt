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
@file:Suppress("SpellCheckingInspection")

package it.neckar.customer


/**
 * Examples for [Address]
 */
object AddressExample {
  val holstenwall: Address.Confirmed = Address("Holstenwall", "85", "06548", "Rottleberode", "Germany")
  val albrechtstrasse: Address.Confirmed = Address("Albrechtstrasse", "30", "86305", "Friedberg", "Germany")
  val gotthardstrasse: Address.Confirmed = Address("Gotthardstrasse", "42a", "09112", "Chemnitz", "Germany")
  val lindenstrasse: Address.Confirmed = Address("Lindenstraße", "12", "10115", "Berlin", "Germany")
  val bahnhofstrasse: Address.Confirmed = Address("Bahnhofstraße", "5", "80331", "München", "Germany")
  val marktstrasse: Address.Confirmed = Address("Marktstraße", "3", "50667", "Köln", "Germany")
  val hauptstrasse: Address.Confirmed = Address("Hauptstraße", "77", "20095", "Hamburg", "Germany")
  val gartenweg: Address.Confirmed = Address("Gartenweg", "9", "04109", "Leipzig", "Germany")
  val sonnenallee: Address.Confirmed = Address("Sonnenallee", "120", "12045", "Berlin", "Germany")
  val schlossweg: Address.Confirmed = Address("Schlossweg", "8", "68159", "Mannheim", "Germany")
  val ringstrasse: Address.Confirmed = Address("Ringstraße", "21", "90402", "Nürnberg", "Germany")
  val buchenweg: Address.Confirmed = Address("Buchenweg", "14", "70563", "Stuttgart", "Germany")
  val ahornstrasse: Address.Confirmed = Address("Ahornstraße", "6", "89073", "Ulm", "Germany")
  val kirchgasse: Address.Confirmed = Address("Kirchgasse", "2", "65183", "Wiesbaden", "Germany")
  val birkenweg: Address.Confirmed = Address("Birkenweg", "17", "14467", "Potsdam", "Germany")
  val dammstrasse: Address.Confirmed = Address("Dammstraße", "40", "39104", "Magdeburg", "Germany")
  val amSportplatz: Address.Confirmed = Address("Am Sportplatz", "1", "04105", "Leipzig", "Germany")
  val beethovenstrasse: Address.Confirmed = Address("Beethovenstraße", "11", "01069", "Dresden", "Germany")
  val koelnerStrasse: Address.Confirmed = Address("Kölner Straße", "55", "40211", "Düsseldorf", "Germany")
  val brunnenweg: Address.Confirmed = Address("Brunnenweg", "18", "67655", "Kaiserslautern", "Germany")
  val hafenstrasse: Address.Confirmed = Address("Hafenstraße", "31", "27568", "Bremerhaven", "Germany")
  val fliederweg: Address.Confirmed = Address("Fliederweg", "24", "85049", "Ingolstadt", "Germany")

  val avenueDesChamps: Address.Confirmed = Address("Avenue des Champs-Élysées", "50", "75008", "Paris", "France")
  val oxfordStreet: Address.Confirmed = Address("Oxford Street", "120", "W1D 1LT", "London", "United Kingdom")
  val granVia: Address.Confirmed = Address("Gran Via", "45", "28013", "Madrid", "Spain")
  val corsoVenezia: Address.Confirmed = Address("Corso Venezia", "15", "20121", "Milan", "Italy")
  val karlJohansGate: Address.Confirmed = Address("Karl Johans gate", "22", "0159", "Oslo", "Norway")
  val damrak: Address.Confirmed = Address("Damrak", "1", "1012LG", "Amsterdam", "Netherlands")
  val kaiserstrasse: Address.Confirmed = Address("Kaiserstraße", "10", "1010", "Vienna", "Austria")
  val fifthAvenue: Address.Confirmed = Address("Fifth Avenue", "350", "10001", "New York", "USA")
  val collinsStreet: Address.Confirmed = Address("Collins Street", "455", "3000", "Melbourne", "Australia")
  val robsonStreet: Address.Confirmed = Address("Robson Street", "789", "V6E 1B5", "Vancouver", "Canada")

  val Addresses: List<Address> =
    listOf(
      holstenwall,
      albrechtstrasse,
      gotthardstrasse,
      lindenstrasse,
      bahnhofstrasse,
      marktstrasse,
      hauptstrasse,
      gartenweg,
      sonnenallee,
      schlossweg,
      ringstrasse,
      buchenweg,
      ahornstrasse,
      kirchgasse,
      birkenweg,
      dammstrasse,
      amSportplatz,
      beethovenstrasse,
      koelnerStrasse,
      brunnenweg,
      hafenstrasse,
      fliederweg,
      avenueDesChamps,
      oxfordStreet,
      granVia,
      corsoVenezia,
      karlJohansGate,
      damrak,
      kaiserstrasse,
      fifthAvenue,
      collinsStreet,
      robsonStreet
    )

  val AddressesGermany: List<Address> = listOf(
    holstenwall,
    albrechtstrasse,
    gotthardstrasse,
    lindenstrasse,
    bahnhofstrasse,
    marktstrasse,
    hauptstrasse,
    gartenweg,
    sonnenallee,
    schlossweg,
    ringstrasse,
    buchenweg,
    ahornstrasse,
    kirchgasse,
    birkenweg,
    dammstrasse,
    amSportplatz,
    beethovenstrasse,
    koelnerStrasse,
    brunnenweg,
    hafenstrasse,
    fliederweg
  )

  val muster: Address.Confirmed = Address("Musterstraße", "99", "77777", "Musterstadt", "Germany")

  val test: Address.Confirmed = Address("Teststraße", "99", "77777", "Test-Stadt", "Germany")
}

val Address.Companion.Example: AddressExample
  get() = AddressExample
