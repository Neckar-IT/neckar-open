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
package it.neckar.financial.payment.sepa

import it.neckar.open.annotations.GermanTerm
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * Value class for a SEPA direct-debit mandate reference.
 *
 * Validates the SEPA mandate-reference constraints at construction time: non-blank,
 * at most [MaxLength] characters, restricted to the SEPA-allowed character set
 * (letters, digits, space, and `'+?/-:().,'`).
 */
@GermanTerm("Mandatsreferenz")
@Serializable
@JvmInline
value class MandateReference(val value: String) {

  init {
    require(value.isNotBlank()) { "MandateReference must not be blank" }
    require(value.length <= MaxLength) { "MandateReference must be at most $MaxLength characters, was ${value.length}" }
    require(Pattern.matches(value)) { "MandateReference contains characters outside the SEPA-allowed set, was <$value>" }
  }

  override fun toString(): String = value

  companion object {
    const val MaxLength: Int = 35

    /** SEPA-allowed character set for mandate references. */
    val Pattern: Regex = Regex("^[A-Za-z0-9 '+?/\\-:().,]+$")
  }
}
