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
package it.neckar.open.javafx

import java.text.MessageFormat
import java.util.MissingResourceException
import java.util.ResourceBundle

/**
 * Helper utility for messages
 */
object MessageUtil {
  @JvmStatic
  operator fun get(resourceBundle: ResourceBundle, key: String): String {
    return getString(resourceBundle, key)
  }

  @JvmStatic
  operator fun get(resourceBundle: ResourceBundle, key: String, vararg args: Any?): String {
    return getString(resourceBundle, key, *args)
  }

  @JvmStatic
  fun getString(resourceBundle: ResourceBundle, key: String): String {
    return try {
      resourceBundle.getString(key)
    } catch (_: MissingResourceException) {
      System.err.println("Can't find entry for key <" + key + "> in bundle " + resourceBundle.baseBundleName)
      key
    }
  }

  @JvmStatic
  fun getString(resourceBundle: ResourceBundle, key: String, vararg args: Any?): String {
    return MessageFormat.format(getString(resourceBundle, key), *args)
  }

  /**
   * Returns the resolved value or null if the key is null.
   */
  @JvmStatic
  @Throws(MissingResourceException::class)
  fun getStringNullable(resourceBundle: ResourceBundle, key: String?): String? {
    return if (key == null) {
      null
    } else resourceBundle.getString(key)
  }

  /**
   * Returns the resolved value or null if the key is null.
   */
  @JvmStatic
  @Throws(MissingResourceException::class)
  fun getStringNullable(resourceBundle: ResourceBundle, key: String?, vararg args: Any?): String? {
    val message = getStringNullable(resourceBundle, key) ?: return null
    return MessageFormat.format(message, *args)
  }

  @JvmStatic
  operator fun get(resourceBundle: ResourceBundle, enumValue: Enum<*>): String {
    return MessageUtil[resourceBundle, enumValue, null]
  }

  @JvmStatic
  operator fun get(resourceBundle: ResourceBundle, enumValue: Enum<*>, category: String?): String {
    val key = getKey(enumValue, category)
    try {
      return resourceBundle.getString(key)
    } catch (_: MissingResourceException) {
      System.err.println("Can't find entry for key <" + enumValue.name + "> in bundle " + resourceBundle.baseBundleName)
      return key
    }
  }

  /**
   * Returns the key for the given enum value and category
   *
   * @param enumValue the enum value
   * @param category  the category
   * @return the key
   */
  @JvmStatic
  @Throws(MissingResourceException::class)
  fun getKey(enumValue: Enum<*>, category: String?): String {
    return if (category == null) {
      enumValue.name
    } else {
      enumValue.name + "." + category
    }
  }
}
