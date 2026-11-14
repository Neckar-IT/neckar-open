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
package it.neckar.csv

import it.neckar.open.kotlin.lang.decodeViaTextDecoder

/**
 * Parses a CSV file.
 *
 * kotlin-csv (used by the js/jvm actuals) publishes no wasmJs variant, therefore this
 * target ships a minimal RFC-4180-style parser with the same configuration as the other
 * targets: ';' as delimiter, '"' for quoting (doubled quote escapes inside quoted fields).
 */
actual suspend fun convertCsv(csvByteArray: ByteArray, charset: String): List<List<String>> {
  val csvString = csvByteArray.decodeViaTextDecoder(charset)
  return parseCsv(csvString, delimiter = ';')
}

private fun parseCsv(csv: String, delimiter: Char): List<List<String>> {
  val rows = mutableListOf<List<String>>()
  val currentRow = mutableListOf<String>()
  val currentField = StringBuilder()
  var insideQuotes = false
  var index = 0

  fun endField() {
    currentRow.add(currentField.toString())
    currentField.clear()
  }

  fun endRow() {
    endField()
    rows.add(currentRow.toList())
    currentRow.clear()
  }

  while (index < csv.length) {
    val char = csv[index]
    when {
      insideQuotes -> when {
        char == '"' && index + 1 < csv.length && csv[index + 1] == '"' -> {
          currentField.append('"')
          index++
        }

        char == '"' -> insideQuotes = false
        else -> currentField.append(char)
      }

      //Quoting only starts at the beginning of a field (RFC 4180) - a quote in the middle
      //of an unquoted field (e.g. an inch mark: 5" pipe) is literal content
      char == '"' && currentField.isEmpty() -> insideQuotes = true
      char == delimiter -> endField()
      char == '\r' && index + 1 < csv.length && csv[index + 1] == '\n' -> {
        endRow()
        index++
      }

      char == '\n' || char == '\r' -> endRow()
      else -> currentField.append(char)
    }
    index++
  }

  if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
    endRow()
  }

  return rows
}
