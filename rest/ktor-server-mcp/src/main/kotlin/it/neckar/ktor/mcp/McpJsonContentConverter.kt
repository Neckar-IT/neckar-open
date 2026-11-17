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

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCMessage
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder

/**
 * Serializes the JSON-RPC messages of the MCP SDK with [McpJson] and hands everything else to [serviceConverter].
 * The service's JSON writes explicit nulls, which the MCP schema of the clients rejects.
 */
class McpJsonContentConverter(private val serviceConverter: ContentConverter) : ContentConverter {
  override suspend fun serialize(contentType: ContentType, charset: Charset, typeInfo: TypeInfo, value: Any?): OutgoingContent? {
    val json: String = when {
      value is JSONRPCMessage -> McpJson.encodeToString<JSONRPCMessage>(value)
      value is List<*> && value.isNotEmpty() && value.all { it is JSONRPCMessage } -> McpJson.encodeToString<List<JSONRPCMessage>>(value.filterIsInstance<JSONRPCMessage>())
      else -> return serviceConverter.serialize(contentType, charset, typeInfo, value)
    }
    return TextContent(json, contentType.withCharsetIfNeeded(charset))
  }

  /**
   * The SDK reads its request body as text itself, so every body that reaches a converter belongs to the service.
   */
  override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any? {
    return serviceConverter.deserialize(charset, typeInfo, content)
  }
}

/**
 * Installs [ContentNegotiation] with the service's JSON wrapped in [McpJsonContentConverter]; Ktor refuses a second, route-scoped [ContentNegotiation] for the MCP route.
 */
fun Application.installJsonWithMcpMessages(configuration: JsonBuilder.() -> Unit) {
  install(ContentNegotiation) {
    register(ContentType.Application.Json, McpJsonContentConverter(KotlinxSerializationConverter(Json { configuration() })))
  }
}
