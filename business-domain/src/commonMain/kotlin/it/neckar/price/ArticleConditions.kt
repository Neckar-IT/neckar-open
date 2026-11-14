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
package it.neckar.price

import it.neckar.financial.currency.Money
import it.neckar.financial.currency.PricePerHour
import it.neckar.financial.currency.QuantityUnit
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.open.annotations.CrossLayer
import it.neckar.open.annotations.GermanTerm
import it.neckar.open.annotations.OpenApiBaseSchema
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * The commercial summary of a catalog position at a point in time: the price for the part itself,
 * the [unit] everything refers to, and — depending on the article — an accessory price and an
 * installation time.
 *
 * Trade-neutral — usable for every priced master-data record (book, PV module, battery, cable, …).
 * Held by the owning entity as an [ArticleConditionsTimeline], so all values are carried forward
 * together as one time-versioned state.
 *
 * ## The implementation belongs to the article type
 *
 * Which commercial parts an article *has* follows from what kind of article it is, not from what a
 * single price change happens to fill in: a book has no installation time at all, a cable has one
 * for every entry of its timeline. Each implementation names one of those shapes, and the type that
 * owns the timeline pins the implementation it uses — `ValueTimeline<BareArticleConditions>` for a
 * book, `ValueTimeline<FullArticleConditions>` for a cable. Nothing is ever serialized through this
 * interface, so no discriminator is written and no polymorphic registration is needed.
 *
 * | Implementation | accessory | installationTime | Typical article |
 * |---|---|---|---|
 * | [BareArticleConditions] | – | – | a book |
 * | [ArticleConditionsWithAccessory] | ✓ | – | an article shipped with small material |
 * | [ArticleConditionsWithInstallationTime] | – | ✓ | an article that is mounted |
 * | [FullArticleConditions] | ✓ | ✓ | a cable |
 *
 * The four are what ships; the interface is open, so a product module adds its own implementation
 * where none of them describes its articles.
 *
 * This interface declares only what *every* article has: the [part] and the [unit] it is priced by.
 * An accessory price and an installation time are declared by the implementations that have them —
 * a reader that displays an accessory asks for an implementation carrying one, it does not read a
 * property that is absent for half the articles.
 *
 * A property an implementation declares is nullable: the shape says the article type *can* carry an
 * accessory, an individual article of that type still may not.
 *
 * The hourly rate does not belong here: labour cost is derived from an implementation's installation
 * time and the tenant's rate, see [it.neckar.financial.currency.PricePerHour.times].
 */
@OpenApiBaseSchema
@CrossLayer("ArticleConditions — identical shape in Domain, Entity and REST")
@GermanTerm("Artikel-Konditionen")
interface ArticleConditions {
  /** Purchase and selling price of the article itself, both per [unit]. */
  @GermanTerm("Teil")
  val part: PriceWithProfit

  /**
   * The reference unit the whole record is priced by — the [part] and every optional part an
   * implementation adds all relate to one of it. `null` is the canonical encoding for
   * [QuantityUnit.Piece] ("Stück"): the common per-piece case needs no input, and the same meaning is
   * never stored in two forms. A different unit (Meter, m², kg, Pauschale, …) is set explicitly.
   */
  @GermanTerm("Einheit")
  val unit: QuantityUnit?

  companion object {
    /**
     * The invariants every implementation shares: prices are not negative, the per-piece default is
     * encoded as `null`, and a stated installation time is a real duration.
     */
    internal fun requireValidConditions(part: PriceWithProfit, unit: QuantityUnit?, installationTime: Duration?) {
      require(part.inputPrice >= Money.Net.Zero) { "part purchase price must be >= 0 but was <${part.inputPrice}>" }
      require(part.sellingPrice >= Money.Net.Zero) { "part selling price must be >= 0 but was <${part.sellingPrice}>" }
      require(unit != QuantityUnit.Piece) { "unit \"${QuantityUnit.Piece}\" must be encoded as null, not as an explicit unit, but was <$unit>" }
      installationTime?.let {
        require(it > Duration.ZERO) { "installationTime must be > 0 when set but was <$it>" }
      }
    }
  }
}

/**
 * Conditions of an article that is sold as it is — no accessory, no installation time. The shape of
 * trade goods that are simply handed over, e.g. a book.
 */
@Serializable
@CrossLayer("Stored unchanged with every priced article and handed unchanged to views and quote scripts")
@GermanTerm("Artikel-Konditionen ohne Zubehör und Montage")
data class BareArticleConditions(
  override val part: PriceWithProfit,
  override val unit: QuantityUnit? = null,
) : ArticleConditions {
  init {
    ArticleConditions.requireValidConditions(part, unit, installationTime = null)
  }
}

/**
 * Conditions of an article that may ship with accessory/small material but is not mounted by the
 * business — the accessory price applies per one [unit] of the part.
 */
@Serializable
@CrossLayer("Stored unchanged with every priced article and handed unchanged to views and quote scripts")
@GermanTerm("Artikel-Konditionen mit Zubehör")
data class ArticleConditionsWithAccessory(
  override val part: PriceWithProfit,
  override val unit: QuantityUnit? = null,
  /**
   * What the accessory/small material supplied with the article adds per [unit]. Carries no unit of
   * its own: the accessory belongs to one unit of the part, so a second, unrelated unit would leave
   * open what it refers to. `null` for an article of this type that ships without one.
   *
   * Stated without a default: a default would make the property optional on the wire, and an
   * optional property is one that every narrower shape satisfies — the generated TypeScript would
   * stop telling an accessory-carrying article type apart from one without.
   */
  @GermanTerm("Zubehör")
  val accessory: PriceWithProfit?,
) : ArticleConditions {
  init {
    ArticleConditions.requireValidConditions(part, unit, installationTime = null)
  }
}

/**
 * Conditions of an article that may be mounted but ships without accessory — the installation time
 * applies per one [unit] of the part.
 */
@Serializable
@CrossLayer("Stored unchanged with every priced article and handed unchanged to views and quote scripts")
@GermanTerm("Artikel-Konditionen mit Montagezeit")
data class ArticleConditionsWithInstallationTime(
  override val part: PriceWithProfit,
  override val unit: QuantityUnit? = null,
  /**
   * Estimated person-hours for the installation. Together with a [PricePerHour] this yields the
   * labour cost. `null` for an article of this type that is not mounted. Stated without a default,
   * for the reason [ArticleConditionsWithAccessory.accessory] gives.
   */
  @GermanTerm("Montagezeit")
  val installationTime: Duration?,
) : ArticleConditions {
  init {
    ArticleConditions.requireValidConditions(part, unit, installationTime)
  }
}

/**
 * Conditions of an article that carries every commercial part — accessory and installation time,
 * both per one [unit] of the part. The shape of installed trade goods, e.g. a cable.
 */
@Serializable
@CrossLayer("Stored unchanged with every priced article and handed unchanged to views and quote scripts")
@GermanTerm("Artikel-Konditionen vollständig")
data class FullArticleConditions(
  override val part: PriceWithProfit,
  override val unit: QuantityUnit? = null,
  /** What the accessory adds per [unit]; `null` for an article of this type that ships without one. */
  @GermanTerm("Zubehör")
  val accessory: PriceWithProfit?,
  /** Estimated person-hours for the installation; `null` for an article of this type that is not mounted. */
  @GermanTerm("Montagezeit")
  val installationTime: Duration?,
) : ArticleConditions {
  init {
    ArticleConditions.requireValidConditions(part, unit, installationTime)
  }
}
