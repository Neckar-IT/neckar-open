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
package it.neckar.domain

/**
 * Marks types that have a corresponding enum-based type discriminator.
 *
 * This interface is useful when you have a sealed class hierarchy or similar
 * polymorphic structure where each concrete type can be identified by an enum value.
 * Common use cases include:
 * - REST API filtering by type
 * - Serialization/deserialization with type discriminators
 * - UI components that need to display type-specific information
 *
 * ## Recommended Usage with Sealed Classes
 *
 * Define [typeDiscriminator] as a computed property in the base class to avoid
 * duplication with kotlinx.serialization's "type" discriminator:
 *
 * ```kotlin
 * enum class PaymentType {
 *   CreditCard,
 *   BankTransfer,
 *   PayPal
 * }
 *
 * @Serializable
 * sealed class Payment : HasTypeDiscriminator<PaymentType> {
 *   abstract val amount: Double
 *
 *   // Derived from class type - no backing field, not serialized
 *   override val typeDiscriminator: PaymentType
 *     get() = when (this) {
 *       is CreditCard -> PaymentType.CreditCard
 *       is BankTransfer -> PaymentType.BankTransfer
 *       is PayPal -> PaymentType.PayPal
 *     }
 *
 *   @Serializable
 *   data class CreditCard(override val amount: Double, val number: String) : Payment()
 *
 *   @Serializable
 *   data class BankTransfer(override val amount: Double, val iban: String) : Payment()
 *
 *   @Serializable
 *   data class PayPal(override val amount: Double, val email: String) : Payment()
 * }
 *
 * // Filter payments by type
 * val creditCardPayments = payments.filter { it.typeDiscriminator == PaymentType.CreditCard }
 * ```
 *
 * This approach ensures:
 * - [typeDiscriminator] is available at runtime for filtering
 * - No duplication with `kotlinx.serialization`'s "type" field in JSON
 * - Exhaustive when-expression catches missing cases at compile time
 *
 * @param T The enum type used as the discriminator
 */
interface HasTypeDiscriminator<T : Enum<T>> {
  /**
   * The enum value that identifies this type.
   *
   * Note: This property is intentionally named `typeDiscriminator` instead of `type`
   * because `type` is the default discriminator property name used by `kotlinx.serialization`
   * for polymorphic serialization. Using `type` would cause conflicts when serializing
   * sealed class hierarchies.
   *
   * **Recommendation:** Implement this as a computed property (using `get()`) in the
   * sealed class base to avoid serialization and ensure no backing field is created.
   */
  val typeDiscriminator: T
}

