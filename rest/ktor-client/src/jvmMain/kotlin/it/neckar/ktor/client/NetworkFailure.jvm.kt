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
package it.neckar.ktor.client

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

/**
 * The JVM engines throw the socket exception directly or wrap it, so the whole cause chain counts.
 * `ConnectException` includes Ktor's `ConnectTimeoutException`; a read timeout means the server was reached and does not count.
 * The CIO engine throws its own `FailToConnectException` for a custom DNS resolver without result; that case does not count.
 */
actual fun Throwable.isNetworkFailure(): Boolean {
  return generateSequence(this) { it.cause }.any { throwable ->
    throwable is ConnectException ||
      throwable is NoRouteToHostException ||
      throwable is UnknownHostException ||
      throwable is UnresolvedAddressException
  }
}
