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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 *
 */
class CreativeCommonsLicenseTest {
  @Test
  fun testUrls() {
    assertNotNull(License.CC_BY.url)
    assertNotNull(License.CC_BY_NC.url)
    assertNotNull(License.CC_BY_NC_ND.url)
    assertNotNull(License.CC_BY_NC_SA.url)
    assertNotNull(License.CC_BY_ND.url)
    assertNotNull(License.CC_BY_SA.url)

    //Ensure different urls
    val urls: MutableSet<String> = HashSet()
    for (license in License.CC_LICENSES) {
      assertNotNull(license)
      val url = license.url
      if (url != null) {
        urls.add(url.toString())
      }
    }
    assertEquals(6, urls.size.toLong())
  }

  @Test
  fun testLicenseUrlTranslated() {
    assertNotNull(License.CC_BY.getUrl(Locale.GERMAN))
    assertEquals("http://creativecommons.org/licenses/by/3.0/de", License.CC_BY.getUrl(Locale.GERMAN).toString())
    assertEquals("http://creativecommons.org/licenses/by-nc-nd/3.0/de", License.CC_BY_NC_ND.getUrl(Locale.GERMAN).toString())
  }

  @Test
  fun testAll() {
    assertEquals(6, License.CC_LICENSES.size.toLong())
  }

  @Test
  fun testCCBy() {
    assertFalse(License.CC_BY.isRestrictedToNonCommercial)
    assertTrue(License.CC_BY.isDerivedWorkAllowed)
    assertTrue(License.CC_BY.isSharedAlikeDerivedWorkAllowed)
    assertTrue(License.CC_BY.isUsableCommercially)
    assertEquals(CreativeCommonsLicense.ModificationsAllowed.YES, License.CC_BY.modificationsAllowed)
  }

  @Test
  fun testCCSA() {
    assertFalse(License.CC_BY_SA.isRestrictedToNonCommercial)
    assertFalse(License.CC_BY_SA.isDerivedWorkAllowed)
    assertTrue(License.CC_BY_SA.isSharedAlikeDerivedWorkAllowed)
    assertTrue(License.CC_BY_SA.isUsableCommercially)
    assertEquals(CreativeCommonsLicense.ModificationsAllowed.SHARE_ALIKE, License.CC_BY_SA.modificationsAllowed)
  }

  @Test
  fun testCCND() {
    assertFalse(License.CC_BY_ND.isRestrictedToNonCommercial)
    assertFalse(License.CC_BY_ND.isDerivedWorkAllowed)
    assertFalse(License.CC_BY_ND.isSharedAlikeDerivedWorkAllowed)
    assertTrue(License.CC_BY_ND.isUsableCommercially)
    assertEquals(CreativeCommonsLicense.ModificationsAllowed.NO, License.CC_BY_ND.modificationsAllowed)
  }

  @Test
  fun testCCNC() {
    assertTrue(License.CC_BY_NC.isRestrictedToNonCommercial)
    assertTrue(License.CC_BY_NC.isDerivedWorkAllowed)
    assertTrue(License.CC_BY_NC.isSharedAlikeDerivedWorkAllowed)
    assertFalse(License.CC_BY_NC.isUsableCommercially)
    assertEquals(CreativeCommonsLicense.ModificationsAllowed.YES, License.CC_BY_NC.modificationsAllowed)
  }

  @Test
  fun testCCNCSA() {
    assertTrue(License.CC_BY_NC_SA.isRestrictedToNonCommercial)
    assertFalse(License.CC_BY_NC_SA.isDerivedWorkAllowed)
    assertTrue(License.CC_BY_NC_SA.isSharedAlikeDerivedWorkAllowed)
    assertFalse(License.CC_BY_NC_SA.isUsableCommercially)
    assertEquals(CreativeCommonsLicense.ModificationsAllowed.SHARE_ALIKE, License.CC_BY_NC_SA.modificationsAllowed)
  }

  @Test
  fun testCCNCND() {
    assertTrue(License.CC_BY_NC_ND.isRestrictedToNonCommercial)
    assertFalse(License.CC_BY_NC_ND.isDerivedWorkAllowed)
    assertFalse(License.CC_BY_NC_ND.isSharedAlikeDerivedWorkAllowed)
    assertFalse(License.CC_BY_NC_ND.isUsableCommercially)
    assertEquals(CreativeCommonsLicense.ModificationsAllowed.NO, License.CC_BY_NC_ND.modificationsAllowed)
  }

  @Test
  fun testResolve() {
    assertEquals(20, License.LICENSES.size.toLong())
    assertEquals(6, License.CC_LICENSES.size.toLong())
    Assertions.assertSame(License.GPL_3, License[License.GPL_3.id])
    Assertions.assertSame(License.CC_BY_NC_ND, CreativeCommonsLicense[License.CC_BY_NC_ND.id])
  }

  @Test
  fun `resolving is limited to the creative commons licenses`() {
    assertFailure {
      CreativeCommonsLicense[License.GPL_3.id]
    }
      .isInstanceOf(IllegalArgumentException::class)
      .hasMessage("No license found for id <GPLv3>")
  }
}
