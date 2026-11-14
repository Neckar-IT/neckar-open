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

import it.neckar.open.kotlin.lang.getValueForced
import kotlin.reflect.KProperty1

/**
 *
 */
object HasUuidTools {
  /**
   * Returns a list of all IDs used in the instance
   */
  fun extractIdsRecursivelyUsingReflection(instance: Any, context: Context = Context()): List<HasUuid> {
    context.markAsVisited(instance)

    return instance::class.members
      .filterIsInstance<KProperty1<Any, *>>()
      .flatMap { property ->
        return@flatMap when (val value = property.getValueForced(instance)) {
          is HasUuid -> listOf(value)
          is Collection<*> -> {
            value.flatMap {
              when (it) {
                is HasUuid -> listOf(it)
                null -> emptyList()
                else -> extractIdsRecursive(it, context)
              }
            }
          }

          else -> {
            if (value != null) {
              extractIdsRecursive(value, context)
            } else {
              emptyList()
            }
          }
        }
      }
  }

  private fun extractIdsRecursive(instanceToVisit: Any, context: Context): List<HasUuid> {
    if (context.alreadyVisited(instanceToVisit)) {
      return emptyList()
    }
    return extractIdsRecursivelyUsingReflection(instanceToVisit, context)
  }

  class Context() {
    private val visitedInstances: MutableSet<Any> = mutableSetOf()

    fun markAsVisited(instance: Any) {
      visitedInstances.add(instance)
    }

    fun alreadyVisited(instanceToVisit: Any): Boolean {
      return visitedInstances.contains(instanceToVisit)
    }
  }
}
