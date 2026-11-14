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
package it.neckar.catalog

import it.neckar.domain.DomainIdentifier
import it.neckar.financial.currency.Money
import it.neckar.open.http.Url
import it.neckar.open.kotlin.serializers.NativeInstant
import kotlinx.serialization.Serializable

/**
 * This is a reference to a specific catalog item at a specific time.
 * This can be used to inline references to catalog items in other data structures (usually used for MongoEntities).
 *
 * Also contains the most important data of the catalog item - so that it can be displayed without loading the full catalog item.
 */
@Serializable
data class CatalogItemReference(
  val referencedId: CatalogItem.Id,
  /**
   * The time when this catalog item was referenced.
   */
  val referencedAt: NativeInstant,

  val domain: DomainIdentifier,

  /**
   * The type key of the catalog item
   */
  val typeKey: CatalogItemTypeKey,

  val title: String,

  /**
   * The current price at the time when the reference has been created.
   *
   * ATTENTION: This price might be outdated!
   */
  //TODO remove if possible(?)
  val price: Money.Net,

  /**
   * The icon for the catalog item
   */
  val icon: Url? = null,
) {
  init {
    require(title.isNotBlank()) { "title must not be blank" }
  }
}
