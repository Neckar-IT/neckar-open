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
package it.neckar.rest.openapi.schema
import it.neckar.open.collections.fastForEach

/**
 * A queue that is used to process [SchemaDescriptor]s.
 */
internal class SchemaTypeProcessingQueue(initialTypes: Set<SchemaDescriptor> = emptySet()) {
  /**
   * A queue of [SchemaDescriptor]s that are pending to be processed.
   */
  private val pending = ArrayDeque(initialTypes)

  /**
   * A set of [SchemaDescriptor]s that have already been processed or added to the pending queue.
   */
  private val processedAndPending = mutableSetOf<SchemaDescriptor>()

  /**
   * Enqueues all [SchemaDescriptor]s from the given list - if they have not been processed yet.
   */
  fun enqueueAllIfNecessary(schemaTypes: List<SchemaDescriptor>) {
    //Reverse because in [enqueueIfNecessary] we add to the front of the queue.
    schemaTypes.reversed().fastForEach {
      enqueueIfNecessary(it)
    }
  }

  /**
   * Enqueues the given [SchemaDescriptor] if it has not been processed yet.
   *
   * If the [SchemaDescriptor] has already been processed or is already in the pending queue, it will not be added again.
   */
  fun enqueueIfNecessary(schemaType: SchemaDescriptor) {
    val added = processedAndPending.add(schemaType)
    if (added) {
      pending.addFirst(schemaType)
    }
  }

  /**
   * Returns true if there are still [SchemaDescriptor]s pending to be processed.
   */
  val hasNext: Boolean
    get() = pending.isNotEmpty()

  /**
   * Returns the next [SchemaDescriptor] to be processed.
   * Removes the [SchemaDescriptor] from the queue.
   */
  fun next(): SchemaDescriptor {
    return pending.removeFirst()
  }
}
