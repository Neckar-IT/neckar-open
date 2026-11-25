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

import io.ktor.server.application.ApplicationCall
import it.neckar.open.authorization.UserCapabilities
import it.neckar.open.tenancy.Tenant

/**
 * The signed-in user of an MCP request, resolved by the product from the access token.
 */
interface McpCaller {
  /**
   * Named in the log line of every tool call, e.g. `anna.berger@example.org`.
   */
  val loginName: String

  val tenantId: Tenant.Id

  val capabilities: UserCapabilities

  val scopes: McpGrantedScopes

  /**
   * The OAuth client that obtained the access token (`azp`), e.g. `patterns-mcp-claude`; `null` for a token without the claim.
   */
  val oAuthClient: String?
}

/**
 * The scopes that limit what a [McpCaller] may do through the MCP endpoint.
 */
sealed interface McpGrantedScopes {
  operator fun contains(scope: OAuthScope): Boolean

  /**
   * The access token of the service itself, issued to its own user interface: the user acts with every capability the user holds.
   */
  data object AllOfTheUser : McpGrantedScopes {
    override fun contains(scope: OAuthScope): Boolean = true
  }

  /**
   * An access token issued for the MCP endpoint: the user granted the client exactly [scopes].
   */
  data class Granted(val scopes: Set<OAuthScope>) : McpGrantedScopes {
    override fun contains(scope: OAuthScope): Boolean = scope in scopes

    companion object {
      /**
       * The scopes of the claim `scope`, separated by spaces (RFC 9068), e.g. `openid patterns:read`.
       */
      fun parse(scopeClaim: String): Granted {
        return Granted(scopeClaim.split(' ').filter { it.isNotBlank() }.map { OAuthScope(it) }.toSet())
      }
    }
  }
}

/**
 * Resolves the [McpCaller] of an authenticated MCP request; throws [McpCallerRefusedException] for a token that identifies no active user.
 */
fun interface McpCallerResolver<C : McpCaller> {
  suspend fun resolve(call: ApplicationCall): C
}

/**
 * Thrown when a tool refuses the caller: no matching user, a missing scope or capability.
 * [McpToolInvocation] answers it as a tool error with [message].
 */
class McpCallerRefusedException(override val message: String) : RuntimeException(message)
