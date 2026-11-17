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

import it.neckar.logging.Logger
import it.neckar.logging.LoggerDelegate
import java.text.FieldPosition
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

/**
 * formats integers to/from hex string
 */
class HexNumberFormat : NumberFormat() {
  override fun format(number: Double, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
    throw IllegalArgumentException("doubles not supported.")
  }

  override fun format(number: Long, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
    return toAppendTo.append("0x").append(String.format(Locale.getDefault(), "%08X", number))
  }

  override fun parse(source: String, parsePosition: ParsePosition): Number {
    if (source.startsWith("0x")) {
      try {
        return source.substring(2).toLong(16)
      } catch (e: NumberFormatException) {
        logger.warn("Could not parse hex [$source] due to $e")
      }
      return 0
    }

    try {
      return source.toLong(16)
    } catch (e: NumberFormatException) {
      logger.warn("Could not parse hex [$source] due to $e")
    }
    return 0
  }

  companion object {
    private val logger: Logger by LoggerDelegate()
  }
}
