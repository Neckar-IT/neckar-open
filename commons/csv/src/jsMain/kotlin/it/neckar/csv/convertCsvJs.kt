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

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.khronos.webgl.Uint8Array


actual suspend fun convertCsv(csvByteArray: ByteArray, charset: String): List<List<String>> {
  // ByteArray.toString() returns Object identity, not the decoded text — and the
  // [charset] parameter has to be honored explicitly on JS. Decode via TextDecoder
  // (browser/Node built-in), which supports UTF-8 and the encodings registered
  // in the Encoding Standard (Windows-1252, ISO-8859-15, …).
  val decoder = TextDecoder(charset)
  val csvString: String = decoder.decode(csvByteArray.toUint8Array())
  return csvReader {
    delimiter = ';'
  }.readAll(csvString)
}

private fun ByteArray.toUint8Array(): Uint8Array {
  val array = Uint8Array(size)
  for (i in indices) {
    array.asDynamic()[i] = this[i]
  }
  return array
}

private external class TextDecoder(label: String = definedExternally) {
  fun decode(input: Uint8Array): String
}


