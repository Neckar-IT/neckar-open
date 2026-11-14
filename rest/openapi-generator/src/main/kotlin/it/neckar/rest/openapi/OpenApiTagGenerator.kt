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
package it.neckar.rest.openapi

import it.neckar.open.http.UrlPathSegment
import it.neckar.open.http.UrlPathSegments

/**
 * Derives an OpenAPI tag from a route's path segments.
 *
 * The tag is the first meaningful path segment after skipping common prefixes
 * (e.g. "api", service name). This groups endpoints in Scalar's sidebar navigation.
 *
 * Example: `/api/patterns/books/{bookId}` → tag `books`, display name `Books`
 */
object OpenApiTagGenerator {

  /**
   * Generates a tag name from the given path segments.
   *
   * Returns `null` if no meaningful segment can be identified (e.g. the path
   * consists only of prefix segments).
   */
  fun generateTag(
    pathSegments: UrlPathSegments,
    skipPrefixCondition: (pathSegment: String) -> Boolean = { it == "api" },
  ): OpenApiTag? {
    val firstSegment = pathSegments.elements
      .dropWhile { it is UrlPathSegment.Segment && skipPrefixCondition(it.value) }
      .firstOrNull { it is UrlPathSegment.Segment }
      as? UrlPathSegment.Segment
      ?: return null

    val tagName = firstSegment.value
    val displayName = tagName.toDisplayName()

    return OpenApiTag(name = tagName, displayName = displayName)
  }

  /**
   * Converts a kebab-case tag name to a human-readable display name.
   *
   * `books` → `Books`, `book-reviews` → `Book Reviews`, `pv-components` → `Pv Components`
   */
  private fun String.toDisplayName(): String {
    return split("-").joinToString(" ") { word ->
      word.replaceFirstChar { it.uppercase() }
    }
  }
}
