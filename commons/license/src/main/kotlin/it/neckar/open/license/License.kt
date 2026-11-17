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

import java.net.MalformedURLException
import java.net.URL

/**
 * Represents the license of the image
 *
 * @noinspection ClassReferencesSubclass
 */
open class License
@JvmOverloads constructor(
  val id: String,
  val name: String,
  val url: URL? = null,
) {

  constructor(id: String, name: String, url: String) : this(id, name, getUrl(url))

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is License) return false
    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun toString(): String {
    return "$name ($id)"
  }

  companion object {
    /**
     * Constant `UNKNOWN`
     */
    @JvmField
    val UNKNOWN: License = License("UNKNOWN", "Unknown")

    /**
     * Constant `ALL_RIGHTS_RESERVED`
     */
    @JvmField
    val ALL_RIGHTS_RESERVED: License = License("ALL_RIGHTS_RESERVED", "All rights reserved")

    /**
     * Constant `PUBLIC_DOMAIN`
     */
    @JvmField
    val PUBLIC_DOMAIN: License = License("PUBLIC_DOMAIN", "Public Domain")

    @JvmField
    val CDDL: License = License("CDDL", "Common Development and Distribution License", "http://www.opensource.org/licenses/cddl1.php")

    @JvmField
    val AFFERO_GPL: License = License("AFFERO GPL", "Affero GNU Public License", "http://www.opensource.org/licenses/agpl-v3.html")

    @JvmField
    val APACHE_20: License = License("APACHE 2.0", "Apache License 2.0", "http://www.opensource.org/licenses/apache2.0.php")

    @JvmField
    val BSD_NEW: License = License("BSD NEW", "New and Simplified BSD licenses", "http://www.opensource.org/licenses/bsd-license.php")

    @JvmField
    val CPAL: License = License("CPAL", "Common Public Attribution License 1.0", "http://www.opensource.org/licenses/cpal_1.0")

    @JvmField
    val EPL: License = License("EPL", "Eclipse Public License", "http://www.opensource.org/licenses/eclipse-1.0.php")

    @JvmField
    val GPL_2: License = License("GPLv2", "GNU General Public License 2.0", "http://www.opensource.org/licenses/gpl-2.0.php")

    @JvmField
    val GPL_3: License = License("GPLv3", "GNU General Public License 3.0", "http://www.opensource.org/licenses/gpl-3.0.html")

    @JvmField
    val LGPL: License = License("LGPL", "GNU Library or \"Lesser\" General Public License", "http://www.opensource.org/licenses/lgpl-2.1.php")

    @JvmField
    val LGPL_3: License = License("LGPLv3", "GNU Library or \"Lesser\" General Public License version 3.0 (LGPLv3)", "http://www.opensource.org/licenses/lgpl-3.0.html")

    @JvmField
    val MPL: License = License("MPL", "Mozilla Public License 1.1 (MPL)", "http://www.opensource.org/licenses/mozilla1.1.php")

    /**
     * Creative Commons
     */
    @JvmField
    val CC_BY: CreativeCommonsLicense = CreativeCommonsLicense("CC-BY", "CC Attribution", false, CreativeCommonsLicense.ModificationsAllowed.YES, "http://creativecommons.org/licenses/by/3.0")

    /**
     * Share-Alike
     */
    @JvmField
    val CC_BY_SA: CreativeCommonsLicense = CreativeCommonsLicense("CC-BY-SA", "CC Attribution Share Alike", false, CreativeCommonsLicense.ModificationsAllowed.SHARE_ALIKE, "http://creativecommons.org/licenses/by-sa/3.0")

    /**
     * No-Derivative-Work
     */
    @JvmField
    val CC_BY_ND: CreativeCommonsLicense = CreativeCommonsLicense("CC-BY-ND", "CC Attribution No Derivates", false, CreativeCommonsLicense.ModificationsAllowed.NO, "http://creativecommons.org/licenses/by-nd/3.0")

    /**
     * Non-Commercial
     */
    @JvmField
    val CC_BY_NC: CreativeCommonsLicense = CreativeCommonsLicense("CC-BY-NC", "CC Attribution Non-Commercial", true, CreativeCommonsLicense.ModificationsAllowed.YES, "http://creativecommons.org/licenses/by-nc/3.0")

    /**
     * Non-Commercial, Share-Alike
     */
    @JvmField
    val CC_BY_NC_SA: CreativeCommonsLicense = CreativeCommonsLicense("CC-BY-NC-SA", "CC Attribution Non-Commercial Share Alike", true, CreativeCommonsLicense.ModificationsAllowed.SHARE_ALIKE, "http://creativecommons.org/licenses/by-nc-sa/3.0")

    /**
     * Non-Commercial, No-Derivative-Work
     */
    @JvmField
    val CC_BY_NC_ND: CreativeCommonsLicense = CreativeCommonsLicense("CC-BY-NC-ND", "CC Attribution Non-Commercial No Derivates", true, CreativeCommonsLicense.ModificationsAllowed.NO, "http://creativecommons.org/licenses/by-nc-nd/3.0")

    @JvmField
    val CC_LICENSES: List<CreativeCommonsLicense> = listOf(
      CC_BY, CC_BY_SA, CC_BY_ND, CC_BY_NC, CC_BY_NC_SA, CC_BY_NC_ND
    )

    @JvmField
    val LICENSES: List<License> = listOf(
      UNKNOWN, ALL_RIGHTS_RESERVED, PUBLIC_DOMAIN, CDDL, AFFERO_GPL, APACHE_20, BSD_NEW, CPAL, EPL, GPL_2, GPL_3, LGPL, LGPL_3, MPL
    ) + CC_LICENSES

    @JvmStatic
    fun getUrl(url: String): URL {
      return try {
        URL(url)
      } catch (e: MalformedURLException) {
        throw IllegalArgumentException("Not a valid license URL: <$url>", e)
      }
    }

    @JvmStatic
    @Throws(IllegalArgumentException::class)
    operator fun get(id: String): License {
      return findById(LICENSES, id)
    }

    /**
     * Returns the license with the given [id] from [licenses] - throws an [IllegalArgumentException] if there is none
     */
    internal fun findById(licenses: List<License>, id: String): License {
      return licenses.firstOrNull { it.id == id } ?: throw IllegalArgumentException("No license found for id <$id>")
    }
  }
}
