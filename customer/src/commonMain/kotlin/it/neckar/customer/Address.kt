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

import it.neckar.open.annotations.CrossLayer
import it.neckar.rest.draft.Confirmable
import it.neckar.rest.draft.IsConfirmed
import it.neckar.rest.draft.IsDraft
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Address with Draft / Confirmed states.
 *
 * [Draft] is what a partially filled form produces — every field may be missing. [Confirmed] is
 * the state an address reaches once all address lines are present, which is why its fields are
 * non-null.
 *
 * Neither state rejects *blank* strings: [Confirmed] only rules out `null`. The rendering members
 * below therefore have to cope with empty parts in both states.
 */
@CrossLayer("Identical wire shape across Domain, Entity, DataView.")
@Serializable
sealed interface Address : Confirmable {
  val street: String?
  val houseNumber: String?
  val zipCode: String?
  val city: String?

  /**
   * The country as free text, not an ISO country code. New addresses start out as `"Germany"`
   * — see [empty] and [invoke].
   */
  val country: String?

  /**
   * Street and house number on one line, for display.
   *
   * Never null, but possibly empty or holding only one of the two parts: in a [Draft] both are
   * optional, and in a [Confirmed] address either may still be blank.
   */
  val streetWithHouseNumber: String

  /**
   * Zip code and city on one line, for display — zip code first, as German addresses are written.
   *
   * Never null, but possibly empty or holding only one of the two parts: in a [Draft] both are
   * optional, and in a [Confirmed] address either may still be blank.
   */
  val cityWithZipCode: String

  /**
   * The address on a single line: `"<street> <houseNumber>, <zipCode> <city>"`. [country] is not
   * part of it. If the street half or the city half is entirely missing or blank, it is dropped
   * along with the comma, so the result never starts or ends with a separator.
   */
  fun singleLine(): String

  @CrossLayer("Identical wire shape across Domain, Entity, DataView.")
  @Serializable
  @SerialName("Draft")
  data class Draft(
    // No property defaults on wire types: a default makes the field optional in the OpenAPI spec
    // (TS: `street?: string | null`) and lets JSON omit it (undefined in the frontend). Every field
    // is required-but-nullable on the wire; construction convenience lives in [Address.empty].
    override val street: String?,
    override val houseNumber: String?,
    override val zipCode: String?,
    override val city: String?,
    override val country: String?,
  ) : Address, IsDraft {
    /**
     * Always [it.neckar.rest.transition.ValidationResult.Valid], and [now] is ignored: an address
     * draft has no rule of its own. The only constraint — a [Confirmed] address needs every field
     * — is already carried by the non-null types of [Confirmed].
     *
     * [Draft] also does not override `confirm`, so calling it hits the default in
     * [it.neckar.rest.draft.IsDraft] and throws. There is no draft-to-confirmed transition for
     * addresses; a [Confirmed] address is constructed directly from complete input.
     */
    override fun validate(now: kotlin.time.Instant): it.neckar.rest.transition.ValidationResult<it.neckar.rest.transition.ValidationError> = it.neckar.rest.transition.ValidationResult.Valid

    override val streetWithHouseNumber: String
      get() = listOfNotNull(street, houseNumber).joinToString(" ")

    override val cityWithZipCode: String
      get() = listOfNotNull(zipCode, city).joinToString(" ")

    override fun singleLine(): String {
      val streetPart = listOfNotNull(street, houseNumber).joinToString(" ")
      val cityPart = listOfNotNull(zipCode, city).joinToString(" ")
      return listOfNotNull(streetPart.ifBlank { null }, cityPart.ifBlank { null }).joinToString(", ")
    }
  }

  @CrossLayer("Identical wire shape across Domain, Entity, DataView.")
  @Serializable
  @SerialName("Confirmed")
  data class Confirmed(
    override val street: String,
    override val houseNumber: String,
    override val zipCode: String,
    override val city: String,
    // No property default on wire types (see Draft) — the "Germany" default lives in [Address.invoke].
    override val country: String,
  ) : Address, IsConfirmed {

    override val streetWithHouseNumber: String
      get() = "$street $houseNumber"

    override val cityWithZipCode: String
      get() = "$zipCode $city"

    override fun singleLine(): String {
      // trim(' ', ',') is what drops an empty half: the fields are non-null but may be blank, so
      // an address without a street renders as ", 72116 Mössingen" before trimming.
      return "$street $houseNumber, $zipCode $city".trim(' ', ',')
    }

    /**
     * Every field carries over unchanged — a confirmed address always satisfies the weaker [Draft]
     * contract, so this conversion cannot fail or lose information.
     */
    override fun toDraft(): Draft {
      return Draft(
        street = street,
        houseNumber = houseNumber,
        zipCode = zipCode,
        city = city,
        country = country,
      )
    }
  }

  companion object {
    /**
     * The starting point for a new address form. Empty except for [country], which is pre-filled
     * with `"Germany"` so the common case needs no input.
     */
    val empty: Draft = Draft(
      street = null,
      houseNumber = null,
      zipCode = null,
      city = null,
      country = "Germany",
    )

    /**
     * Creates a [Confirmed] address. Writing `Address(...)` at a call site therefore yields the
     * confirmed state, never the sealed interface — the call requires every address line, which
     * is exactly what [Confirmed] demands. [country] is the one parameter with a default.
     */
    operator fun invoke(
      street: String,
      houseNumber: String,
      zipCode: String,
      city: String,
      country: String = "Germany",
    ): Confirmed {
      return Confirmed(
        street = street,
        houseNumber = houseNumber,
        zipCode = zipCode,
        city = city,
        country = country,
      )
    }
  }
}

/**
 * Returns the street with house number or an empty string if the address is null
 */
fun Address?.streetWithHouseNumber(): String {
  return this?.streetWithHouseNumber ?: ""
}

/**
 * Returns the city with zip code or an empty string if the address is null
 */
fun Address?.cityWithZipCode(): String {
  return this?.cityWithZipCode ?: ""
}
