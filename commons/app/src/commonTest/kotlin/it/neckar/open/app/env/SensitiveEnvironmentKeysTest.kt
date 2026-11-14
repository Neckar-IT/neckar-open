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
package it.neckar.open.app.env

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SensitiveEnvironmentKeysTest {
  @Test
  fun `names a credential by its substring whatever the prefix`() {
    assertTrue(SensitiveEnvironmentKeys.isSensitive("GITLAB_ACCESS_TOKEN"))
    assertTrue(SensitiveEnvironmentKeys.isSensitive("KEYCLOAK_CLIENT_SECRET"))
    assertTrue(SensitiveEnvironmentKeys.isSensitive("MONGO_PASSWORD"))
    assertTrue(SensitiveEnvironmentKeys.isSensitive("CA_CERT_BASE64"))
    assertTrue(SensitiveEnvironmentKeys.isSensitive("SSH_PRIVATE_KEY"))
  }

  @Test
  fun `lower case counts as well because an env file is not required to shout`() {
    assertTrue(SensitiveEnvironmentKeys.isSensitive("gitlab_access_token"))
  }

  @Test
  fun `leaves an ordinary key alone`() {
    assertFalse(SensitiveEnvironmentKeys.isSensitive("GITLAB_SERVER_URL"))
    assertFalse(SensitiveEnvironmentKeys.isSensitive("CI_PROJECT_ID"))
  }

  @Test
  fun `masks the value of a credential and passes every other through`() {
    assertEquals("****", SensitiveEnvironmentKeys.maskIfSensitive("GITLAB_ACCESS_TOKEN", "glpat-Ky7mQ2vX8nR4tL9wZ3aB"))
    assertEquals("https://git.neckar.it", SensitiveEnvironmentKeys.maskIfSensitive("GITLAB_SERVER_URL", "https://git.neckar.it"))
  }

  @Test
  fun `masks a credential that carries no value at all so its absence stays unreadable`() {
    assertEquals("****", SensitiveEnvironmentKeys.maskIfSensitive("GITLAB_ACCESS_TOKEN", null))
  }
}
