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
package it.neckar.rest.pagination

import it.neckar.open.annotations.TsExport
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents the size of a page
 */
@Serializable
@JvmInline
@TsExport
value class PageSize(
  /**
   * A page size of "-1" represents an unlimited page size
   */
  val value: Int,
) {

  init {
    // A page size of 0 would divide by zero in PaginationInfo.create; above Max one request loads an unbounded number of documents.
    require(value == UnlimitedAsInt || value in 1..Max) { "The page size must be $UnlimitedAsInt (unlimited) or between 1 and $Max but was $value" }
  }

  /**
   * Returns true if the page size is unlimited
   */
  fun isUnlimited(): Boolean {
    return value == UnlimitedAsInt
  }

  companion object {
    /**
     * The default page size
     */
    val Default: PageSize = PageSize(20)

    const val UnlimitedAsInt: Int = -1

    /**
     * The largest page size, also the bound for a page with [Unlimited].
     */
    const val Max: Int = 10_000

    /**
     * Represents an unlimited page size
     */
    val Unlimited: PageSize = PageSize(UnlimitedAsInt)
  }
}
