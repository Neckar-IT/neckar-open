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
@file:OptIn(ExperimentalSerializationApi::class)

package it.neckar.rest.openapi.schema
import it.neckar.uuid.UuidSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KClassifier

/**
 * Represents an OpenAPI schema type that is *not* a reference - a native/primitive type with an
 * optional format (as defined by the OpenAPI "Data Types" table).
 */
data class NativeSchemaType(
  val type: String,
  val format: String? = null,
) {

  companion object {
    /**
     * Creates a native schema type for a string with optional format
     */
    fun string(format: String? = null): NativeSchemaType {
      return NativeSchemaType("string", format)
    }

    val String: NativeSchemaType = string()

    /**
     * Represents a UUID (not officially supported)
     */
    val Uuid: NativeSchemaType = string("uuid")

    /**
     * A URI as defined by RFC 3986
     */
    val Uri: NativeSchemaType = string("uri")

    /**
     * the date-time notation as defined by RFC 3339, section 5.6, for example, 2017-07-21T17:32:28Z
     */
    val DateTime: NativeSchemaType = string("date-time")

    /**
     * full-date notation as defined by RFC 3339, section 5.6, for example, 2017-07-21
     */
    val Date: NativeSchemaType = string("date")
    val Integer: NativeSchemaType = NativeSchemaType("integer")


    val Number: NativeSchemaType = NativeSchemaType("number")
    val Boolean: NativeSchemaType = NativeSchemaType("boolean")

    /**
     * binary data, used to describe files
     */
    val Binary: NativeSchemaType = string("binary")

    /**
     * base64-encoded characters, for example, U3dhZ2dlciByb2Nrcw==
     */
    val Byte: NativeSchemaType = string("byte")


    /*
     * Below types as defined by OAS in https://swagger.io/specification/ "Data Types"
     */

    /**
     * Signed 32 bits
     */
    val Int32: NativeSchemaType = NativeSchemaType("integer", "int32")
    val Int64: NativeSchemaType = NativeSchemaType("integer", "int64")
    val Float: NativeSchemaType = NativeSchemaType("number", "float")
    val Double: NativeSchemaType = NativeSchemaType("number", "double")

    /**
     * a hint to UIs to mask the input
     */
    val Password: NativeSchemaType = string("password")
  }
}

/**
 * Maps a primitive [SerialDescriptor] to its [NativeSchemaType], recognizing string sub-formats
 * (date-time, date, uuid, uri) from the serial name.
 */
fun SerialDescriptor.primitiveToOpenApiType(): NativeSchemaType {
  //If this is a string, check for annotations that hint at a date time format
  if (this.kind == PrimitiveKind.STRING) {
    val serialName = this.serialName
    if (serialName.contains("DateTime")) {
      return NativeSchemaType.DateTime
    }
    if (serialName == "kotlin.time.Instant") {
      return NativeSchemaType.DateTime
    }
    if (serialName == "kotlinx.datetime.Instant") {
      return NativeSchemaType.DateTime
    }
    if (serialName.contains("Date")) {
      return NativeSchemaType.Date
    }
    if (serialName == "kotlin.uuid.Uuid") {
      return NativeSchemaType.Uuid
    }

    if (serialName == UuidSerializer.SerialName) {
      return NativeSchemaType.Uuid
    }

    val baseSerialName = serialName.removeSuffix("?")
    if (baseSerialName == "Url" || baseSerialName.startsWith("Url.")) {
      return NativeSchemaType.Uri
    }
  }

  return kind.primitiveToOpenApiType()
}

/**
 * Maps a primitive [SerialKind] to its [NativeSchemaType].
 */
private fun SerialKind.primitiveToOpenApiType(): NativeSchemaType {
  return when (this) {
    PrimitiveKind.BOOLEAN -> NativeSchemaType.Boolean
    PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT -> NativeSchemaType.Integer
    PrimitiveKind.LONG -> NativeSchemaType.Int64
    PrimitiveKind.DOUBLE -> NativeSchemaType.Double
    PrimitiveKind.FLOAT -> NativeSchemaType.Float
    PrimitiveKind.STRING, PrimitiveKind.CHAR -> NativeSchemaType.String
    else -> throw UnsupportedOperationException("Unknown primitive kind $this")
  }
}

/**
 * Maps a Kotlin [KClassifier] to its native OpenAPI type, or `null` if it is not a native type.
 */
fun KClassifier?.toOpenApiNativeType(): NativeSchemaType? {
  return when (this) {
    String::class, Char::class -> NativeSchemaType.String
    Int::class, Short::class, Byte::class -> NativeSchemaType.Integer
    Long::class -> NativeSchemaType.Int64
    Double::class -> NativeSchemaType.Double
    Float::class -> NativeSchemaType.Float
    Boolean::class -> NativeSchemaType.Boolean
    ByteArray::class -> NativeSchemaType.Binary
    else -> null
  }
}

/**
 * Wraps this native type in a nullable representation (`["<type>", "null"]`) if required by the
 * schema constraints, otherwise returns the plain `"<type>"`.
 */
internal fun NativeSchemaType.wrapNullable(schemaConstraints: SchemaPropertyConstraints): JsonElement {
  return if (schemaConstraints.showNullInType) {
    JsonArray(
      listOf(
        JsonPrimitive(this.type),
        JsonPrimitive("null") //must be a string!
      )
    )
  } else {
    JsonPrimitive(this.type)
  }
}
