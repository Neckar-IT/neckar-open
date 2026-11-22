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
import it.neckar.domain.HasDomainIdentifier
import it.neckar.open.http.Url
import it.neckar.uuid.HasValueClassUuid
import it.neckar.uuid.NativeUuid
import it.neckar.uuid.ValueClassUuid
import it.neckar.uuid.randomUuid4
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

/**
 * Base interface for catalog item definitions - without price information.
 *
 * Leaves the type of `id` to the implementer: an entity declares its own `<Entity>.Id`,
 * an interface several entities share, such as [PricedCatalogItem], narrows it to [CatalogItem.Id].
 */
interface CatalogItem : HasValueClassUuid, HasDomainIdentifier {
  /**
   * The type key of the catalog item
   */
  val typeKey: CatalogItemTypeKey

  /**
   * The domain identifier of the catalog item
   */
  override val domainIdentifier: DomainIdentifier

  /**
   * The title of the catalog item
   */
  val title: String

  /**
   * The description of the catalog item
   */
  val description: String?

  /**
   * The icon for the catalog item
   */
  val icon: Url?

  @Serializable
  @JvmInline
  value class Id(
    override val uuid: NativeUuid,
  ) : ValueClassUuid {

    override fun toString(): String {
      return uuid.toHexDashString()
    }

    companion object {
      /**
       * Creates a new random id
       */
      fun random(): Id = Id(randomUuid4())
    }
  }
}
