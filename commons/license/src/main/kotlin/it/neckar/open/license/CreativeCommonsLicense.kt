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
import java.util.Locale

/**
 *
 * CreativeCommonsLicense class.
 *
 */
class CreativeCommonsLicense(
  id: String,
  name: String,
  val isRestrictedToNonCommercial: Boolean,
  val modificationsAllowed: ModificationsAllowed,
  url: String,
) : License(id, name, url) {

  val isDerivedWorkAllowed: Boolean
    get() = modificationsAllowed == ModificationsAllowed.YES

  val isUsableCommercially: Boolean
    get() = !isRestrictedToNonCommercial

  val isSharedAlikeDerivedWorkAllowed: Boolean
    get() = modificationsAllowed == ModificationsAllowed.SHARE_ALIKE || modificationsAllowed == ModificationsAllowed.YES

  /**
   * Returns the translated URL
   *
   * @param locale the locale
   * @return the URL for the locale
   */
  fun getUrl(locale: Locale): URL {
    requireNotNull(url) {
      "Url must not be null"
    }

    return try {
      URL(url.protocol, url.host, url.port, url.file + "/" + locale.language)
    } catch (e: MalformedURLException) {
      throw IllegalArgumentException("Could not build the license URL for locale <${locale.language}> from <$url>", e)
    }
  }

  enum class ModificationsAllowed {
    YES, SHARE_ALIKE, NO
  }

  companion object {
    operator fun get(id: String): License {
      return findById(CC_LICENSES, id)
    }
  }
}
