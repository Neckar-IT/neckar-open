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
package it.neckar.financial.quote.query

import it.neckar.commons.tags.Tag
import it.neckar.financial.priced.PricedElement
import it.neckar.financial.priced.PricedElements
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility

/**
 * Decides which elements of a quote are *shown*.
 *
 * The counterpart is [CalculationRelevanceQuery], which decides which elements are *summed up*.
 * Both are plain [Query]s with the same `matches` contract; they are separate types only so that
 * the two roles cannot be swapped at a call site - a quote is regularly rendered with one query
 * and calculated with another (see `PricedElements.dump`).
 */
interface VisibilityQuery : Query {
  /**
   * Creates a new query that connects both queries using "and"
   */
  operator fun plus(otherQuery: VisibilityQuery): VisibilityQuery {
    return AndVisibilityQuery(listOf(this, otherQuery))
  }


  companion object {
    /**
     * Returns the visible and mandatory items.
     */
    val allVisibleOnlyMandatory: VisibilityQuery = object : VisibilityQuery {
      override fun matches(pricedElement: PricedElement): Boolean {
        return onlyMandatory.matches(pricedElement) && onlyPublic.matches(pricedElement)
      }
    }

    /**
     * Filters all visible elements - including optional elements.
     * This query should typically be used to decide which elements are shown
     */
    val onlyPublic: VisibilityQuery = object : VisibilityQuery {
      override fun matches(pricedElement: PricedElement): Boolean {
        return pricedElement.visibility == Visibility.Public
      }
    }

    /**
     * Shows everything - including the [Visibility.Internal] elements that exist only to be
     * aggregated into a parent. Meant for internal views such as `PricedElements.dump`,
     * never for what the customer gets to see.
     */
    val allIncludingOptional: VisibilityQuery = object : VisibilityQuery {
      override fun matches(pricedElement: PricedElement): Boolean {
        return true
      }
    }

    val onlyOptional: VisibilityQuery = object : VisibilityQuery {
      override fun matches(pricedElement: PricedElement): Boolean {
        return pricedElement.optionality == Optionality.Optional
      }
    }

    /**
     * Hides the optional elements - the quote reads as if nothing optional had been offered.
     * Note that this ignores [Visibility]: internal elements pass too, so it is normally
     * combined with [onlyPublic] (that combination is [allVisibleOnlyMandatory]).
     */
    val onlyMandatory: VisibilityQuery = object : VisibilityQuery {
      override fun matches(pricedElement: PricedElement): Boolean {
        return (pricedElement.optionality == Optionality.Mandatory)
      }
    }

    /**
     * Skips every element whose [PricedElement.netPrice] is zero - for a section that is the
     * sum over its children, so an all-zero section disappears along with its items.
     */
    val skipZero: VisibilityQuery = object : VisibilityQuery {
      override fun matches(pricedElement: PricedElement): Boolean {
        return pricedElement.netPrice.isZero().not()
      }
    }

    /**
     * Shows only public entries and drops those priced at 0.00 €.
     * This is the default of `PricedElements.toFlatLines()`, the rendering path for the customer's quote.
     */
    val onlyPublicSkipZero: VisibilityQuery = object : VisibilityQuery {
      override fun matches(pricedElement: PricedElement): Boolean {
        return onlyPublic.matches(pricedElement) && skipZero.matches(pricedElement)
      }
    }

    /**
     * The default query for visualization for the consumer
     */
    val default: VisibilityQuery = onlyPublic


    fun onlyMandatoryWithoutTag(tag: Tag): VisibilityQuery {
      return object : VisibilityQuery {
        override fun matches(pricedElement: PricedElement): Boolean {
          return onlyMandatory.matches(pricedElement) && (pricedElement.relevanceOrNull()?.contains(tag)?.not() == true)
        }
      }
    }

  }
}

/**
 * Returns true if all the delegates returns true.
 * Returns false if any of the delegates returns false.
 */
data class AndVisibilityQuery(override val delegates: List<VisibilityQuery>) : AndQuery, VisibilityQuery

/**
 * Returns true if all the delegates return false.
 * Returns false if at least one of the delegates returns true
 */
data class NoneVisibilityQuery(override val delegates: List<VisibilityQuery>) : NoneQuery, VisibilityQuery
