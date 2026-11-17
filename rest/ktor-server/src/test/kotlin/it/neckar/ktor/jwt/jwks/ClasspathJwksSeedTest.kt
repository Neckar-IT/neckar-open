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

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

/**
 * The seed resource is a contract between two places: `tools/jwks-seed/mirror.sh` writes the files,
 * this class reads them. Both realms are asserted by name, because a renamed file is a service that
 * cold-starts with no keys and no other symptom.
 */
class ClasspathJwksSeedTest {
  @Test
  fun `reads the seed the build put on the classpath, for every realm a service uses`() {
    listOf("internal", "customers").forEach { realm ->
      val document = ClasspathJwksSeed(realm).read()

      assertThat(document, "seed of realm '$realm'").isNotNull()
      assertThat(document!!.keyIds, "keys of realm '$realm'").isNotEmpty()
    }
  }

  @Test
  fun `the seed holds a key usable for signature verification`() {
    val signatureKeys = ClasspathJwksSeed("internal").read()!!.keys.values.filter { it.usage == "sig" }

    assertThat(signatureKeys.map { it.algorithm }).contains("RS256")
  }

  @Test
  fun `answers null for a realm the image carries no seed for`() {
    assertThat(ClasspathJwksSeed("a-realm-nobody-configured").read()).isNull()
  }

  @Test
  fun `derives the resource path from the realm name`() {
    assertThat(ClasspathJwksSeed("customers").resourcePath).isEqualTo("jwks/customers.json")
  }
}
