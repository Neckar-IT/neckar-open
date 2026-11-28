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

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * The arguments of this request; an argument outside the properties of [schema] throws, which the SDK answers as a tool error.
 * A tool without arguments calls it for the check alone.
 */
@IgnorableReturnValue
fun CallToolRequest.strictArguments(toolName: String, schema: ToolSchema): JsonObject {
  // A schema without properties declares a tool that takes no argument.
  val allowed: Set<String> = schema.properties?.keys ?: emptySet()
  val arguments: JsonObject = this.arguments ?: JsonObject(emptyMap())
  val unknownArguments = arguments.keys - allowed
  require(unknownArguments.isEmpty()) { "$toolName takes only $allowed but got $unknownArguments" }
  return arguments
}

/**
 * The argument [name] as text, or `null` without it; any other JSON value or blank text throws naming the found type.
 */
fun JsonObject.optionalNonBlankString(name: String): String? {
  val argument = this[name] ?: return null
  require(argument is JsonPrimitive && argument.isString && argument.content.isNotBlank()) { "$name must be a non-blank string, found ${argument.jsonTypeName()}" }
  return argument.content
}

/**
 * Reads [name] like [optionalNonBlankString]; a missing argument throws `<name> is required`.
 */
fun JsonObject.requiredNonBlankString(name: String): String {
  return optionalNonBlankString(name) ?: throw missingArgument(name)
}

/**
 * The argument [name] as a whole number within [range], or `null` without it; anything else throws naming the found type.
 */
fun JsonObject.optionalInt(name: String, range: IntRange): Int? {
  val argument = this[name] ?: return null
  return (argument as? JsonPrimitive)?.takeIf { it.isString.not() }?.intOrNull?.takeIf { it in range }
    ?: throw IllegalArgumentException("$name must be a whole number between ${range.first} and ${range.last}, found ${argument.numberKindOrTypeName()}")
}

/**
 * Reads [name] like [optionalInt]; a missing argument throws `<name> is required`.
 */
fun JsonObject.requiredInt(name: String, range: IntRange): Int {
  return optionalInt(name, range) ?: throw missingArgument(name)
}

private fun JsonElement.numberKindOrTypeName(): String {
  if (this !is JsonPrimitive || this is JsonNull || isString || booleanOrNull != null) return jsonTypeName()
  return if (intOrNull != null) "a whole number outside the range" else "a number with a fraction or beyond Int"
}

/**
 * The kind of this JSON value for an error message, which never repeats the value itself.
 */
fun JsonElement.jsonTypeName(): String = when (this) {
  is JsonObject -> "an object"
  is JsonArray -> "an array"
  JsonNull -> "null"
  is JsonPrimitive -> when {
    isString -> if (content.isBlank()) "a blank string" else "a string"
    booleanOrNull != null -> "a boolean"
    else -> "a number"
  }
}

/**
 * The argument [name] as `true` or `false`, or `null` without it; any other JSON value throws naming the found type.
 */
fun JsonObject.optionalBoolean(name: String): Boolean? {
  val argument = this[name] ?: return null
  return (argument as? JsonPrimitive)?.takeIf { it.isString.not() }?.booleanOrNull
    ?: throw IllegalArgumentException("$name must be true or false, found ${argument.jsonTypeName()}")
}

/**
 * Reads [name] like [optionalBoolean]; a missing argument throws `<name> is required`.
 */
fun JsonObject.requiredBoolean(name: String): Boolean {
  return optionalBoolean(name) ?: throw missingArgument(name)
}

/**
 * The argument [name] as one of [values] by its name, or `null` without it; another name throws listing [values], any other JSON value naming the found type.
 */
fun <E : Enum<E>> JsonObject.optionalEnum(name: String, values: List<E>, nameOf: (E) -> String = { it.name }): E? {
  val text = optionalNonBlankString(name) ?: return null
  return values.firstOrNull { nameOf(it) == text }
    ?: throw IllegalArgumentException("$name must be one of ${values.joinToString(", ") { nameOf(it) }}, found \"$text\"")
}

/**
 * Reads [name] like [optionalEnum]; a missing argument throws `<name> is required`.
 */
fun <E : Enum<E>> JsonObject.requiredEnum(name: String, values: List<E>, nameOf: (E) -> String = { it.name }): E {
  return optionalEnum(name, values, nameOf) ?: throw missingArgument(name)
}

private fun missingArgument(name: String): IllegalArgumentException = IllegalArgumentException("$name is required")
