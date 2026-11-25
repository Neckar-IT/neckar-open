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
package it.neckar.ktor.mcp

import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Declares the text argument [name] in a tool's `inputSchema`.
 */
fun JsonObjectBuilder.stringProperty(name: String, description: String) {
  putJsonObject(name) {
    put("type", "string")
    put("description", description)
  }
}

/**
 * Declares the text argument [name] that takes one of [values].
 */
fun JsonObjectBuilder.enumProperty(name: String, description: String, values: List<String>) {
  putJsonObject(name) {
    put("type", "string")
    putJsonArray("enum") { values.forEach { add(JsonPrimitive(it)) } }
    put("description", description)
  }
}

/**
 * Declares the whole-number argument [name] within [range].
 */
fun JsonObjectBuilder.integerProperty(name: String, description: String, range: IntRange) {
  putJsonObject(name) {
    put("type", "integer")
    put("minimum", range.first)
    put("maximum", range.last)
    put("description", description)
  }
}

/**
 * Declares the argument [name] that takes `true` or `false`.
 */
fun JsonObjectBuilder.booleanProperty(name: String, description: String) {
  putJsonObject(name) {
    put("type", "boolean")
    put("description", description)
  }
}
