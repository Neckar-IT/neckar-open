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
package it.neckar.open.guava

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet

/**
 * Contains extension methods for guava collections
 */

/**
 * Creates a new copy including the additional element
 */
fun <T : Any> ImmutableList<T>.copyAndAdd(elementToAdd: T): ImmutableList<T> {
  return ImmutableList
    .builder<T>()
    .addAll(this)
    .add(elementToAdd)
    .build()
}

/**
 * Converts a list to an immutable list
 */
fun <T : Any> List<T>.toImmutable(): ImmutableList<T> {
  return ImmutableList.copyOf(this)
}

/**
 * Converts a list to an immutable set. Duplicates are dropped, the order of the first occurrences is kept.
 */
fun <T : Any> List<T>.toImmutableSet(): ImmutableSet<T> {
  return ImmutableSet.copyOf(this)
}

/**
 * Converts a set to an immutable set
 */
fun <T : Any> Set<T>.toImmutable(): ImmutableSet<T> {
  return ImmutableSet.copyOf(this)
}

/**
 * Converts a sequence to an immutable list
 */
fun <T : Any> Sequence<T>.toImmutable(): ImmutableList<T> {
  val builder = ImmutableList.builder<T>()

  for (item in this) {
    builder.add(item)
  }

  return builder.build()
}

/**
 * Converts a sequence to an immutable set. Duplicates are dropped, the order of the first occurrences is kept.
 */
fun <T : Any> Sequence<T>.toImmutableSet(): ImmutableSet<T> {
  val builder = ImmutableSet.builder<T>()

  for (item in this) {
    builder.add(item)
  }

  return builder.build()
}

/**
 * Converts to an immutable map
 */
fun <K : Any, V : Any> Map<K, V>.toImmutable(): ImmutableMap<K, V> {
  return ImmutableMap.copyOf(this)
}

/**
 * Converts a collection to an immutable list. For a [List] receiver [toImmutable] does the same.
 */
fun <T : Any> Collection<T>.toImmutableList(): ImmutableList<T> {
  return ImmutableList.copyOf(this)
}
