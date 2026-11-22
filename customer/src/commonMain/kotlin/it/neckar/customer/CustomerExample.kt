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
@file:Suppress("NonAsciiCharacters", "FunctionName", "ObjectPropertyName")

package it.neckar.customer

import kotlin.uuid.Uuid


object CustomerExample {
  val PeterMüllerNoAddress: Customer = Customer(
    id = Customer.Id(Uuid.parse("ab0c262d-fa24-4c71-a2be-e0f8ce825b4b")),
    firstName = "Peter",
    lastName = "Müller",
  )

  val PeterMüller: Customer = Customer(
    id = Customer.Id(Uuid.parse("b2fb2c9f-26c2-475b-9211-a57a55dc081b")),
    firstName = "Peter",
    lastName = "Müller",
    address = AddressExample.holstenwall,
  )

  val LisaMüller: Customer = Customer(
    id = Customer.Id(Uuid.parse("e7889ec6-7bac-4de5-8054-8296000c0ea8")),
    firstName = "Lisa",
    lastName = "Müller",
    address = AddressExample.hauptstrasse,
  )

  val MarkusSchneider: Customer = Customer(
    id = Customer.Id(Uuid.parse("91f9464d-8409-4d01-9cf7-20b74a5bf6d6")),
    firstName = "Markus",
    lastName = "Schneider",
    address = AddressExample.albrechtstrasse,
  )

  val AnnaWeber: Customer = Customer(
    id = Customer.Id(Uuid.parse("16346618-a8ce-41e1-983b-3f1ec60de32d")),
    firstName = "Anna",
    lastName = "Weber",
    address = AddressExample.gotthardstrasse,
  )

  val JuliaSchmidt: Customer = Customer(
    id = Customer.Id(Uuid.parse("c2171b46-dbaa-4187-82d1-c9fb9a8c00de")),
    firstName = "Julia",
    lastName = "Schmidt",
    address = AddressExample.lindenstrasse,
  )

  val TomSchulz: Customer = Customer(
    id = Customer.Id(Uuid.parse("228e8be9-5a2d-4819-a764-440ad1b4c5c1")),
    firstName = "Tom",
    lastName = "Schulz",
    address = AddressExample.bahnhofstrasse,
  )

  val SarahNeumann: Customer = Customer(
    id = Customer.Id(Uuid.parse("3a8f9c8b-1285-4fdb-9230-b2032fa11da1")),
    firstName = "Sarah",
    lastName = "Neumann",
    address = AddressExample.marktstrasse,
  )

  val PaulBecker: Customer = Customer(
    id = Customer.Id(Uuid.parse("3996f680-049f-49f6-92b7-c9f03e8f8f98")),
    firstName = "Paul",
    lastName = "Becker",
    address = AddressExample.hauptstrasse,
  )

  val LeaSchuster: Customer = Customer(
    id = Customer.Id(Uuid.parse("15e1347e-4e13-47d4-a6c0-2e5d4f67f958")),
    firstName = "Lea",
    lastName = "Schuster",
    address = AddressExample.gartenweg,
  )

  val NoahFischer: Customer = Customer(
    id = Customer.Id(Uuid.parse("6c4a152c-f7ef-422e-8ecb-490c95d0a641")),
    firstName = "Noah",
    lastName = "Fischer",
    address = AddressExample.sonnenallee,
  )

  val MiaWolf: Customer = Customer(
    id = Customer.Id(Uuid.parse("0db0387f-a2cd-4a32-a58b-4f9fd23492d5")),
    firstName = "Mia",
    lastName = "Wolf",
    address = AddressExample.schlossweg,
  )

  val EliasHoffmann: Customer = Customer(
    id = Customer.Id(Uuid.parse("3e4e58d3-6051-4126-9c7f-23789db1b302")),
    firstName = "Elias",
    lastName = "Hoffmann",
    address = AddressExample.ringstrasse,
  )

  val EmilyLang: Customer = Customer(
    id = Customer.Id(Uuid.parse("1f21496b-41ae-426b-a170-dfbf38e801f9")),
    firstName = "Emily",
    lastName = "Lang",
    address = AddressExample.buchenweg,
  )

  val LeonKrüger: Customer = Customer(
    id = Customer.Id(Uuid.parse("1b91b110-ad78-4feb-9c1c-b9ad826a14e7")),
    firstName = "Leon",
    lastName = "Krüger",
    address = AddressExample.ahornstrasse,
  )

  val SophieVogel: Customer = Customer(
    id = Customer.Id(Uuid.parse("8e6895f5-f81f-4a1b-825c-f9d2efe3586c")),
    firstName = "Sophie",
    lastName = "Vogel",
    address = AddressExample.kirchgasse,
  )

  val LucaSchwarz: Customer = Customer(
    id = Customer.Id(Uuid.parse("cf6e5c4c-486a-41ab-95fc-53b46a5a2317")),
    firstName = "Luca",
    lastName = "Schwarz",
    address = AddressExample.brunnenweg,
  )

  val MaxMeyer: Customer = Customer(
    id = Customer.Id(Uuid.parse("dc6e3f12-0921-46d8-9eb8-7d330121a558")),
    firstName = "Max",
    lastName = "Meyer",
    address = AddressExample.dammstrasse,
  )

  val NatalieBauer: Customer = Customer(
    id = Customer.Id(Uuid.parse("e3f6c859-96ea-4c43-a2c3-2b686e16b7ed")),
    firstName = "Natalie",
    lastName = "Bauer",
    address = AddressExample.birkenweg,
  )

  val YannickGraf: Customer = Customer(
    id = Customer.Id(Uuid.parse("3bfe6c44-485e-47b3-a75e-db6797063082")),
    firstName = "Yannick",
    lastName = "Graf",
    address = AddressExample.fliederweg,
  )

  // Auslandskunden
  val ChloeDubois: Customer = Customer(
    id = Customer.Id(Uuid.parse("3bc1e020-c2e3-4e25-b7fb-abcf4acee791")),
    firstName = "Chloé",
    lastName = "Dubois",
    address = AddressExample.avenueDesChamps,
  )

  val JamesSmith: Customer = Customer(
    id = Customer.Id(Uuid.parse("7df88174-affa-4831-837b-ae8d356bea45")),
    firstName = "James",
    lastName = "Smith",
    address = AddressExample.oxfordStreet,
  )

  val CarlosGomez: Customer = Customer(
    id = Customer.Id(Uuid.parse("4bea62f7-583f-492c-aa2f-17c154c603d4")),
    firstName = "Carlos",
    lastName = "Gómez",
    address = AddressExample.granVia,
  )

  val GiuliaRossi: Customer = Customer(
    id = Customer.Id(Uuid.parse("44335efa-6984-42fa-b2db-3c02bbf4baea")),
    firstName = "Giulia",
    lastName = "Rossi",
    address = AddressExample.corsoVenezia,
  )

  val IngridLarsen: Customer = Customer(
    id = Customer.Id(Uuid.parse("50164a52-d184-409b-8875-b78567c4de28")),
    firstName = "Ingrid",
    lastName = "Larsen",
    address = AddressExample.karlJohansGate,
  )

  val SvenJansen: Customer = Customer(
    id = Customer.Id(Uuid.parse("b599eb68-ae76-4ee6-83e7-c73cc1c801bc")),
    firstName = "Sven",
    lastName = "Jansen",
    address = AddressExample.damrak,
  )

  val FranzHuber: Customer = Customer(
    id = Customer.Id(Uuid.parse("0784d2c2-b445-4962-ac11-a71e6d8d0d60")),
    firstName = "Franz",
    lastName = "Huber",
    address = AddressExample.kaiserstrasse,
  )

  val EmmaWilliams: Customer = Customer(
    id = Customer.Id(Uuid.parse("cb759b00-084d-4a7e-8310-1336e70458f5")),
    firstName = "Emma",
    lastName = "Williams",
    address = AddressExample.fifthAvenue,
  )

  val OliverBrown: Customer = Customer(
    id = Customer.Id(Uuid.parse("8a2cbad4-b312-4c9c-8c88-a7b631e236ad")),
    firstName = "Oliver",
    lastName = "Brown",
    address = AddressExample.collinsStreet,
  )

  val SophieLee: Customer = Customer(
    id = Customer.Id(Uuid.parse("bfb580eb-eb0d-489e-8ac7-b7db449598b6")),
    firstName = "Sophie",
    lastName = "Lee",
    address = AddressExample.robsonStreet,
  )

  val customers: List<Customer> = listOf(
    PeterMüller,
    LisaMüller,
    MarkusSchneider,
    AnnaWeber,
    JuliaSchmidt,
    TomSchulz,
    SarahNeumann,
    PaulBecker,
    LeaSchuster,
    NoahFischer,
    MiaWolf,
    EliasHoffmann,
    EmilyLang,
    LeonKrüger,
    SophieVogel,
    LucaSchwarz,
    MaxMeyer,
    NatalieBauer,
    YannickGraf,
    ChloeDubois,
    JamesSmith,
    CarlosGomez,
    GiuliaRossi,
    IngridLarsen,
    SvenJansen,
    FranzHuber,
    EmmaWilliams,
    OliverBrown,
    SophieLee
  )

  val customersGermany: List<Customer> = listOf(
    PeterMüller,
    LisaMüller,
    MarkusSchneider,
    AnnaWeber,
    JuliaSchmidt,
    TomSchulz,
    SarahNeumann,
    PaulBecker,
    LeaSchuster,
    NoahFischer,
    MiaWolf,
    EliasHoffmann,
    EmilyLang,
    LeonKrüger,
    SophieVogel,
    LucaSchwarz,
    MaxMeyer,
    NatalieBauer,
    YannickGraf
  )
}
