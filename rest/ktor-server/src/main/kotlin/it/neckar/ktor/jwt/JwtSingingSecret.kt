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

import java.io.File
import java.io.InputStream
import java.util.Properties

/**
 * Represents secret used to sign the JWT tokens
 */
object JwtSingingSecret {
  private const val jwsTokenHmac256Key = "jws.token.HMAC256.key"

  /**
   * Loads the secrets from the given input stream (e.g. from a file)
   */
  fun load(stream: InputStream): JwtSigningKey {
    val properties = Properties()
    properties.load(stream)

    val keyHS256 = requireNotNull(properties.getProperty(jwsTokenHmac256Key)) {
      "prop key [$jwsTokenHmac256Key] not found"
    }
    return JwtSigningKey.HMAC256(keyHS256)
  }

  /**
   * Loads the secrets from the given properties file. Use the key [jwsTokenHmac256Key] to specify the secret for HMAC256
   */
  fun load(file: File): JwtSigningKey {
    try {
      return file.inputStream().use {
        load(it)
      }
    } catch (e: Exception) {
      throw IllegalStateException("Could not load JWT signing secrets from file [${file.absolutePath}]", e)
    }
  }
}
