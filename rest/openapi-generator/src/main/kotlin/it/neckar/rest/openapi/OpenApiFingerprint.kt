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
package it.neckar.rest.openapi

import it.neckar.open.kotlin.lang.Hex
import it.neckar.rest.version.ProtocolFingerprint
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.security.MessageDigest

/**
 * The fingerprint of [documentsByCategory], one document per category as Orval generates one client file from each:
 * it changes whenever the types, functions, example files, file layout or wire format of the generated clients change.
 * Left out: [IgnoredTopLevelKeys], [ClientDocumentation]; keys are sorted, so map order has no effect.
 */
fun openApiFingerprint(documentsByCategory: Map<OpenApiMetaData.Category, JsonObject>): ProtocolFingerprint {
  val clientRelevant = JsonObject(documentsByCategory.entries.associate { (category, document) -> category.value to document.clientRelevant() })
  val digest = MessageDigest.getInstance("SHA-256").digest(clientRelevant.canonical().toByteArray(Charsets.UTF_8))
  // Hex rather than base64: the value travels in a header and in log lines.
  return ProtocolFingerprint(ProtocolFingerprint.Prefix + Hex.encodeLower(digest))
}

private fun JsonObject.clientRelevant(): JsonElement {
  return ClientDocumentation.withoutDocumentation(JsonObject(filterKeys { it !in IgnoredTopLevelKeys }))
}

/**
 * The `info` block (title, version, contact, license) and the servers of the deployment; top level only.
 */
private val IgnoredTopLevelKeys: Set<String> = setOf("info", "servers")

/**
 * Leaves out the keys that change no generated code: prose, the `deprecated` flag and its reason, source references,
 * tags and their groups, the unserialized properties, and the security schemes with their deployment's OAuth2 URLs.
 * Of an example entry `summary`, `description` and `x-source-kotlin-type` fall away.
 */
private val ClientDocumentation: DocumentationKeyFilter = DocumentationKeyFilter(
  documentationKeys = setOf(
    "description",
    "summary",
    "deprecated",
    "tags",
    AdditionalOpenApiKeys.TagGroups,
    AdditionalOpenApiKeys.DisplayName,
    AdditionalOpenApiKeys.IgnoredProperties,
    AdditionalOpenApiKeys.DeprecatedReason,
    AdditionalOpenApiKeys.EnumDescriptions,
    AdditionalOpenApiKeys.EnumGermanTerms,
    AdditionalOpenApiKeys.SourceRef.KotlinType,
    AdditionalOpenApiKeys.SourceRef.SourceInfo,
    AdditionalOpenApiKeys.ValueClassMeta.GermanTerm,
    "securitySchemes",
  ),
  nameMapKeys = DocumentationKeyFilter.SchemaNameMapKeys + setOf(
    "paths",
    "schemas",
    "responses",
    "parameters",
    "requestBodies",
    "headers",
    "content",
    "examples",
  ),
  verbatimKeys = DocumentationKeyFilter.SchemaVerbatimKeys + "value",
)

/** Renders [this] with sorted keys, so equal interfaces render equally. */
private fun JsonElement.canonical(): String = when (this) {
  is JsonObject -> entries
    .sortedBy { (key, _) -> key }
    .joinToString(",", "{", "}") { (key, value) -> "$key:${value.canonical()}" }

  is JsonArray -> joinToString(",", "[", "]") { it.canonical() }
  is JsonPrimitive -> toString()
}
