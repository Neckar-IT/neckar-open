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

/**
 * The JWKS document of one realm as checked into the repository and built into the image.
 *
 * A seed holds public signature keys only — it is the JWKS endpoint's own answer, written by the
 * nightly mirror job, never anything read from the Keycloak admin API.
 *
 * Its purpose is the first boot of a deployment that has no persisted set yet and cannot reach the
 * realm. A service serving from it reports `degraded`, because nothing in this deployment has
 * confirmed those keys against the realm.
 */
fun interface JwksSeed {
  /**
   * The seed document, or `null` when the image carries none for this realm.
   */
  fun read(): JwksDocument?
}

/**
 * [JwksSeed] reading `jwks/<realm>.json` off the classpath, where the Gradle build puts the file
 * checked into the repository.
 */
class ClasspathJwksSeed(
  /**
   * The realm the seed belongs to, e.g. `internal` or `customers`.
   */
  val realmName: String,
  private val classLoader: ClassLoader = ClasspathJwksSeed::class.java.classLoader,
) : JwksSeed {
  val resourcePath: String
    get() = "jwks/$realmName.json"

  override fun read(): JwksDocument? {
    val raw = classLoader.getResourceAsStream(resourcePath)?.use { it.readBytes().decodeToString() }
      ?: return null
    return JwksDocument.parse(raw)
  }

  override fun toString(): String = "ClasspathJwksSeed($resourcePath)"

  companion object {
    /**
     * The Keycloak whose keys `tools/jwks-seed/mirror.sh` writes into the seed files. The slash ends the host, so a longer host
     * name never matches.
     */
    private const val MirroredKeycloakUrlPrefix: String = "https://auth.neckar.it/"

    /**
     * The seed of [realmName] for a service that verifies tokens of [issuer], or `null` when [issuer] belongs to another
     * Keycloak: two Keycloaks sign a realm of one name with different keys.
     */
    fun forIssuer(issuer: String, realmName: String): ClasspathJwksSeed? {
      return if (issuer.startsWith(MirroredKeycloakUrlPrefix)) ClasspathJwksSeed(realmName) else null
    }
  }
}
