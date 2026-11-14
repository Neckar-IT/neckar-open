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

import assertk.*
import assertk.assertions.*
import it.neckar.open.serialization.roundTrip
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * Tests demonstrating kotlinx.serialization integration with domain interfaces.
 */
class DomainSerializationTest {

  private val json = Json {
    prettyPrint = true
  }

  @Serializable
  enum class ProductType(override val domainIdentifier: DomainIdentifier) : HasDomainIdentifier {
    @SerialName("switch")
    Switch(DomainIdentifier("electro")),

    @SerialName("outlet")
    Outlet(DomainIdentifier("electro")),

    @SerialName("radiator")
    Radiator(DomainIdentifier("heating")),

    @SerialName("thermostat")
    Thermostat(DomainIdentifier("heating"))
  }

  @Serializable
  private data class Product(
    val id: String,
    val name: String,
    val productType: ProductType
  ) {
    val domain: DomainIdentifier get() = productType.domainIdentifier
  }

  @Test
  fun testProductSerialization() {
    val product = Product("p1", "Light Switch", ProductType.Switch)

    val serialized = json.encodeToString(product)
    assertThat(serialized).isEqualTo(
      //language=JSON
      """
      {
          "id": "p1",
          "name": "Light Switch",
          "productType": "switch"
      }
      """.trimIndent()
    )

    roundTrip(product) {
      //language=JSON
      $$"""
        {
          "id" : "p1",
          "name" : "Light Switch",
          "productType" : "switch"
        }
      """.trimIndent()
    }
  }

  @Test
  fun testDomainIdentifierSerialization() {
    val domain = DomainIdentifier("electro")

    val serialized = json.encodeToString(domain)
    assertThat(serialized).isEqualTo("\"electro\"")

    roundTrip(domain) {
      //language=JSON
      $$"""
        "electro"
      """.trimIndent()
    }
  }

  private enum class PaymentType {
    CreditCard,
    BankTransfer,
    PayPal
  }

  /**
   * Sealed class hierarchy with type discriminator.
   *
   * The [typeDiscriminator] is derived from the class type in the base class,
   * so it has no backing field and is NOT serialized. This avoids duplication
   * with kotlinx.serialization's "type" discriminator.
   */
  @Serializable
  private sealed class Payment : HasTypeDiscriminator<PaymentType> {
    abstract val amount: Double

    /**
     * Derived from the concrete class type - no backing field, not serialized.
     */
    override val typeDiscriminator: PaymentType
      get() = when (this) {
        is CreditCard -> PaymentType.CreditCard
        is BankTransfer -> PaymentType.BankTransfer
        is PayPal -> PaymentType.PayPal
      }

    @Serializable
    @SerialName("CreditCard")
    data class CreditCard(
      override val amount: Double,
      val cardNumber: String
    ) : Payment()

    @Serializable
    @SerialName("BankTransfer")
    data class BankTransfer(
      override val amount: Double,
      val iban: String
    ) : Payment()

    @Serializable
    @SerialName("PayPal")
    data class PayPal(
      override val amount: Double,
      val email: String
    ) : Payment()
  }

  @Test
  fun testPaymentSerialization() {
    val payment: Payment = Payment.CreditCard(100.0, "1234-5678")

    val serialized = json.encodeToString(Payment.serializer(), payment)
    // "type" is the kotlinx.serialization discriminator.
    // typeDiscriminator is NOT serialized because it's a computed property without backing field.
    assertThat(serialized).isEqualTo(
      //language=JSON
      """
      {
          "type": "CreditCard",
          "amount": 100.0,
          "cardNumber": "1234-5678"
      }
      """.trimIndent()
    )

    // typeDiscriminator is derived in the base class, so it has no backing field
    // and is never serialized - no duplication with "type"!
    roundTrip(payment, Payment.serializer()) {
      //language=JSON
      $$"""
        {
          "type" : "CreditCard",
          "amount" : 100.0,
          "cardNumber" : "1234-5678"
        }
      """.trimIndent()
    }
  }

  @Test
  fun testPaymentListSerialization() {
    val payments: List<Payment> = listOf(
      Payment.CreditCard(100.0, "1234-5678"),
      Payment.BankTransfer(200.0, "DE89370400440532013000"),
      Payment.PayPal(50.0, "user@example.com")
    )

    val serialized = json.encodeToString(payments)
    assertThat(serialized).isEqualTo(
      //language=JSON
      """
      [
          {
              "type": "CreditCard",
              "amount": 100.0,
              "cardNumber": "1234-5678"
          },
          {
              "type": "BankTransfer",
              "amount": 200.0,
              "iban": "DE89370400440532013000"
          },
          {
              "type": "PayPal",
              "amount": 50.0,
              "email": "user@example.com"
          }
      ]
      """.trimIndent()
    )

    // After deserialization, typeDiscriminator is available for filtering
    val deserialized = json.decodeFromString<List<Payment>>(serialized)
    val creditCards = deserialized.filter { it.typeDiscriminator == PaymentType.CreditCard }
    assertThat(creditCards.size).isEqualTo(1)
  }
}
