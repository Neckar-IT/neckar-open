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

import io.ktor.http.*
import io.ktor.util.*
import it.neckar.open.http.UrlParameterName
import it.neckar.open.http.UrlPathSegment
import it.neckar.open.http.UrlPathSegments
import it.neckar.open.kotlin.lang.toCamelCase
import it.neckar.open.text.Inflector
import it.neckar.rest.openapi.dsl.OperationId

/**
 * Creates unique operation ids for OpenAPI operations.
 */
object OperationIdGenerator {
  /**
   * Generates an operation id based on the given [pathSegments].
   */
  @Suppress("DEPRECATION")
  fun generateOperationId(
    method: HttpMethod,
    pathSegments: UrlPathSegments,
    /**
     * Condition that is used to skip the first segments of the path.
     */
    skipPrefixCondition: (pathSegment: String) -> Boolean = { it == "api" },
  ): OperationId {
    val pathStrings = mutableListOf<String>()

    val elements = pathSegments.elements.dropWhile { it is UrlPathSegment.Segment && skipPrefixCondition(it.value) }

    var index = 0
    while (index < elements.size) {
      val segment = elements[index]

      //last segment, add directly
      if (index == elements.size - 1) {
        pathStrings.add(segment.formatForOperationId())
        index++
        continue
      }

      //we have (at least) two segments left
      val nextSegment = elements[index + 1]

      //If the first segment is a path (with plural) and the next segment is a parameter, we can combine both to singular

      if (segment is UrlPathSegment.Segment && nextSegment is UrlParameterName) {
        val segmentName = segment.value
        if (segmentName.isPlural()) {
          pathStrings.add(segmentName.singular().toCamelCase())
          index += 2 //skip the next segment
          continue
        }
      }

      pathStrings.add(segment.formatForOperationId())
      index++
    }


    //if there are at least two segments, invert them
    pathStrings.reverse()

    //Merge them again
    val methodString = method.value.toLowerCasePreservingASCIIRules().capitalize()

    if (pathStrings.size > 1) {
      return OperationId(methodString + pathStrings.joinToString(separator = "For") { it.capitalize() })
    }

    return OperationId(methodString + pathStrings.joinToString { it.capitalize() })
  }
}

private fun UrlPathSegment.formatForOperationId(): String {
  return when (this) {
    is UrlPathSegment.Segment -> this.value.toCamelCase()
    is UrlParameterName -> this.value.toCamelCase() //just use the name of the parameter
  }
}

private fun String.singular(): String {
  return Inflector().singularize(this)
}

private fun String.isPlural(): Boolean {
  return this.endsWith("s") || this.endsWith("ies")
}
