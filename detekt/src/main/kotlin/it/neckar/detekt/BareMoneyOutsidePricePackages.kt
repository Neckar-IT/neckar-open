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
package it.neckar.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

/**
 * Rejects a type reference to the bare `it.neckar.financial.currency.Money` inside a price package.
 *
 * A price package prices things: every amount there is a net, a gross or a tax amount, and
 * `Money.Net`, `Money.Gross`, `Money.Vat` and `Money.WithVat` say which. The bare `Money` carries no
 * tax basis and belongs to amounts that have none — a loan, a subsidy, an earning. Which packages
 * price things is configured per repository; the rule itself names no product.
 *
 * ```yaml
 * neckar-rules:
 *   BareMoneyOutsidePricePackages:
 *     active: true
 *     pricePackages:
 *       - "it.neckar.financial.priced"
 *       - "it.neckar.price"
 * ```
 *
 * The rule reads the syntax tree: a type reference whose type is the unqualified `Money` (or its import
 * alias) in a file that imports `it.neckar.financial.currency.Money`, or the fully qualified name itself.
 * A package entry matches the package and everything under it.
 */
class BareMoneyOutsidePricePackages(config: Config) : Rule(
  config,
  description = "A price package names the tax basis of every amount: `Money.Net`, `Money.Gross`, `Money.Vat` or `Money.WithVat`, never the bare `Money`.",
) {

  /** Package prefixes whose files price things. */
  private val pricePackages: List<String> by config(emptyList<String>())

  override fun visitTypeReference(typeReference: KtTypeReference) {
    super.visitTypeReference(typeReference)

    val file = typeReference.containingKtFile
    if (file.isInPricePackage().not()) return

    val userType = typeReference.typeElement?.unwrapNullable() as? KtUserType ?: return
    if (userType.isBareMoney(file).not()) return

    report(
      Finding(
        Entity.from(typeReference),
        "`${file.packageFqName.asString()}` prices things: this amount is a `Money.Net`, `Money.Gross`, `Money.Vat` or `Money.WithVat`, not the bare `Money`.",
      ),
    )
  }

  private fun KtFile.isInPricePackage(): Boolean {
    val packageName = packageFqName.asString()
    return pricePackages.any { prefix -> packageName == prefix || packageName.startsWith("$prefix.") }
  }

  private fun KtTypeElement.unwrapNullable(): KtTypeElement? {
    return if (this is KtNullableType) innerType?.unwrapNullable() else this
  }

  private fun KtUserType.isBareMoney(file: KtFile): Boolean {
    if (qualifier == null) {
      return referencedName in file.simpleNamesOfMoney()
    }
    return text == MoneyFqName
  }

  /** The simple names under which the file sees the currency `Money`: the import alias, else `Money`; empty without an import. */
  private fun KtFile.simpleNamesOfMoney(): List<String> {
    return importDirectives.mapNotNull { directive ->
      val imported = directive.importedFqName?.asString() ?: return@mapNotNull null
      when {
        imported == MoneyFqName -> directive.aliasName ?: MoneySimpleName
        directive.isAllUnder && imported == MoneyPackage -> MoneySimpleName
        else -> null
      }
    }
  }

  companion object {
    private const val MoneyPackage: String = "it.neckar.financial.currency"
    private const val MoneySimpleName: String = "Money"
    private const val MoneyFqName: String = "$MoneyPackage.$MoneySimpleName"
  }
}
