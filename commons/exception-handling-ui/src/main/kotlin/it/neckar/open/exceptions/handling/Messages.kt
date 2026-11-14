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
package it.neckar.open.exceptions.handling

import org.jetbrains.annotations.PropertyKey
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

/**
 */
object Messages {
  private val LOG = LoggerFactory.getLogger(Messages::class.java.name)

  private const val BundleName = "it.neckar.open.exceptions.handling.exception-messages"
  private val RESOURCE_BUNDLE = ResourceBundle.getBundle(BundleName, Locale.getDefault(), Messages::class.java.classLoader)

  /**
   * Returns the value for the given key
   */
  @JvmStatic
  operator fun get(@PropertyKey(resourceBundle = BundleName) key: String): String {
    return try {
      RESOURCE_BUNDLE.getString(key)
    } catch (_: MissingResourceException) {
      LOG.error("Key not found <" + RESOURCE_BUNDLE.javaClass.name + ": " + key + ">")
      key
    }
  }

  /**
   * Formats the resolved value for the given key
   */
  @JvmStatic
  operator fun get(@PropertyKey(resourceBundle = BundleName) key: String, vararg args: Any): String {
    val message = get(key)
    return MessageFormat.format(message, *args)
  }

  /**
   * Returns the value or null if the key is null
   *
   * @param key the key
   */
  @JvmStatic
  fun getNullable(@PropertyKey(resourceBundle = BundleName) key: String?): String? {
    return if (key == null) {
      null
    } else get(key)
  }

  @JvmStatic
  fun getNullable(@PropertyKey(resourceBundle = BundleName) key: String?, vararg args: Any): String? {
    val message = getNullable(key) ?: return null
    return MessageFormat.format(message, *args)
  }

  @JvmStatic
  @JvmOverloads
  operator fun get(enumValue: Enum<*>, specifier: String? = null): String {
    return get(getKey(enumValue, specifier))
  }

  @JvmStatic
  @Throws(MissingResourceException::class)
  private fun getKey(enumValue: Enum<*>): String {
    return getKey(enumValue, null)
  }

  /**
   * Returns the key for a enum value and specifier
   */
  @JvmStatic
  @Throws(MissingResourceException::class)
  fun getKey(enumValue: Enum<*>, specifier: String?): String {
    return if (specifier == null) {
      enumValue.name
    } else enumValue.name + "." + specifier

  }
}
