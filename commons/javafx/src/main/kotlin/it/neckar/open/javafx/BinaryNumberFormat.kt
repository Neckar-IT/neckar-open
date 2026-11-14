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
import it.neckar.open.kotlin.lang.fastFor
import java.text.FieldPosition
import java.text.NumberFormat
import java.text.ParsePosition

/**
 * Formats integers and long values to/from binary string
 */
class BinaryNumberFormat(private val minTextLength: Int) : NumberFormat() {
  override fun format(number: Double, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
    throw IllegalArgumentException("doubles not supported.")
  }

  override fun format(number: Long, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
    val stringBuffer = toAppendTo.append("0b")
    val binaryString = java.lang.Long.toBinaryString(number)
    val padCount = minTextLength - binaryString.length

    padCount.fastFor {
      stringBuffer.append('0')
    }

    stringBuffer.append(binaryString)
    return stringBuffer
  }

  override fun parse(source: String, parsePosition: ParsePosition): Number {
    if (source.startsWith("0b")) {
      try {
        return java.lang.Long.parseUnsignedLong(source.substring(2), 2)
      } catch (e: NumberFormatException) {
        logger.warn("Could not parse binary [$source] due to $e")
      }
    }
    try {
      return java.lang.Long.parseUnsignedLong(source, 2)
    } catch (e: NumberFormatException) {
      logger.warn("Could not parse binary [$source] due to $e")
    }
    return 0
  }

  companion object {
    private val logger: Logger by LoggerDelegate()
  }
}
