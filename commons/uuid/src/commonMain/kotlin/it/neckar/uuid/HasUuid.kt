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
package it.neckar.uuid


import it.neckar.open.http.Url
import it.neckar.open.http.UrlPattern
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Describes an element that has a UUID.
 *
 * **Attention**:
 * * All implementations *must* implement equals/hashCode. And these methods *must* only use [uuid].
 * * All other properties must *not* be used to compare the objects.
 *
 * A UUID identifies a "logical" element. E.g., it describes the roof facing west on a given building.
 * It does *not* identify a "version" of a description object.
 *
 * Therefore, there may exist several *different* objects with the same UUID that describe the same object.
 * When, for example, the label of the roof is updated, the *same* UUID is used.
 *
 */
interface HasUuid {
  /**
   * The UUID for the element.
   */
  val uuid: Uuid
}

