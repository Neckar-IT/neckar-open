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
package it.neckar.open.app.xdg

import it.neckar.open.app.xdg.XdgUtil.cacheHome
import it.neckar.open.app.xdg.XdgUtil.configHome
import it.neckar.open.app.xdg.XdgUtil.dataHome
import com.google.common.base.StandardSystemProperty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 */
class XdgUtilDemo {
  @Test
  @Throws(Exception::class)
  fun testIt() {
    val value = StandardSystemProperty.OS_NAME.value()
    if (!value!!.contains("Linux")) {
      System.err.println("Not running on Linux!")
      return
    }
    System.err.println("Config Home: " + configHome.absolutePath)
    System.err.println("Cache Home: " + cacheHome.absolutePath)
    System.err.println("Data Home: " + dataHome.absolutePath)
  }

  @Disabled
  @Test
  @Throws(Exception::class)
  fun testFields() {
    println("Fields --------------")
    for (field in XdgUtil::class.java.declaredFields) {
      println("Field: $field")
    }
    println("Methods --------------")
    for (method in XdgUtil::class.java.declaredMethods) {
      println("Method: $method")
    }
  }
}
