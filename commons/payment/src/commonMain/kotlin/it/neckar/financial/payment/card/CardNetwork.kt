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
package it.neckar.financial.payment.card

import it.neckar.open.annotations.GermanTerm
import it.neckar.open.kotlin.lang.ValueClassString
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * Card network of a [PaymentMethod.CardPayment].
 *
 * Modelled as an **open vocabulary** value class rather than a closed `enum class`: the set of
 * real-world card networks is governed externally and grows over time (girocard, Diners,
 * Discover, JCB, UnionPay, …), so a closed enum would force a code change plus a data migration
 * whenever a new network appears. The well-known networks are exposed as named constants
 * ([Visa], [Mastercard], [Amex]) for type-safe references and the UI dropdown, while any value
 * still constructs — the contrast with the closed [PaymentMethodKind] discriminator enum is
 * intentional and part of what this living example demonstrates.
 */
@GermanTerm("Kartennetzwerk")
@Serializable
@JvmInline
value class CardNetwork(override val value: String) : ValueClassString {

  init {
    require(value.isNotBlank()) { "CardNetwork must not be blank" }
  }

  override fun toString(): String = value

  companion object {
    val Visa: CardNetwork = CardNetwork("Visa")
    val Mastercard: CardNetwork = CardNetwork("Mastercard")
    val Amex: CardNetwork = CardNetwork("Amex")

    /** The well-known card networks offered in the UI dropdown — the vocabulary stays open. */
    val knownNetworks: List<CardNetwork> = listOf(Visa, Mastercard, Amex)
  }
}
