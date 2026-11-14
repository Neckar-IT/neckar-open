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

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Instant

/**
 * The last successful fetch, read back from where it was persisted.
 */
data class PersistedJwks(
  val document: JwksDocument,
  /**
   * When the fetch that wrote this document happened.
   */
  val fetchedAt: Instant,
)

/**
 * Keeps the last successful fetch across a restart of the process.
 *
 * Without it a restart throws the confirmed key set away and the service falls back to the seed —
 * and a restart is exactly what happens when a container is unhealthy for any other reason.
 */
interface JwksPersistence {
  /**
   * The persisted document, or `null` when none was written yet.
   *
   * Throws when something was written that no longer parses. The caller logs and carries on with the
   * seed — swallowing it here would hide a corrupted file behind a service that looks freshly
   * installed.
   */
  fun read(): PersistedJwks?

  /**
   * Persists [document]. Failures are the caller's to log — a service that cannot write its key set
   * still serves with it.
   */
  fun write(document: JwksDocument)

  /**
   * Persists nothing and reads nothing. For tests, and for a service whose deployment has no volume
   * to write to; it then falls back to the seed after every restart.
   */
  object None : JwksPersistence {
    override fun read(): PersistedJwks? = null
    override fun write(document: JwksDocument) {}
  }
}

/**
 * [JwksPersistence] over one file.
 *
 * The file holds the raw JWKS document and nothing else — the fetch time is its modification time,
 * so there is no second format to keep in step with the first. Written through a temporary file in
 * the same directory and moved into place, so a process killed mid-write leaves the previous
 * document rather than half of the new one.
 *
 * On a volume the document survives re-creating the container; on the container filesystem only a
 * restart.
 */
class FileJwksPersistence(
  private val file: Path,
) : JwksPersistence {

  override fun read(): PersistedJwks? {
    if (file.exists().not()) {
      return null
    }

    return PersistedJwks(
      document = JwksDocument.parse(file.readText()),
      fetchedAt = Instant.fromEpochMilliseconds(file.getLastModifiedTime().toMillis()),
    )
  }

  override fun write(document: JwksDocument) {
    file.parent?.createDirectories()

    val temporary = file.resolveSibling("${file.fileName}.tmp")
    try {
      temporary.writeText(document.raw)
      Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
    } finally {
      temporary.deleteIfExists()
    }
  }

  override fun toString(): String = "FileJwksPersistence($file)"
}
