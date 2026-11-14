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

import it.neckar.financial.priced.PricedElement
import it.neckar.open.collections.fastForEach

/**
 * A predicate over the elements of a quote.
 *
 * Two independent hierarchies extend it, and keeping them apart is the point:
 * a [VisibilityQuery] decides what is *shown*, a [CalculationRelevanceQuery] what is *summed up*.
 * The same quote is rendered with one and calculated with the other, so neither is assignable to
 * the other. This supertype exists for the code that does not care which of the two it got:
 * the tree-walking methods on `PricedElement`, and [UnionQuery.delegates].
 */
sealed interface Query {
  fun matches(pricedElement: PricedElement): Boolean
}


sealed interface UnionQuery : Query {
  val delegates: List<Query>
}

sealed interface AndQuery : UnionQuery {
  override fun matches(pricedElement: PricedElement): Boolean {
    delegates.fastForEach { if (it.matches(pricedElement).not()) return false }
    return true
  }
}

sealed interface NoneQuery : UnionQuery {
  override fun matches(pricedElement: PricedElement): Boolean {
    delegates.fastForEach { if (it.matches(pricedElement)) return false }
    return true
  }
}
