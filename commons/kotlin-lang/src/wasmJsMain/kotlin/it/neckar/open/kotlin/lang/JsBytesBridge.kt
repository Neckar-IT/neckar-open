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
package it.neckar.open.kotlin.lang

import org.khronos.webgl.ArrayBuffer

/**
 * Bridges between Kotlin [ByteArray]s and JS binary data in a *single* interop call per direction.
 *
 * Per-element access on Int8Array/Uint8Array crosses the Wasm-JS boundary once per byte -
 * these helpers transport the whole payload as one Base64 string instead and do the
 * byte-level work on whichever side of the boundary already holds the data.
 */

/**
 * Copies the content of this JS [ArrayBuffer] into a Kotlin [ByteArray]
 */
fun ArrayBuffer.toByteArray(): ByteArray {
  return jsArrayBufferToBase64(this).fromBase64()
}

/**
 * Decodes this byte array to a string using the JS host `TextDecoder`.
 * Supports UTF-8 and the encodings registered in the Encoding Standard (Windows-1252, ISO-8859-15, ...).
 */
fun ByteArray.decodeViaTextDecoder(charset: String): String {
  return jsTextDecodeBase64(this.toBase64(), charset)
}

private fun jsArrayBufferToBase64(buffer: ArrayBuffer): String = js(
  """(function() {
    var bytes = new Uint8Array(buffer);
    var chunks = [];
    for (var i = 0; i < bytes.length; i += 0x8000) {
      chunks.push(String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000)));
    }
    return btoa(chunks.join(''));
  })()"""
)

private fun jsTextDecodeBase64(base64: String, charset: String): String = js(
  """(function() {
    var binary = atob(base64);
    var bytes = new Uint8Array(binary.length);
    for (var i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return new TextDecoder(charset).decode(bytes);
  })()"""
)
