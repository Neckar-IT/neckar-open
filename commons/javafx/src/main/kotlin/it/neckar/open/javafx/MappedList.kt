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
package it.neckar.open.javafx

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.transformation.TransformationList
import java.util.function.Function
import java.util.stream.Collectors

/**
 * Maps elements of type IN to OUT with change listeners working as expected.
 */
class MappedList<IN, OUT>(
  source: ObservableList<out IN>,
  mapper: Function<IN, OUT>,
) : TransformationList<OUT, IN>(source) {
  private val mapper: Function<IN, OUT>
  private val mapped: ArrayList<OUT>

  /**
   * Creates a new MappedList list wrapped around the source list.
   * Each element will have the given function applied to it, such that the list is cast through the mapper.
   */
  init {
    this.mapper = mapper
    mapped = ArrayList(source.size)
    mapAll()
  }

  private fun mapAll() {
    mapped.clear()
    for (currentValue in source) {
      mapped.add(mapper.apply(currentValue))
    }
  }

  override fun sourceChanged(c: ListChangeListener.Change<out IN>) {
    // Is all this stuff right for every case? Probably it doesn't matter for this app.
    beginChange()
    while (c.next()) {
      if (c.wasPermutated()) {
        val perm = IntArray(c.to - c.from)
        for (i in c.from until c.to) {
          perm[i - c.from] = c.getPermutation(i)
        }
        nextPermutation(c.from, c.to, perm)
      } else if (c.wasUpdated()) {
        for (i in c.from until c.to) {
          remapIndex(i)
          nextUpdate(i)
        }
      } else {
        if (c.wasRemoved()) {
          // Removed should come first to properly handle replacements, then add.
          val removed = mapped.subList(c.from, c.from + c.removedSize)
          val duped: List<OUT> = ArrayList(removed)
          removed.clear()
          nextRemove(c.from, duped)
        }
        if (c.wasAdded()) {
          // Insert the mapped addedSubList exactly once at c.from. The previous version
          // looped over the inserted index range and re-inserted the whole sublist on
          // every iteration, so adding 3 items produced 9 mapped entries.
          mapped.addAll(c.from, c.addedSubList.stream().map(mapper).collect(Collectors.toList()))
          nextAdd(c.from, c.to)
        }
      }
    }
    endChange()
  }

  private fun remapIndex(i: Int) {
    if (i >= mapped.size) {
      for (j in mapped.size..i) {
        mapped.add(mapper.apply(source[j]))
      }
    }
    mapped[i] = mapper.apply(source[i])
  }

  override fun getSourceIndex(index: Int): Int {
    return index
  }

  /**
   * Do not @Override annotation since this method has only been added in Java 9
   */
  override fun getViewIndex(index: Int): Int {
    return index
  }

  override fun get(index: Int): OUT {
    return mapped[index]
  }

  override val size: Int
    get() {
      return mapped.size
    }
}
