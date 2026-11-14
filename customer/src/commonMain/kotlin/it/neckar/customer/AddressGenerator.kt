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

import kotlin.random.Random

/**
 * Deterministic generator for [Address.Confirmed] instances.
 *
 * Generates realistic German and international addresses from combinatorial data pools.
 * The same [seed] always produces the same addresses.
 *
 * Usage:
 * ```
 * val generator = AddressGenerator()
 * val addresses = generator.generate(count = 10_000)
 * val singleAddress = generator[42]
 * ```
 */
class AddressGenerator(
  /**
   * The seed for deterministic random generation.
   * Same seed always produces the same addresses.
   */
  val seed: Int = DefaultSeed,
) {

  /**
   * Generates a single address for the given [index].
   * Each index deterministically produces the same address, regardless of prior calls.
   */
  operator fun get(index: Int): Address.Confirmed {
    val random = Random(seed.toLong() * 31 + index.toLong())

    val locationEntry = locations.let { pool ->
      pool[random.nextInt(pool.size)]
    }

    val street = streets.let { pool ->
      pool[random.nextInt(pool.size)]
    }

    val houseNumber = generateHouseNumber(random)

    return Address.Confirmed(
      street = street,
      houseNumber = houseNumber,
      zipCode = locationEntry.zipCode,
      city = locationEntry.city,
      country = locationEntry.country,
    )
  }

  /**
   * Generates a list of [count] addresses.
   */
  fun generate(count: Int): List<Address.Confirmed> {
    return List(count) { index -> this[index] }
  }

  companion object {
    const val DefaultSeed: Int = 73_951

    /**
     * Generates a realistic house number (1–199, optionally with suffix a/b/c).
     */
    fun generateHouseNumber(random: Random): String {
      val number = random.nextInt(1, 200)
      val suffixRoll = random.nextInt(100)
      val suffix = when {
        suffixRoll < 10 -> "a"
        suffixRoll < 15 -> "b"
        suffixRoll < 18 -> "c"
        else -> ""
      }
      return "$number$suffix"
    }

    /**
     * Pool of street names for address generation.
     */
    val streets: List<String> = listOf(
      // German streets
      "Hauptstraße",
      "Bahnhofstraße",
      "Gartenstraße",
      "Schulstraße",
      "Kirchstraße",
      "Bergstraße",
      "Dorfstraße",
      "Waldstraße",
      "Wiesenweg",
      "Ringstraße",
      "Lindenstraße",
      "Birkenweg",
      "Ahornstraße",
      "Buchenweg",
      "Eichenweg",
      "Tannenweg",
      "Rosenstraße",
      "Tulpenweg",
      "Sonnenallee",
      "Mondstraße",
      "Parkstraße",
      "Schlossweg",
      "Marktstraße",
      "Rathausplatz",
      "Brunnenweg",
      "Mühlenstraße",
      "Feldstraße",
      "Ackerweg",
      "Industriestraße",
      "Gewerbestraße",
      "Mozartstraße",
      "Beethovenstraße",
      "Schillerstraße",
      "Goethestraße",
      "Lessingstraße",
      "Hölderlinweg",
      "Karlstraße",
      "Friedrichstraße",
      "Wilhelmstraße",
      "Auguststraße",
      "Jahnstraße",
      "Turnstraße",
      "Sportplatzweg",
      "Steinweg",
      "Sandstraße",
      "Kiesweg",
      "Bachstraße",
      "Flussstraße",
      "Seeweg",
      "Teichstraße",
      "Am Hang",
      "Am Waldrand",
      "Am Sportplatz",
      "Am Markt",
      "Am Kirchberg",
      "Neue Straße",
      "Alte Straße",
      "Kurze Straße",
      "Lange Straße",
      "Breite Straße",
      "Schmale Gasse",
      "Obere Straße",
      "Untere Straße",
      "Dammstraße",
      "Deichstraße",
      "Hafenstraße",
      "Holstenwall",
      "Fliederweg",
      "Kirchgasse",
      "Albrechtstraße",
      "Gotthardstraße",
      // International streets
      "Rue de la Paix",
      "Rue Victor Hugo",
      "Boulevard Saint-Germain",
      "Avenue des Champs-Élysées",
      "Rue de Rivoli",
      "Oxford Street",
      "Baker Street",
      "King's Road",
      "High Street",
      "Park Lane",
      "Via Roma",
      "Via Garibaldi",
      "Corso Venezia",
      "Via Dante",
      "Calle Mayor",
      "Gran Vía",
      "Paseo de la Castellana",
      "Karl Johans gate",
      "Storgatan",
      "Damrak",
      "Keizersgracht",
      "Kaiserstraße",
      "Mariahilfer Straße",
      "Fifth Avenue",
      "Broadway",
      "Main Street",
      "Maple Avenue",
      "Collins Street",
      "Queen Street",
      "Robson Street",
      "Yonge Street",
    )

    /**
     * Pool of zip code / city / country combinations for address generation.
     */
    val locations: List<LocationEntry> = listOf(
      // German locations
      LocationEntry("10115", "Berlin", "Germany"),
      LocationEntry("10178", "Berlin", "Germany"),
      LocationEntry("10245", "Berlin", "Germany"),
      LocationEntry("10317", "Berlin", "Germany"),
      LocationEntry("10405", "Berlin", "Germany"),
      LocationEntry("10585", "Berlin", "Germany"),
      LocationEntry("10719", "Berlin", "Germany"),
      LocationEntry("12045", "Berlin", "Germany"),
      LocationEntry("12099", "Berlin", "Germany"),
      LocationEntry("13353", "Berlin", "Germany"),
      LocationEntry("20095", "Hamburg", "Germany"),
      LocationEntry("20249", "Hamburg", "Germany"),
      LocationEntry("20357", "Hamburg", "Germany"),
      LocationEntry("21073", "Hamburg", "Germany"),
      LocationEntry("22041", "Hamburg", "Germany"),
      LocationEntry("22303", "Hamburg", "Germany"),
      LocationEntry("80331", "München", "Germany"),
      LocationEntry("80469", "München", "Germany"),
      LocationEntry("80637", "München", "Germany"),
      LocationEntry("80799", "München", "Germany"),
      LocationEntry("81241", "München", "Germany"),
      LocationEntry("81675", "München", "Germany"),
      LocationEntry("50667", "Köln", "Germany"),
      LocationEntry("50823", "Köln", "Germany"),
      LocationEntry("50937", "Köln", "Germany"),
      LocationEntry("51065", "Köln", "Germany"),
      LocationEntry("70173", "Stuttgart", "Germany"),
      LocationEntry("70178", "Stuttgart", "Germany"),
      LocationEntry("70376", "Stuttgart", "Germany"),
      LocationEntry("70563", "Stuttgart", "Germany"),
      LocationEntry("60311", "Frankfurt am Main", "Germany"),
      LocationEntry("60318", "Frankfurt am Main", "Germany"),
      LocationEntry("60486", "Frankfurt am Main", "Germany"),
      LocationEntry("60594", "Frankfurt am Main", "Germany"),
      LocationEntry("40211", "Düsseldorf", "Germany"),
      LocationEntry("40215", "Düsseldorf", "Germany"),
      LocationEntry("40474", "Düsseldorf", "Germany"),
      LocationEntry("44135", "Dortmund", "Germany"),
      LocationEntry("44137", "Dortmund", "Germany"),
      LocationEntry("45127", "Essen", "Germany"),
      LocationEntry("45131", "Essen", "Germany"),
      LocationEntry("04109", "Leipzig", "Germany"),
      LocationEntry("04105", "Leipzig", "Germany"),
      LocationEntry("04229", "Leipzig", "Germany"),
      LocationEntry("01067", "Dresden", "Germany"),
      LocationEntry("01069", "Dresden", "Germany"),
      LocationEntry("01159", "Dresden", "Germany"),
      LocationEntry("28195", "Bremen", "Germany"),
      LocationEntry("28203", "Bremen", "Germany"),
      LocationEntry("30159", "Hannover", "Germany"),
      LocationEntry("30161", "Hannover", "Germany"),
      LocationEntry("90402", "Nürnberg", "Germany"),
      LocationEntry("90419", "Nürnberg", "Germany"),
      LocationEntry("47051", "Duisburg", "Germany"),
      LocationEntry("44787", "Bochum", "Germany"),
      LocationEntry("42103", "Wuppertal", "Germany"),
      LocationEntry("33602", "Bielefeld", "Germany"),
      LocationEntry("53111", "Bonn", "Germany"),
      LocationEntry("48143", "Münster", "Germany"),
      LocationEntry("76131", "Karlsruhe", "Germany"),
      LocationEntry("68159", "Mannheim", "Germany"),
      LocationEntry("86150", "Augsburg", "Germany"),
      LocationEntry("65183", "Wiesbaden", "Germany"),
      LocationEntry("41061", "Mönchengladbach", "Germany"),
      LocationEntry("38100", "Braunschweig", "Germany"),
      LocationEntry("09111", "Chemnitz", "Germany"),
      LocationEntry("24103", "Kiel", "Germany"),
      LocationEntry("39104", "Magdeburg", "Germany"),
      LocationEntry("55116", "Mainz", "Germany"),
      LocationEntry("79098", "Freiburg im Breisgau", "Germany"),
      LocationEntry("23552", "Lübeck", "Germany"),
      LocationEntry("99084", "Erfurt", "Germany"),
      LocationEntry("18055", "Rostock", "Germany"),
      LocationEntry("67655", "Kaiserslautern", "Germany"),
      LocationEntry("34117", "Kassel", "Germany"),
      LocationEntry("06108", "Halle (Saale)", "Germany"),
      LocationEntry("14467", "Potsdam", "Germany"),
      LocationEntry("89073", "Ulm", "Germany"),
      LocationEntry("85049", "Ingolstadt", "Germany"),
      LocationEntry("72116", "Mössingen", "Germany"),
      LocationEntry("72070", "Tübingen", "Germany"),
      LocationEntry("72764", "Reutlingen", "Germany"),
      LocationEntry("78462", "Konstanz", "Germany"),
      LocationEntry("88212", "Ravensburg", "Germany"),
      LocationEntry("73728", "Esslingen am Neckar", "Germany"),
      LocationEntry("71032", "Böblingen", "Germany"),
      LocationEntry("74072", "Heilbronn", "Germany"),
      LocationEntry("73033", "Göppingen", "Germany"),
      LocationEntry("88045", "Friedrichshafen", "Germany"),
      LocationEntry("72250", "Freudenstadt", "Germany"),
      LocationEntry("27568", "Bremerhaven", "Germany"),
      // French locations
      LocationEntry("75001", "Paris", "France"),
      LocationEntry("75004", "Paris", "France"),
      LocationEntry("75008", "Paris", "France"),
      LocationEntry("75011", "Paris", "France"),
      LocationEntry("69001", "Lyon", "France"),
      LocationEntry("13001", "Marseille", "France"),
      LocationEntry("31000", "Toulouse", "France"),
      LocationEntry("33000", "Bordeaux", "France"),
      // British locations
      LocationEntry("W1D 1LT", "London", "United Kingdom"),
      LocationEntry("EC1A 1BB", "London", "United Kingdom"),
      LocationEntry("SW1A 1AA", "London", "United Kingdom"),
      LocationEntry("M1 1AD", "Manchester", "United Kingdom"),
      LocationEntry("B1 1BB", "Birmingham", "United Kingdom"),
      LocationEntry("EH1 1YZ", "Edinburgh", "United Kingdom"),
      // Spanish locations
      LocationEntry("28001", "Madrid", "Spain"),
      LocationEntry("28013", "Madrid", "Spain"),
      LocationEntry("08001", "Barcelona", "Spain"),
      LocationEntry("41001", "Sevilla", "Spain"),
      LocationEntry("46001", "Valencia", "Spain"),
      // Italian locations
      LocationEntry("00100", "Roma", "Italy"),
      LocationEntry("00186", "Roma", "Italy"),
      LocationEntry("20121", "Milano", "Italy"),
      LocationEntry("50122", "Firenze", "Italy"),
      LocationEntry("30100", "Venezia", "Italy"),
      // Nordic locations
      LocationEntry("0159", "Oslo", "Norway"),
      LocationEntry("0160", "Oslo", "Norway"),
      LocationEntry("5003", "Bergen", "Norway"),
      LocationEntry("11120", "Stockholm", "Sweden"),
      LocationEntry("11453", "Stockholm", "Sweden"),
      LocationEntry("1050", "København", "Denmark"),
      LocationEntry("00100", "Helsinki", "Finland"),
      // Dutch locations
      LocationEntry("1012LG", "Amsterdam", "Netherlands"),
      LocationEntry("1017CT", "Amsterdam", "Netherlands"),
      LocationEntry("3011AA", "Rotterdam", "Netherlands"),
      LocationEntry("2511AA", "Den Haag", "Netherlands"),
      // Austrian locations
      LocationEntry("1010", "Wien", "Austria"),
      LocationEntry("1030", "Wien", "Austria"),
      LocationEntry("5020", "Salzburg", "Austria"),
      LocationEntry("6020", "Innsbruck", "Austria"),
      LocationEntry("8010", "Graz", "Austria"),
      // Swiss locations
      LocationEntry("8001", "Zürich", "Switzerland"),
      LocationEntry("3001", "Bern", "Switzerland"),
      LocationEntry("4001", "Basel", "Switzerland"),
      LocationEntry("1201", "Genève", "Switzerland"),
      // US locations
      LocationEntry("10001", "New York", "USA"),
      LocationEntry("10013", "New York", "USA"),
      LocationEntry("90001", "Los Angeles", "USA"),
      LocationEntry("60601", "Chicago", "USA"),
      LocationEntry("77001", "Houston", "USA"),
      LocationEntry("85001", "Phoenix", "USA"),
      // Australian locations
      LocationEntry("2000", "Sydney", "Australia"),
      LocationEntry("3000", "Melbourne", "Australia"),
      LocationEntry("4000", "Brisbane", "Australia"),
      // Canadian locations
      LocationEntry("V6E 1B5", "Vancouver", "Canada"),
      LocationEntry("M5H 2N2", "Toronto", "Canada"),
      LocationEntry("H2X 1Y4", "Montréal", "Canada"),
    )
  }
}

/**
 * A default [AddressGenerator] instance using the default seed.
 */
val Address.Companion.Generator: AddressGenerator
  get() = AddressGenerator()


/**
 * A location entry combining zip code, city, and country.
 */
data class LocationEntry(
  val zipCode: String,
  val city: String,
  val country: String,
)
