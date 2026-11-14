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
package it.neckar.open.app


import it.neckar.open.net.HostnameSupport
import it.neckar.runtime.context.Hostname
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Offers information about the environment.
 *
 * In most cases [it.neckar.runtime.context.RuntimeContext] should be used instead.
 */
object EnvironmentInformation {
  /**
   * Returns the host name
   */
  @JvmStatic
  val hostName: Hostname?
    get() {
      return HostnameSupport.guess()
    }

  /**
   * Returns the current username
   */
  @JvmStatic
  val userName: String
    get() = System.getProperty("user.name")


  /**
   * Creates host description string
   */
  fun createHostDescription(): String {
    return "Local Date: " + DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()) + "\n" +
      "User: " + System.getProperty("user.name") + "\n" +
      "Host Name: " + hostName + "\n"
  }

  /**
   * Returns a string that describes the environment
   */
  fun createEnvironmentDescription(): String {
    val builder = StringBuilder()

    val properties = System.getProperties()
    val enumeration = properties.propertyNames()
    while (enumeration.hasMoreElements()) {
      val propertyKey = enumeration.nextElement()
      val value = properties[propertyKey]

      builder.append(propertyKey).append(": ").append(value).append("\n")
    }

    return builder.toString()
  }
}
