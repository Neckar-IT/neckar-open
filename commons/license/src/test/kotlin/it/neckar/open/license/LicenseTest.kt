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
package it.neckar.open.license

import assertk.*
import assertk.assertions.*

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 *
 */
class LicenseTest {
  @Test
  fun testEquals() {
    assertEquals(License.ALL_RIGHTS_RESERVED, License.ALL_RIGHTS_RESERVED)
    assertFalse(License.PUBLIC_DOMAIN.equals(License.ALL_RIGHTS_RESERVED))
    assertFalse(License.PUBLIC_DOMAIN.equals("asdf"))
  }

  @Test
  fun testUrl() {
    assertNull(License.ALL_RIGHTS_RESERVED.url)
    assertNull(License.PUBLIC_DOMAIN.url)
    assertNull(License.UNKNOWN.url)
    assertNotNull(License.CDDL.url)
  }

  @Test
  fun testResolve() {
    assertEquals(20, License.LICENSES.size.toLong())
    assertSame(License.GPL_3, License[License.GPL_3.id])
    assertSame(License.CC_BY_NC_ND, License[License.CC_BY_NC_ND.id])
  }

  @Test
  fun `resolving an unknown id fails`() {
    assertFailure {
      License["does not exist"]
    }
      .isInstanceOf(IllegalArgumentException::class)
      .hasMessage("No license found for id <does not exist>")
  }

  @Test
  fun testToString() {
    assertThat(License.APACHE_20.toString()).isEqualTo("Apache License 2.0 (APACHE 2.0)")
  }
}
