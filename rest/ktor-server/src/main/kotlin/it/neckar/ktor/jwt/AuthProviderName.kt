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
package it.neckar.ktor.jwt

import it.neckar.projects.common.ServiceName

/**
 * The name a Ktor authentication provider is registered under.
 *
 * The same name links the provider installation (`install(Authentication) { jwt(name) { … } }`)
 * to every route that selects it (`authenticate(name)`). Wrapping it in a value class makes that
 * link an explicit type instead of a bare string, and keeps the raw [value] at the Ktor API
 * boundary (`jwt(...)`, `authenticate(...)`), which is the only place the primitive is needed.
 */
@JvmInline
value class AuthProviderName(val value: String) {
  init {
    require(value.isNotBlank()) { "auth provider name must not be blank" }
  }

  companion object {
    /**
     * The name for a service's access-token provider. Prefixed with [serviceName] so provider
     * names showing up in Ktor error messages and logs identify the service they belong to.
     */
    fun accessTokenFor(serviceName: ServiceName): AuthProviderName {
      return AuthProviderName("${serviceName.value}.accessToken")
    }

    /**
     * The name for the provider of a service's MCP endpoint, whose tokens name the endpoint in `aud`. See [accessTokenFor].
     */
    fun mcpResourceTokenFor(serviceName: ServiceName): AuthProviderName {
      return AuthProviderName("${serviceName.value}.mcpResourceToken")
    }

    /**
     * The name for a service's refresh-token provider. See [accessTokenFor].
     */
    fun refreshTokenFor(serviceName: ServiceName): AuthProviderName {
      return AuthProviderName("${serviceName.value}.refreshToken")
    }
  }
}
