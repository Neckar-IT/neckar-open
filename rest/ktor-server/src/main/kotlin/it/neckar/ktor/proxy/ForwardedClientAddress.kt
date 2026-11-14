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
package it.neckar.ktor.proxy

import io.ktor.http.*
import io.ktor.server.request.*
import kotlin.jvm.JvmInline

/**
 * The client address of a request behind Traefik. Traefik appends to whatever the caller sent, so only the last
 * `X-Forwarded-For` entry is ours; Ktor's `XForwardedHeaders` reports the first, which one header can then forge.
 */
@JvmInline
value class ForwardedClientAddress(val value: String) {
  override fun toString(): String = value

  companion object {
    /**
     * The last entry of the last `X-Forwarded-For` line in [headers], or [socketAddress] without one.
     * The last line, because a caller may send its own and the proxy appends to only one.
     */
    fun of(headers: Headers, socketAddress: String): ForwardedClientAddress {
      val appendedByProxy: String? = headers.getAll(HttpHeaders.XForwardedFor)?.lastOrNull()
        ?.split(',')?.map { it.trim() }?.lastOrNull { it.isNotEmpty() }
      return ForwardedClientAddress(appendedByProxy ?: socketAddress)
    }
  }
}

/**
 * `local.remoteAddress`, never `remoteHost`: the latter does a blocking reverse lookup on the event loop.
 */
fun ApplicationRequest.forwardedClientAddress(): ForwardedClientAddress = ForwardedClientAddress.of(headers, local.remoteAddress)
