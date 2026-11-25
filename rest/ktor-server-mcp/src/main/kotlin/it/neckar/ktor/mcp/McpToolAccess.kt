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

import it.neckar.open.authorization.Capability
import it.neckar.open.authorization.CoreCapability

/**
 * What a tool does with the data of the caller's tenant: the scope an MCP client's access token must carry and the capability the user must hold.
 */
data class McpToolAccess(
  val kind: Kind,
  val scope: OAuthScope,
  val capability: Capability,
) {
  enum class Kind {
    /**
     * Reads data; clients call the tool without asking (`readOnlyHint: true`).
     */
    Read,

    /**
     * Adds a record and changes no other; clients ask before the call (`readOnlyHint: false`, `destructiveHint: false`).
     */
    Create,

    /**
     * Overwrites fields of a record or ends a workflow step; clients ask before the call (`readOnlyHint: false`, `destructiveHint: true`).
     */
    Change,
  }
}

/**
 * The [McpToolAccess] values of one product: reads need [readScope] and [CoreCapability.ReadData], writes need [writeScope] and [CoreCapability.WriteData].
 */
class McpToolAccesses(
  readScope: OAuthScope,
  writeScope: OAuthScope,
) {
  val read: McpToolAccess = McpToolAccess(McpToolAccess.Kind.Read, readScope, CoreCapability.ReadData)

  val create: McpToolAccess = McpToolAccess(McpToolAccess.Kind.Create, writeScope, CoreCapability.WriteData)

  val change: McpToolAccess = McpToolAccess(McpToolAccess.Kind.Change, writeScope, CoreCapability.WriteData)

  /**
   * The scopes a client requests: announced as `scopes_supported` and in the `401` challenge.
   */
  val scopes: List<OAuthScope> = listOf(readScope, writeScope)
}
