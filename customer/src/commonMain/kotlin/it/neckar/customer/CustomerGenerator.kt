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

import it.neckar.contact.EmailAddress
import it.neckar.contact.PhoneNumber
import kotlin.random.Random
import kotlin.uuid.Uuid

/**
 * Deterministic generator for [Customer] instances.
 *
 * Generates realistic customers with German and international names, addresses,
 * phone numbers, email addresses, and optional company names.
 * The same [seed] always produces the same customers.
 *
 * Usage:
 * ```
 * val generator = CustomerGenerator()
 * val customers = generator.generate(count = 10_000)
 * val singleCustomer = generator[42]
 * ```
 */
class CustomerGenerator(
  /**
   * The seed for deterministic random generation.
   * Same seed always produces the same customers.
   */
  val seed: Int = DefaultSeed,
) {

  private val addressGenerator: AddressGenerator = AddressGenerator(seed = seed * 17 + 3)

  /**
   * Generates a single customer for the given [index].
   * Each index deterministically produces the same customer, regardless of prior calls.
   */
  operator fun get(index: Int): Customer {
    val random = Random(seed.toLong() * 37 + index.toLong())

    val firstName = firstNames.let { pool ->
      pool[random.nextInt(pool.size)]
    }

    val lastName = lastNames.let { pool ->
      pool[random.nextInt(pool.size)]
    }

    val id = Customer.Id(
      Uuid.fromLongs(
        mostSignificantBits = random.nextLong(),
        leastSignificantBits = random.nextLong(),
      ),
    )

    val address = if (random.nextInt(100) < 90) {
      addressGenerator[index]
    } else {
      null
    }

    val phone = if (random.nextInt(100) < 60) {
      generatePhoneNumber(random)
    } else {
      null
    }

    val cellphone = if (random.nextInt(100) < 70) {
      generateCellphoneNumber(random)
    } else {
      null
    }

    val email = if (random.nextInt(100) < 80) {
      generateEmail(firstName, lastName, random)
    } else {
      null
    }

    val company = if (random.nextInt(100) < 15) {
      generateCompanyName(random)
    } else {
      null
    }

    return Customer(
      id = id,
      firstName = firstName,
      lastName = lastName,
      address = address,
      phone = phone,
      cellphone = cellphone,
      email = email,
      company = company,
    )
  }

  /**
   * Generates a list of [count] customers.
   */
  fun generate(count: Int): List<Customer> {
    return List(count) { index -> this[index] }
  }

  companion object {
    const val DefaultSeed: Int = 73_951

    /**
     * Generates a realistic German landline phone number (e.g. `+49 89 1234567`).
     */
    fun generatePhoneNumber(random: Random): PhoneNumber {
      val areaCode = phoneAreaCodes[random.nextInt(phoneAreaCodes.size)]
      val numberLength = random.nextInt(6, 9)
      val number = buildString {
        repeat(numberLength) {
          append(random.nextInt(0, 10))
        }
      }
      return PhoneNumber("+49 $areaCode $number")
    }

    /**
     * Generates a realistic German mobile phone number (e.g. `+49 176 12345678`).
     */
    fun generateCellphoneNumber(random: Random): PhoneNumber {
      val prefix = cellphonePrefixes[random.nextInt(cellphonePrefixes.size)]
      val number = buildString {
        repeat(8) {
          append(random.nextInt(0, 10))
        }
      }
      return PhoneNumber("+49 $prefix $number")
    }

    /**
     * Generates a plausible email address derived from the given [firstName] and [lastName].
     */
    fun generateEmail(firstName: String, lastName: String, random: Random): EmailAddress {
      val domain = emailDomains[random.nextInt(emailDomains.size)]
      val normalizedFirst = firstName.lowercase().normalizeForEmail()
      val normalizedLast = lastName.lowercase().normalizeForEmail()

      val raw = when (random.nextInt(4)) {
        0 -> "$normalizedFirst.$normalizedLast@$domain"
        1 -> "$normalizedFirst.$normalizedLast${random.nextInt(10, 100)}@$domain"
        2 -> "${normalizedFirst[0]}.$normalizedLast@$domain"
        else -> "$normalizedFirst$normalizedLast@$domain"
      }
      return EmailAddress(raw)
    }

    /**
     * Generates a realistic German company name (e.g. `Nordwind Elektrotechnik GmbH`).
     */
    fun generateCompanyName(random: Random): String {
      val name = companyNames[random.nextInt(companyNames.size)]
      val suffix = companySuffixes[random.nextInt(companySuffixes.size)]
      return "$name $suffix"
    }

    /**
     * Normalizes a name for use in email addresses by replacing umlauts and special characters.
     */
    fun String.normalizeForEmail(): String {
      return this
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("ß", "ss")
        .replace("é", "e")
        .replace("è", "e")
        .replace("ê", "e")
        .replace("á", "a")
        .replace("à", "a")
        .replace("ó", "o")
        .replace("ò", "o")
        .replace("ñ", "n")
        .replace("ø", "o")
        .replace("å", "a")
        .replace(" ", "-")
    }

    /**
     * Pool of first names for customer generation.
     */
    val firstNames: List<String> = listOf(
      // German first names
      "Max", "Anna", "Lukas", "Sophie", "Leon", "Mia", "Elias", "Emilia",
      "Paul", "Marie", "Noah", "Lea", "Finn", "Lina", "Jonas", "Emma",
      "Ben", "Hannah", "Felix", "Clara", "Luca", "Laura", "Moritz", "Sarah",
      "Julian", "Julia", "David", "Lisa", "Tim", "Nele", "Niklas", "Leni",
      "Alexander", "Charlotte", "Jan", "Katharina", "Tobias", "Johanna", "Daniel", "Helena",
      "Simon", "Eva", "Florian", "Theresa", "Philipp", "Sophia", "Maximilian", "Amelie",
      "Sebastian", "Frieda", "Matthias", "Ida", "Thomas", "Greta", "Andreas", "Marlene",
      "Stefan", "Helene", "Christian", "Luise", "Michael", "Martha", "Peter", "Natalie",
      "Martin", "Franziska", "Markus", "Antonia", "Oliver", "Carla",
      // International first names
      "James", "Emily", "Carlos", "Giulia", "Ingrid", "Sven", "Chloé", "Pierre",
      "François", "Isabelle", "William", "Olivia", "Santiago", "Valentina", "Marco", "Chiara",
      "Lars", "Astrid", "Erik", "Freya", "Hans", "Brigitte", "Franz", "Maria",
      "Jean", "Margaux", "George", "Victoria", "Alejandro", "Carmen", "Giuseppe", "Francesca",
      "Henrik", "Sigrid", "Olaf", "Katrine", "Pieter", "Annika", "Yannick", "Céline",
    )

    /**
     * Pool of last names for customer generation.
     */
    val lastNames: List<String> = listOf(
      // German last names
      "Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner", "Becker",
      "Schulz", "Hoffmann", "Schäfer", "Koch", "Bauer", "Richter", "Klein", "Wolf",
      "Schröder", "Neumann", "Schwarz", "Zimmermann", "Braun", "Krüger", "Hofmann", "Hartmann",
      "Lange", "Schmitt", "Werner", "Schmitz", "Krause", "Meier", "Lehmann", "Schmid",
      "Schulze", "Maier", "Köhler", "Herrmann", "König", "Walter", "Mayer", "Huber",
      "Kaiser", "Fuchs", "Peters", "Lang", "Scholz", "Möller", "Weiß", "Jung",
      "Hahn", "Schubert", "Vogel", "Friedrich", "Keller", "Günther", "Frank", "Berger",
      "Winkler", "Roth", "Beck", "Lorenz", "Baumann", "Franke", "Albrecht", "Schuster",
      "Simon", "Ludwig", "Böhm", "Winter", "Kraus", "Graf",
      // International last names
      "Smith", "Williams", "Brown", "Jones", "Wilson", "Taylor", "Davies", "Evans",
      "Dubois", "Martin", "Bernard", "Lefebvre", "Moreau", "Laurent", "Rossi", "Russo",
      "Ferrari", "Colombo", "García", "Martínez", "López", "González", "Rodríguez", "Fernández",
      "Larsen", "Hansen", "Andersen", "Johansson", "Eriksson", "Nilsson", "Jansen", "De Vries",
      "Van den Berg", "Bakker",
    )

    /**
     * Pool of German landline area codes.
     */
    val phoneAreaCodes: List<String> = listOf(
      "30", "40", "69", "89", "711", "221", "211", "231",
      "341", "351", "421", "511", "911", "201", "228", "251",
      "721", "621", "821", "611", "202", "521", "531", "361",
      "381", "431", "391", "6131", "761", "451", "561", "345",
      "7071", "7121", "7531", "751", "7161", "7541",
    )

    /**
     * Pool of German mobile phone prefixes.
     */
    val cellphonePrefixes: List<String> = listOf(
      "151", "152", "157", "160", "162", "163", "170", "171",
      "172", "173", "174", "175", "176", "177", "178", "179",
    )

    /**
     * Pool of email domains.
     */
    val emailDomains: List<String> = listOf(
      "gmail.com", "web.de", "gmx.de", "outlook.com", "yahoo.de",
      "t-online.de", "posteo.de", "mailbox.org", "icloud.com", "protonmail.com",
      "freenet.de", "arcor.de", "hotmail.com", "live.de", "mail.de",
    )

    /**
     * Pool of company name stems.
     */
    val companyNames: List<String> = listOf(
      "Müller", "Schmidt", "Wagner", "Becker", "Schneider",
      "Hoffmann", "König", "Kaiser", "Richter", "Wolf",
      "Braun", "Vogel", "Berger", "Lang", "Friedrich",
      "Huber", "Franke", "Weber", "Fischer", "Keller",
      "Nordwind", "Südkreuz", "Ostwerk", "Westpark", "Rheintal",
      "Sonnenberg", "Waldblick", "Seerose", "Bergmann", "Stadtwerk",
    )

    /**
     * Pool of company legal form suffixes.
     */
    val companySuffixes: List<String> = listOf(
      "GmbH", "GmbH & Co. KG", "AG", "e.K.", "OHG",
      "KG", "UG", "Elektrotechnik GmbH", "Solartechnik GmbH", "Haustechnik GmbH",
      "Gebäudetechnik GmbH", "Energietechnik GmbH", "Bau GmbH", "Service GmbH", "Consulting GmbH",
    )
  }
}

/**
 * A default [CustomerGenerator] instance using the default seed.
 */
val Customer.Companion.Generator: CustomerGenerator
  get() = CustomerGenerator()
