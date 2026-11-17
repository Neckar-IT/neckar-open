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
package it.neckar.open.bson

import it.neckar.open.unit.number.MayBeNaN
import it.neckar.uuid.HasUuid
import org.bson.BsonArray
import org.bson.BsonBinary
import org.bson.BsonDateTime
import org.bson.BsonDocument
import org.bson.BsonValue
import org.bson.codecs.kotlinx.BsonConfiguration
import org.bson.conversions.Bson
import org.bson.json.JsonWriterSettings
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid


/**
 * Standard BSON configuration - as used for MongoDB
 */
val DefaultBsonConfiguration: BsonConfiguration = BsonConfiguration(
  explicitNulls = true, //this is different compared to the default configuration
  encodeDefaults = true,
  classDiscriminator = "_t"
)

/**
 * Converts a UUID to a BSON UUID.
 */
fun HasUuid.toBsonUuid(): BsonBinary {
  return uuid.toBsonUuid()
}

/**
 * Converts a Kotlin UUID to a BSON UUID (binary type) - not the object id type!
 */
fun Uuid.toBsonUuid(): BsonBinary {
  return BsonBinary(org.bson.BsonBinarySubType.UUID_STANDARD, toByteArray())
}

/**
 * Converts a BSON UUID to a Kotlin UUID.
 * Attention: There are different binary types in BSON. Throws an [org.bson.BsonInvalidOperationException] if the type is not a UUID.
 */
fun BsonBinary.asKotlinUuid(): Uuid {
  return asUuid().toKotlinUuid()
}

/**
 * Converts a bson document to a pretty-printed JSON-like string.
 */
fun Bson.toPrettyString(): String {
  //toBsonDocument() is the only platform type in this chain - pin it down here, toJson always returns a string
  val bsonDocument: BsonDocument = toBsonDocument()

  return bsonDocument.toJson(
    JsonWriterSettings.builder()
      .indent(true)
      .build()
  )
}

/**
 * Returns a double value or null if the value is null.
 */
fun BsonValue?.doubleOrNull(): Double? {
  if (this == null) {
    return null
  }

  if (this.isNull) {
    return null
  }

  if (this.isDouble) {
    return this.asDouble().value
  }

  throw IllegalArgumentException("Value is not a double but a ${this.bsonType}")
}

fun BsonValue?.doubleOrNaN(): @MayBeNaN Double {
  if (this == null) {
    return Double.NaN
  }

  if (this.isNull) {
    return Double.NaN
  }

  if (this.isDouble) {
    return this.asDouble().value
  }

  throw IllegalArgumentException("Value is not a double but a ${this.bsonType}")
}

/**
 * The text stored in this value, or null when the field is absent or holds BSON null.
 *
 * Throws when the value is of another type: a field that was supposed to hold text and holds a number
 * is a defect in the data, and returning null would hide it behind "no value here". Absence is an
 * answer, a wrong type is a bug — the same split [doubleOrNull] makes.
 */
fun BsonValue?.stringOrNull(): String? {
  if (this == null) return null
  if (isNull) return null

  require(isString) { "Value is not a string but a $bsonType" }

  return asString().value
}

/*
 * Field and path readers of a document: an absent field and BSON null read as null, any other type
 * throws naming the field or path, the expected and the found type, and the document's `_id` where it
 * has one - never a value.
 */

/** The text in [fieldName]. */
fun BsonDocument.stringOrNull(fieldName: String): String? {
  return fieldOrNull(fieldName, "STRING") { if (it.isString) it.asString().value else null }
}

/** The number in [fieldName] as a double, whichever numeric BSON type holds it. */
fun BsonDocument.numberOrNull(fieldName: String): Double? {
  return fieldOrNull(fieldName, "a number") { if (it.isNumber) it.asNumber().doubleValue() else null }
}

/** The sub-document in [fieldName]; with `explicitNulls` on, an unset embedded object is stored as BSON null. */
fun BsonDocument.documentOrNull(fieldName: String): BsonDocument? {
  return fieldOrNull(fieldName, "DOCUMENT") { if (it.isDocument) it.asDocument() else null }
}

/** The array in [fieldName]. */
fun BsonDocument.arrayOrNull(fieldName: String): BsonArray? {
  return fieldOrNull(fieldName, "ARRAY") { if (it.isArray) it.asArray() else null }
}

/** The binary in [fieldName]. */
fun BsonDocument.binaryOrNull(fieldName: String): BsonBinary? {
  return fieldOrNull(fieldName, "BINARY") { if (it.isBinary) it.asBinary() else null }
}

fun BsonDocument.dateTimeOrNull(fieldName: String): BsonDateTime? {
  return fieldOrNull(fieldName, "DATE_TIME") { if (it.isDateTime) it.asDateTime() else null }
}

/** The documents in the array [fieldName], empty when the field is absent or holds BSON null; an element of another type throws. */
fun BsonDocument.documentsOrEmpty(fieldName: String): List<BsonDocument> {
  val elements: List<BsonValue> = arrayOrNull(fieldName)?.values ?: return emptyList()
  return elements.mapIndexed { index, element ->
    element as? BsonDocument
      ?: throw IllegalArgumentException("<$fieldName[$index]> holds ${element.bsonType} instead of DOCUMENT${describeId()}")
  }
}

/** The value at the dotted [path], or null where a segment is absent or holds BSON null; a segment that is no document throws. */
fun BsonDocument.findAt(path: String): BsonValue? {
  var current: BsonValue = this
  val walked: MutableList<String> = mutableListOf()

  path.split('.').forEach { segment ->
    if (current.isNull) return null
    val parent: BsonDocument = current as? BsonDocument
      ?: throw IllegalArgumentException("<${walked.joinToString(".")}> holds ${current.bsonType} instead of DOCUMENT, so <$segment> cannot be reached${describeId()}")
    current = parent[segment] ?: return null
    walked += segment
  }

  return current.takeUnless { it.isNull }
}

/** The document at the dotted [path], null where a segment is absent or holds BSON null. */
fun BsonDocument.findDocumentAt(path: String): BsonDocument? {
  return valueAt(path, "DOCUMENT") { if (it.isDocument) it.asDocument() else null }
}

/** The text at the dotted [path], null where a segment is absent or holds BSON null. */
fun BsonDocument.findStringAt(path: String): String? {
  return valueAt(path, "STRING") { if (it.isString) it.asString().value else null }
}

/** The boolean at the dotted [path], null where a segment is absent or holds BSON null. */
fun BsonDocument.findBooleanAt(path: String): Boolean? {
  return valueAt(path, "BOOLEAN") { if (it.isBoolean) it.asBoolean().value else null }
}

/** The INT32 or INT64 at the dotted [path], null where a segment is absent or holds BSON null; a DOUBLE throws instead of rounding. */
fun BsonDocument.findIntegerAt(path: String): Long? {
  return valueAt(path, "INT32 or INT64") {
    when {
      it.isInt32 -> it.asInt32().value.toLong()
      it.isInt64 -> it.asInt64().value
      else -> null
    }
  }
}

/** [findAt] narrowed by [narrow]. */
private inline fun <T : Any> BsonDocument.valueAt(path: String, expected: String, narrow: (BsonValue) -> T?): T? {
  return narrowOrThrow(findAt(path), path, expected, narrow)
}

/** The value of [fieldName] narrowed by [narrow]. */
private inline fun <T : Any> BsonDocument.fieldOrNull(fieldName: String, expected: String, narrow: (BsonValue) -> T?): T? {
  return narrowOrThrow(get(fieldName), fieldName, expected, narrow)
}

/** [value] as [narrow] reads it: null for absent and BSON null, a throw naming [label] and [expected] where [narrow] answers null. */
private inline fun <T : Any> BsonDocument.narrowOrThrow(value: BsonValue?, label: String, expected: String, narrow: (BsonValue) -> T?): T? {
  if (value == null || value.isNull) return null

  return narrow(value) ?: throw IllegalArgumentException("<$label> holds ${value.bsonType} instead of $expected${describeId()}")
}

/** ` in document <id>` for a document with an `_id`, empty for a sub-document. */
private fun BsonDocument.describeId(): String {
  val id: BsonValue = this["_id"] ?: return ""
  return " in document <${id.toReadableString()}>"
}

/**
 * The value as it reads in a message — the value itself, not the BSON wrapper around it.
 *
 * `BsonBinary` holding a UUID prints as that UUID rather than base64, an `ObjectId` as its hex string.
 * Anything else falls back to `toString`. Meant for error messages that name a document a human then
 * has to find; not a serialization format.
 */
fun BsonValue?.toReadableString(): String {
  if (this == null) return "none"
  if (isObjectId) return asObjectId().value.toHexString()
  if (isString) return asString().value
  if (isBinary.not()) return toString()

  return runCatching { asBinary().asUuid().toString() }.getOrElse { toString() }
}
