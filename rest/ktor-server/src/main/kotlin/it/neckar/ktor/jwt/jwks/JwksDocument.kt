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
package it.neckar.ktor.jwt.jwks

import com.auth0.jwk.Jwk
import it.neckar.open.kotlin.serializers.jsonTypeName
import kotlin.jvm.JvmInline
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/**
 * The `kid` header of a JWT, and the identifier a key carries in a JWKS document.
 */
@JvmInline
value class JwkKeyId(val value: String) {
  init {
    require(value.isNotBlank()) { "key id must not be blank" }
  }

  override fun toString(): String = value
}

/**
 * One JWKS document, parsed into its keys and kept alongside the text it was parsed from.
 *
 * The [raw] text is what gets persisted and what a seed file holds, so a key set survives a restart
 * byte for byte instead of being re-serialized from the parsed form.
 *
 * A document is always whole. Nothing in this package ever holds or fetches a single key: that
 * property is what makes a refresh triggered by an attacker deliver the same keys as a regular one.
 */
class JwksDocument(
  /**
   * The JWKS document as it was fetched, read from disk, or read from the seed resource.
   */
  val raw: String,
  /**
   * The keys of the document, by their `kid`.
   */
  val keys: Map<JwkKeyId, Jwk>,
) {
  val keyIds: Set<JwkKeyId>
    get() = keys.keys

  operator fun get(keyId: JwkKeyId): Jwk? = keys[keyId]

  override fun toString(): String = "JwksDocument(${keyIds.joinToString()})"

  companion object {
    /**
     * Parses [raw] as a JWKS document.
     *
     * A key whose `kid` is missing or blank is dropped: it can never be selected by a token's `kid`
     * header, and one such entry must not cost the service every other key in the document.
     *
     * Throws for anything that says the document is not a JWKS at all — no `keys` array, an entry
     * that is not a key, a `kid` that is no string, or no key with a usable `kid`. Fail closed: the
     * caller keeps the set it holds, whereas adopting a half-read document would turn every request
     * into a `401`. A message names the position and the JSON type, never the key material.
     */
    fun parse(raw: String): JwksDocument {
      val keyElements: JsonArray = when (val declared: JsonElement? = Json.parseToJsonElement(raw).jsonObject["keys"]) {
        null -> throw IllegalArgumentException("not a JWKS document: no 'keys' array")
        is JsonArray -> declared
        else -> throw IllegalArgumentException("not a JWKS document: 'keys' holds ${declared.jsonTypeName()} instead of an array")
      }

      val keys = keyElements.mapIndexedNotNull { index, element ->
        val key: JsonObject = element as? JsonObject
          ?: throw IllegalArgumentException("not a JWKS document: 'keys[$index]' holds ${element.jsonTypeName()} instead of an object")
        val kid: JsonElement = key["kid"]?.takeUnless { it is JsonNull } ?: return@mapIndexedNotNull null
        val keyId: String = (kid as? JsonPrimitive)?.takeIf { it.isString }?.content
          ?: throw IllegalArgumentException("not a JWKS document: 'keys[$index].kid' holds ${kid.jsonTypeName()} instead of a string")
        if (keyId.isBlank()) return@mapIndexedNotNull null
        JwkKeyId(keyId) to Jwk.fromValues(key.toJavaValues())
      }.toMap()

      require(keys.isNotEmpty()) { "JWKS document holds no key with a 'kid'" }
      return JwksDocument(raw, keys)
    }

    /**
     * `Jwk.fromValues` takes the untyped map shape of the Jackson parser this project does not use,
     * so the kotlinx tree is converted to it: strings, numbers and booleans unwrapped, arrays and
     * objects recursed into.
     */
    private fun JsonObject.toJavaValues(): Map<String, Any> =
      mapNotNull { (key, value) -> value.toJavaValue()?.let { key to it } }.toMap()

    private fun JsonElement.toJavaValue(): Any? = when (this) {
      is JsonNull -> null
      is JsonPrimitive -> if (isString) content else booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
      is JsonArray -> map { it.toJavaValue() }
      is JsonObject -> toJavaValues()
    }
  }
}
