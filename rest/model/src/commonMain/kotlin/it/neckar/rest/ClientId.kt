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
@file:UseSerializers(UuidSerializer::class)

package it.neckar.rest

import it.neckar.uuid.NanoId
import it.neckar.uuid.UuidSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * Represents a client id - used to identify the client
 */
@Serializable
data class ClientId(val value: NanoId) {
  override fun toString(): String {
    return value.toString()
  }

  companion object {
    /**
     * The default generator for client ids
     */
    val generator: NanoId.NanoIdGenerator = NanoId.generator(10, NanoId.AlphaNumeric)

    /**
     * Creates a random client id
     */
    fun random(): ClientId {
      return ClientId(generator.generate())
    }
  }
}
