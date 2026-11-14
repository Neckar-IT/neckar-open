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
package it.neckar.customer.company

import it.neckar.open.kotlin.lang.ValueClassString
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * A BIC as the user typed it — unvalidated.
 *
 * This is the wire type of every request field that carries a BIC the user may have gotten
 * wrong. [BIC] carries the invariant and therefore cannot represent bad input: a typed [BIC] on
 * the wire fails while the request body is being deserialized, which reaches the routing as a
 * technical error rather than as a validation error for the BIC field.
 *
 * The same boundary as [it.neckar.open.serialization.TrimmedStringOrNull] for free text: raw on
 * the wire, converted when it crosses into the domain.
 */
@JvmInline
@Serializable
value class BicInput(override val value: String) : ValueClassString {

  /**
   * Checks this input against the BIC format rule.
   */
  fun parse(): BicParseResult = BIC.parse(value)

  override fun toString(): String = value
}
