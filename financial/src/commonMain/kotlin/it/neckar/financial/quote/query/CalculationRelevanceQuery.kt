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
import it.neckar.commons.tags.Tags
import it.neckar.financial.priced.PricedElement
import it.neckar.financial.priced.PricedElements
import it.neckar.financial.quote.Optionality

/**
 * Decides which elements of a quote are *summed up*.
 *
 * The counterpart is [VisibilityQuery], which decides which elements are *shown*. Both are plain
 * [Query]s with the same `matches` contract; they are separate types only so that the two roles
 * cannot be swapped at a call site - a quote is regularly rendered with one query and calculated
 * with another (see `PricedElements.dump`).
 *
 * Beyond optionality this also selects by [Tag] (see [QueryByCalculationRelevanceCategory]), which
 * is how partial sums such as "material only" are built.
 */
interface CalculationRelevanceQuery : Query {
  /**
   * Creates a new query that connects both queries using "and"
   */
  operator fun plus(otherQuery: CalculationRelevanceQuery): CalculationRelevanceQuery {
    return AndCalculationRelevanceQuery(listOf(this, otherQuery))
  }

  companion object {
    /**
     * Sums up everything, optional elements included. Not the sum the customer owes - that is
     * [onlyMandatory]. Use it for the "with all options" figure that is shown next to the real
     * total (`FlatQuoteLineDetails.totalIncludingOptional`).
     */
    val allIncludingOptional: CalculationRelevanceQuery = object : CalculationRelevanceQuery {
      override fun matches(pricedElement: PricedElement): Boolean {
        return true
      }
    }

    val onlyOptional: CalculationRelevanceQuery = object : CalculationRelevanceQuery {
      override fun matches(pricedElement: PricedElement): Boolean {
        return pricedElement.optionality == Optionality.Optional
      }
    }

    /**
     * Only mandatory items are returned (not optional).
     *
     * This is the query to sum with: the total the customer owes contains no optional element.
     */
    val onlyMandatory: CalculationRelevanceQuery = object : CalculationRelevanceQuery {
      override fun matches(pricedElement: PricedElement): Boolean {
        return (pricedElement.optionality == Optionality.Mandatory)
      }
    }

    /**
     * The default query for calculations (only contains the mandatory items) - not the optional!
     */
    val default: CalculationRelevanceQuery = onlyMandatory


    fun onlyMandatoryWithoutTag(tag: Tag): CalculationRelevanceQuery {
      return object : CalculationRelevanceQuery {
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
data class AndCalculationRelevanceQuery(override val delegates: List<CalculationRelevanceQuery>) : AndQuery, CalculationRelevanceQuery

/**
 * Returns true if all the delegates return false.
 * Returns false if at least one of the delegates returns true
 */
data class NoneCalculationRelevanceQuery(override val delegates: List<CalculationRelevanceQuery>) : NoneQuery, CalculationRelevanceQuery


/**
 * Returns a new query for this category
 */
fun Tag.asQuery(): CalculationRelevanceQuery {
  return QueryByCalculationRelevanceCategory(this)
}

class QueryByCalculationRelevanceCategory(val tag: Tag) : CalculationRelevanceQuery {
  override fun matches(pricedElement: PricedElement): Boolean {
    return pricedElement.relevanceOrNull()?.contains(tag) == true
  }
}

fun Tags.asQuery(): CalculationRelevanceQuery {
  return QueryByCalculationRelevanceCategories(this)
}

/**
 * Matches the items carrying *all* of [tags].
 *
 * An empty [tags] is not "no filter" but its own category: it matches exactly the items that carry
 * no tag at all. Without that branch `all {}` on an empty list would be true for everything, which
 * would turn an untagged partial sum into the full sum.
 *
 * Sections never match either way - [PricedElement.relevanceOrNull] is null for them.
 */
class QueryByCalculationRelevanceCategories(val tags: Tags) : CalculationRelevanceQuery {
  override fun matches(pricedElement: PricedElement): Boolean {
    if (tags.isEmpty()) return pricedElement.relevanceOrNull()?.isEmpty() == true
    return tags.tags.all {
      pricedElement.relevanceOrNull()?.contains(it) == true
    }
  }
}
