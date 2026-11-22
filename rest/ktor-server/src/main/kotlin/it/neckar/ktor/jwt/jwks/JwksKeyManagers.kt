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

import it.neckar.ktor.jwt.KeycloakConfiguration
import it.neckar.rest.jwt.JwtAudience
import java.nio.file.Path

/**
 * The environment variable naming the directory the last successful fetch is written to. Deployments
 * set it to `/var/lib/neckar/jwks`, under the state directory the base image creates and the compose
 * file mounts a named volume on, so the confirmed key set survives both a restart and a re-created
 * container.
 *
 * State, not a cache — nothing in it expires, and losing it sends the service back to the seed no
 * fetch ever confirmed. That is why it is not named after one.
 *
 * Where it is unset — every local run, every test — nothing is persisted and a restarted service
 * falls back to the seed. Deployment-only rather than a default path, so a local run writes nothing
 * outside its own directory and logs no warning about a directory it may not write to.
 */
const val jwksStateDirectoryEnvironmentVariable: String = "JWKS_STATE_DIR"

/**
 * The [JwksKeyManager] for this realm: fetching from its JWKS endpoint, seeded from the file built
 * into the image when [ClasspathJwksSeed.forIssuer] names one, persisting to [jwksStateDirectoryEnvironmentVariable] where that is set. A token naming the client or one of
 * [additionalAudiences] with an unknown `kid` triggers a refresh.
 */
fun KeycloakConfiguration.jwksKeyManager(
  additionalAudiences: Set<JwtAudience> = emptySet(),
  persistence: JwksPersistence = defaultJwksPersistence(realmName),
): JwksKeyManager = JwksKeyManager(
  realmName = realmName,
  source = HttpJwksSource(jwksUrl),
  prefilter = JwksRefreshPrefilter(issuer = issuer, audiences = additionalAudiences + clientId),
  persistence = persistence,
  seed = ClasspathJwksSeed.forIssuer(issuer, realmName),
)

/**
 * [FileJwksPersistence] under the directory [jwksStateDirectoryEnvironmentVariable] names, or
 * [JwksPersistence.None] when it names none.
 */
fun defaultJwksPersistence(realmName: String): JwksPersistence {
  val directory = System.getenv(jwksStateDirectoryEnvironmentVariable)?.takeIf { it.isNotBlank() }
    ?: return JwksPersistence.None
  return FileJwksPersistence(Path.of(directory, "$realmName.json"))
}
