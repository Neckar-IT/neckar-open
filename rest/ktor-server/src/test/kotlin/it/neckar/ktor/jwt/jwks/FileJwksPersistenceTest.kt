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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText

class FileJwksPersistenceTest {
  private val key = JwksTestKey.generate("persisted-key")

  @Test
  fun `reads back nothing before anything was written`(@TempDir directory: Path) {
    assertThat(FileJwksPersistence(directory.resolve("internal.json")).read()).isNull()
  }

  @Test
  fun `reads back the document it wrote, byte for byte`(@TempDir directory: Path) {
    val persistence = FileJwksPersistence(directory.resolve("internal.json"))
    val document = jwksDocumentOf(key)

    persistence.write(document)

    assertThat(persistence.read()?.document?.raw).isEqualTo(document.raw)
    assertThat(persistence.read()?.document?.keyIds).isEqualTo(setOf(key.keyId))
  }

  @Test
  fun `creates the directory it writes into`(@TempDir directory: Path) {
    val persistence = FileJwksPersistence(directory.resolve("cache").resolve("internal.json"))

    persistence.write(jwksDocumentOf(key))

    assertThat(persistence.read()?.document?.keyIds).isEqualTo(setOf(key.keyId))
  }

  @Test
  fun `replaces the document it wrote before`(@TempDir directory: Path) {
    val persistence = FileJwksPersistence(directory.resolve("internal.json"))
    val rotated = JwksTestKey.generate("rotated-key")

    persistence.write(jwksDocumentOf(key))
    persistence.write(jwksDocumentOf(rotated))

    assertThat(persistence.read()?.document?.keyIds).isEqualTo(setOf(rotated.keyId))
  }

  @Test
  fun `propagates when what was written no longer parses`(@TempDir directory: Path) {
    val file = directory.resolve("internal.json")
    file.writeText("this was a JWKS document once")

    assertFailure { FileJwksPersistence(file).read() }.isInstanceOf(SerializationException::class)
  }

  @Test
  fun `leaves no temporary file behind`(@TempDir directory: Path) {
    FileJwksPersistence(directory.resolve("internal.json")).write(jwksDocumentOf(key))

    assertThat(directory.listDirectoryEntries().map { it.fileName.toString() }).isEqualTo(listOf("internal.json"))
  }
}
