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
package it.neckar.financial.priced

import it.neckar.commons.tags.Tag
import it.neckar.commons.tags.Tags
import it.neckar.commons.tags.toTagsOrEmpty
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.PriceWithProfitAndWorkingTime
import it.neckar.financial.currency.VatRate
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.AmountUnit
import it.neckar.financial.quote.ConfiguredOptionality
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.open.unit.other.pct
import it.neckar.uuid.randomUuid4
import kotlin.time.DurationUnit
import kotlin.uuid.Uuid

@PricedDsl
class PricedSectionBuilder(
  var headline: String,
  var visibility: Visibility = Visibility.Public,
  var amount: Amount? = null,
  var factor: @pct Double? = null,
) : PricedElementBuilder {
  var uuid: Uuid = randomUuid4()

  val children: MutableList<PricedElementBuilder> = mutableListOf()

  var details: String? = null

  fun add(pricedElementBuilder: PricedElementBuilder) {
    this.children.add(pricedElementBuilder)
  }

  @PricedDsl
  operator fun invoke(configuration: PricedSectionBuilder.() -> Unit) {
    this.apply(configuration)
  }

  override fun build(): PricedSection {
    return PricedSection(
      uuid = uuid,
      headline = headline,
      amount = amount,
      factor = factor,
      visibility = visibility,
      details = details,
      children = children.map { it.build() },
    )
  }


  @PricedDsl
  fun item(
    headline: String,
    /**
     * Price for only the material
     */
    priceForOneElement: PriceWithProfit,

    amount: Double = 1.0,
    /**
     * An optional factor to multiply the price with - see [PricedItem.factor].
     */
    factor: @pct Double = 1.0,

    relevance: Tags,

    visibility: Visibility,

    vatRate: VatRate,
    config: PricedItemBuilder.() -> Unit = {},
  ): PricedItemBuilder {
    return pricedItem(headline, priceForOneElement, amount, factor, relevance, visibility, Optionality.Mandatory, vatRate, config)
      .also {
        add(it)
      }
  }

  @PricedDsl
  fun item(headline: String, priceForOneElement: PriceWithProfit, amount: Double = 1.0, factor: @pct Double = 1.0, relevance: Tag, visibility: Visibility, vatRate: VatRate, config: PricedItemBuilder.() -> Unit = {}): PricedItemBuilder {
    return item(headline, priceForOneElement, amount, factor, relevance.toTagsOrEmpty(), visibility, vatRate, config)
  }

  @PricedDsl
  fun optionalItem(
    configuredOptionality: ConfiguredOptionality,

    headline: String,
    /**
     * Price for only the material
     */
    priceForOneElement: PriceWithProfit,

    amount: Double = 1.0,

    factor: @pct Double = 1.0,

    relevance: Tags,

    visibility: Visibility,

    vatRate: VatRate,
    config: PricedItemBuilder.() -> Unit = {},
  ): PricedItemBuilder? {

    if (configuredOptionality == ConfiguredOptionality.NotSelected) {
      return null
    }

    return pricedItem(
      headline = headline,
      priceForOneElement = priceForOneElement,
      amount = amount,
      factor = factor,
      relevance = relevance,
      visibility = visibility,
      optionality = configuredOptionality.toOptionality(),
      vatRate = vatRate,
      config = config,
    ).also {
      add(it)
    }
  }

  @PricedDsl
  fun optionalItem(
    configuredOptionality: ConfiguredOptionality,
    headline: String,
    priceForOneElement: PriceWithProfit,
    amount: Double = 1.0,
    factor: @pct Double = 1.0,
    relevance: Tag,
    visibility: Visibility,
    vatRate: VatRate,
    config: PricedItemBuilder.() -> Unit = {},
  ): PricedItemBuilder? {
    return optionalItem(configuredOptionality, headline, priceForOneElement, amount, factor, relevance.toTagsOrEmpty(), visibility, vatRate, config)
  }


  @PricedDsl
  fun itemWithWork(
    headline: String,
    priceForMaterialAndWork: PriceWithProfitAndWorkingTime,
    amount: Int = 1,
    factor: @pct Double = 1.0,
    relevance: Tags,
    visibility: Visibility,
    vatRate: VatRate,
    config: PricedSectionBuilder.() -> Unit = {},
  ) {
    itemWithWork(
      headline = headline,
      priceForMaterialAndWork = priceForMaterialAndWork,
      amount = amount,
      factor = factor,
      materialRelevance = relevance,
      workRelevance = relevance,
      visibility = visibility,
      materialVatRate = vatRate,
      workVatRate = vatRate,
      config = config,
    )
  }

  @IgnorableReturnValue
  @PricedDsl
  fun itemWithWork(
    headline: String,
    priceForMaterialAndWork: PriceWithProfitAndWorkingTime,
    amount: Int = 1,
    factor: @pct Double = 1.0,
    materialRelevance: Tags,
    workRelevance: Tags,
    visibility: Visibility,
    materialVatRate: VatRate,
    workVatRate: VatRate,
    materialConfig: PricedItemBuilder.() -> Unit = {},
    workConfig: PricedItemBuilder.() -> Unit = {},
    config: PricedSectionBuilder.() -> Unit = {},
  ): PricedSectionBuilder {
    return pricedSectionBuilder(
      headline = headline,
      amount = Amount(amount),
      factor = factor,

      visibility = visibility,
      config = config,
    ).also { sectionBuilder ->
      add(sectionBuilder)

      sectionBuilder {
        item(
          headline = "$headline: Materialkosten",
          priceForOneElement = priceForMaterialAndWork.materialPrice,
          relevance = materialRelevance,
          visibility = Visibility.Internal,
          vatRate = materialVatRate,
        ) {
          this.amount = Amount(amount)
          this.factor = factor
          materialConfig(this)
        }

        item(
          headline = "$headline: Arbeitszeit",
          priceForOneElement = priceForMaterialAndWork.workingTimes.workingHourPrice.priceWithProfit,
          relevance = workRelevance,
          visibility = Visibility.Internal,
          vatRate = workVatRate,
        ) {
          val workingHour = Amount(priceForMaterialAndWork.workingTimes.workingTime.toDouble(DurationUnit.HOURS), AmountUnit.Hours)
          this.amount = workingHour * amount
          this.factor = factor
          workConfig(this)
        }
      }

    }
  }

  @PricedDsl
  fun itemWithWork(
    headline: String,
    priceForMaterialAndWork: PriceWithProfitAndWorkingTime,
    amount: Double,
    materialRelevance: Tags,
    workRelevance: Tags,
    visibility: Visibility,
    materialVatRate: VatRate,
    workVatRate: VatRate,
    materialConfig: PricedItemBuilder.() -> Unit = {},
    workConfig: PricedItemBuilder.() -> Unit = {},
    config: PricedSectionBuilder.() -> Unit = {},
  ): PricedSectionBuilder {
    return pricedSectionBuilder(
      headline = headline,
      visibility = visibility,
      amount = Amount(amount),
      config = config,
    ).also { sectionBuilder ->
      add(sectionBuilder)

      sectionBuilder {
        item(
          headline = "$headline: Materialkosten",
          priceForOneElement = priceForMaterialAndWork.materialPrice,
          relevance = materialRelevance,
          visibility = Visibility.Internal,
          vatRate = materialVatRate,
        ) {
          this.amount = Amount(amount)
          materialConfig(this)
        }

        priceForMaterialAndWork.workingTimes.let { timesWithPrice ->
          item(
            headline = "$headline: Arbeitszeit",
            priceForOneElement = timesWithPrice.workingHourPrice.priceWithProfit,
            relevance = workRelevance,
            visibility = Visibility.Internal,
            vatRate = workVatRate,
          ) {
            this.amount = Amount(timesWithPrice.workingTime.toDouble(DurationUnit.HOURS) * amount, AmountUnit.Hours)
            workConfig(this)
          }
        }
      }

    }
  }

  @PricedDsl
  fun itemWithWork(
    headline: String,
    priceForMaterialAndWork: PriceWithProfitAndWorkingTime,
    amount: Int = 1,
    factor: @pct Double = 1.0,
    materialRelevance: Tag,
    workRelevance: Tag,
    visibility: Visibility,
    materialVatRate: VatRate,
    workVatRate: VatRate,
    materialConfig: PricedItemBuilder.() -> Unit = {},
    workConfig: PricedItemBuilder.() -> Unit = {},
    config: PricedSectionBuilder.() -> Unit = {},
  ): PricedSectionBuilder {
    return itemWithWork(headline, priceForMaterialAndWork, amount, factor, materialRelevance.toTagsOrEmpty(), workRelevance.toTagsOrEmpty(), visibility, materialVatRate, workVatRate, materialConfig, workConfig, config)
  }

  @PricedDsl
  fun itemWithWork(
    headline: String,
    priceForMaterialAndWork: PriceWithProfitAndWorkingTime,
    amount: Double,
    materialRelevance: Tag,
    workRelevance: Tag,
    visibility: Visibility,
    materialVatRate: VatRate,
    workVatRate: VatRate,
    materialConfig: PricedItemBuilder.() -> Unit = {},
    workConfig: PricedItemBuilder.() -> Unit = {},
    config: PricedSectionBuilder.() -> Unit = {},
  ): PricedSectionBuilder {
    return itemWithWork(headline, priceForMaterialAndWork, amount, materialRelevance.toTagsOrEmpty(), workRelevance.toTagsOrEmpty(), visibility, materialVatRate, workVatRate, materialConfig, workConfig, config)
  }

  @PricedDsl
  fun itemWithWork(
    headline: String,
    priceForMaterialAndWork: PriceWithProfitAndWorkingTime,
    amount: Int = 1,
    factor: @pct Double = 1.0,
    materialRelevance: Tag,
    workRelevance: Tags,
    visibility: Visibility,
    materialVatRate: VatRate,
    workVatRate: VatRate,
    materialConfig: PricedItemBuilder.() -> Unit = {},
    workConfig: PricedItemBuilder.() -> Unit = {},
    config: PricedSectionBuilder.() -> Unit = {},
  ): PricedSectionBuilder {
    return itemWithWork(headline, priceForMaterialAndWork, amount, factor, materialRelevance.toTagsOrEmpty(), workRelevance, visibility, materialVatRate, workVatRate, materialConfig, workConfig, config)
  }

  @PricedDsl
  fun itemWithWork(
    headline: String,
    priceForMaterialAndWork: PriceWithProfitAndWorkingTime,
    amount: Int = 1,
    factor: @pct Double = 1.0,
    materialRelevance: Tags,
    workRelevance: Tag,
    visibility: Visibility,
    materialVatRate: VatRate,
    workVatRate: VatRate,
    materialConfig: PricedItemBuilder.() -> Unit = {},
    workConfig: PricedItemBuilder.() -> Unit = {},
    config: PricedSectionBuilder.() -> Unit = {},
  ): PricedSectionBuilder {
    return itemWithWork(headline, priceForMaterialAndWork, amount, factor, materialRelevance, workRelevance.toTagsOrEmpty(), visibility, materialVatRate, workVatRate, materialConfig, workConfig, config)
  }

  @PricedDsl
  fun optionalItemWithWork(
    configuredOptionality: ConfiguredOptionality,
    headline: String,
    priceForMaterialAndWork: PriceWithProfitAndWorkingTime,
    amount: Int = 1,
    factor: @pct Double = 1.0,
    materialRelevance: Tags,
    workRelevance: Tags,
    visibility: Visibility,
    materialVatRate: VatRate,
    workVatRate: VatRate,
    config: PricedSectionBuilder.() -> Unit = {},
  ): PricedSectionBuilder {
    return pricedSectionBuilder(
      headline = headline,
      visibility = visibility,
      amount = Amount(amount),
      factor = factor,
      config = config,
    ).also { sectionBuilder ->
      add(sectionBuilder)

      sectionBuilder {
        optionalItem(
          configuredOptionality = configuredOptionality,
          headline = "$headline: Materialkosten",
          priceForOneElement = priceForMaterialAndWork.materialPrice,
          relevance = materialRelevance,
          visibility = Visibility.Internal,
          vatRate = materialVatRate,
        ) {
          this.amount = Amount(amount)
          this.factor = factor
        }

        optionalItem(
          configuredOptionality = configuredOptionality,
          headline = "$headline: Arbeitszeit",
          priceForOneElement = priceForMaterialAndWork.workingTimes.workingHourPrice.priceWithProfit,
          relevance = workRelevance,
          visibility = Visibility.Internal,
          vatRate = workVatRate,
        ) {
          val workingHour = Amount(priceForMaterialAndWork.workingTimes.workingTime.toDouble(DurationUnit.HOURS), AmountUnit.Hours)
          this.amount = workingHour * amount
          this.factor = factor
        }
      }
    }
  }

  @PricedDsl
  fun optionalItemWithWork(
    configuredOptionality: ConfiguredOptionality,
    headline: String,
    priceForMaterialAndWork: PriceWithProfitAndWorkingTime,
    amount: Int = 1,
    factor: @pct Double = 1.0,
    materialRelevance: Tag,
    workRelevance: Tag,
    visibility: Visibility,
    materialVatRate: VatRate,
    workVatRate: VatRate,
    config: PricedSectionBuilder.() -> Unit = {},
  ): PricedSectionBuilder {
    return optionalItemWithWork(configuredOptionality, headline, priceForMaterialAndWork, amount, factor, materialRelevance.toTagsOrEmpty(), workRelevance.toTagsOrEmpty(), visibility, materialVatRate, workVatRate, config)
  }

  @PricedDsl
  fun optionalItemWithWork(
    configuredOptionality: ConfiguredOptionality,
    headline: String,
    priceForMaterialAndWork: PriceWithProfitAndWorkingTime,
    amount: Int = 1,
    factor: @pct Double = 1.0,
    materialRelevance: Tag,
    workRelevance: Tags,
    visibility: Visibility,
    materialVatRate: VatRate,
    workVatRate: VatRate,
    config: PricedSectionBuilder.() -> Unit = {},
  ): PricedSectionBuilder {
    return optionalItemWithWork(configuredOptionality, headline, priceForMaterialAndWork, amount, factor, materialRelevance.toTagsOrEmpty(), workRelevance, visibility, materialVatRate, workVatRate, config)
  }

  @PricedDsl
  fun optionalItemWithWork(
    configuredOptionality: ConfiguredOptionality,
    headline: String,
    priceForMaterialAndWork: PriceWithProfitAndWorkingTime,
    amount: Int = 1,
    factor: @pct Double = 1.0,
    materialRelevance: Tags,
    workRelevance: Tag,
    visibility: Visibility,
    materialVatRate: VatRate,
    workVatRate: VatRate,
    config: PricedSectionBuilder.() -> Unit = {},
  ): PricedSectionBuilder {
    return optionalItemWithWork(configuredOptionality, headline, priceForMaterialAndWork, amount, factor, materialRelevance, workRelevance.toTagsOrEmpty(), visibility, materialVatRate, workVatRate, config)
  }


  @PricedDsl
  fun section(headline: String, visibility: Visibility, config: PricedSectionBuilder.() -> Unit): PricedSectionBuilder {
    return pricedSectionBuilder(headline = headline, visibility = visibility, config = config).also {
      add(it)
    }
  }

}

private fun ConfiguredOptionality.toOptionality(): Optionality {
  return when (this) {
    ConfiguredOptionality.Selected -> Optionality.Mandatory
    ConfiguredOptionality.Optional -> Optionality.Optional
    ConfiguredOptionality.NotSelected -> error("Can not convert $this to Optionality")
  }
}

fun pricedSectionBuilder(headline: String, visibility: Visibility? = null, amount: Amount? = null, factor: @pct Double? = null, config: PricedSectionBuilder.() -> Unit): PricedSectionBuilder {
  return PricedSectionBuilder(headline)
    .also { sectionBuilder ->
      visibility?.let { sectionBuilder.visibility = it }
      amount?.let { sectionBuilder.amount = it }
      factor?.let { sectionBuilder.factor = it }
    }
    .also(config)
}
