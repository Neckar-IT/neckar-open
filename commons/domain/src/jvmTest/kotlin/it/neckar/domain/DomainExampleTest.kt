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
import org.junit.jupiter.api.Test

/**
 * Example implementations and tests demonstrating the usage of domain interfaces.
 */
class DomainExampleTest {

  /**
   * Example enum representing product categories with associated domains.
   */
  enum class ProductCategory(override val domainIdentifier: DomainIdentifier) : HasDomainIdentifier {
    Switch(DomainIdentifier("electro")),
    Outlet(DomainIdentifier("electro")),
    Cable(DomainIdentifier("electro")),
    Radiator(DomainIdentifier("heating")),
    Thermostat(DomainIdentifier("heating")),
    Pipe(DomainIdentifier("plumbing")),
    Faucet(DomainIdentifier("plumbing"))
  }

  @Test
  fun testProductCategoryDomains() {
    assertThat(ProductCategory.Switch.domainIdentifier).isEqualTo(DomainIdentifier("electro"))
    assertThat(ProductCategory.Radiator.domainIdentifier).isEqualTo(DomainIdentifier("heating"))
    assertThat(ProductCategory.Pipe.domainIdentifier).isEqualTo(DomainIdentifier("plumbing"))

    // Filter all electro categories
    val electroCategories = ProductCategory.entries.filter {
      it.domainIdentifier == DomainIdentifier("electro")
    }
    assertThat(electroCategories).hasSize(3)
    assertThat(electroCategories).isEqualTo(listOf(ProductCategory.Switch, ProductCategory.Outlet, ProductCategory.Cable))
  }

  /**
   * Example data class implementing [HasDomainIdentifier] - mirrors the KDoc example of that interface.
   */
  data class CatalogItem(
    val id: String,
    val name: String,
    override val domainIdentifier: DomainIdentifier
  ) : HasDomainIdentifier

  @Test
  fun testCatalogItemDomain() {
    val item = CatalogItem("c1", "Wall Switch", DomainIdentifier("electro"))
    assertThat(item.domainIdentifier).isEqualTo(DomainIdentifier("electro"))
  }

  /**
   * Example enum for payment type discrimination.
   */
  enum class PaymentType {
    CreditCard,
    BankTransfer,
    PayPal
  }

  /**
   * Example sealed class hierarchy with type discriminator.
   */
  private sealed class Payment : HasTypeDiscriminator<PaymentType> {
    abstract val amount: Double

    data class CreditCard(
      override val amount: Double,
      val cardNumber: String,
      val expiryDate: String
    ) : Payment() {
      override val typeDiscriminator: PaymentType = PaymentType.CreditCard
    }

    data class BankTransfer(
      override val amount: Double,
      val iban: String,
      val bic: String
    ) : Payment() {
      override val typeDiscriminator: PaymentType = PaymentType.BankTransfer
    }

    data class PayPal(
      override val amount: Double,
      val email: String
    ) : Payment() {
      override val typeDiscriminator: PaymentType = PaymentType.PayPal
    }
  }

  @Test
  fun testPaymentTypeDiscriminator() {
    val payments: List<Payment> = listOf(
      Payment.CreditCard(100.0, "1234-5678-9012-3456", "12/25"),
      Payment.BankTransfer(200.0, "DE89370400440532013000", "COBADEFFXXX"),
      Payment.PayPal(50.0, "user@example.com"),
      Payment.CreditCard(75.0, "9876-5432-1098-7654", "06/24")
    )

    // Filter by type
    val creditCardPayments = payments.filter { it.typeDiscriminator == PaymentType.CreditCard }
    assertThat(creditCardPayments).hasSize(2)

    val bankTransfers = payments.filter { it.typeDiscriminator == PaymentType.BankTransfer }
    assertThat(bankTransfers).hasSize(1)

    // Calculate total by type
    val creditCardTotal = creditCardPayments.sumOf { it.amount }
    assertThat(creditCardTotal).isEqualTo(175.0)
  }

  @Test
  fun testDomainIdentifierBasics() {
    val electro = DomainIdentifier("electro")

    // Value access
    assertThat(electro.value).isEqualTo("electro")

    // toString returns the value
    assertThat(electro.toString()).isEqualTo("electro")

    // Equality
    assertThat(electro).isEqualTo(DomainIdentifier("electro"))

    // Self-reference (DomainIdentifier implements HasDomainIdentifier)
    assertThat(electro.domainIdentifier).isEqualTo(electro)

    // Unknown domain
    assertThat(DomainIdentifier.Unknown.value).isEqualTo("unknown")
  }
}
