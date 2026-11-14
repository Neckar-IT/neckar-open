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
import assertk.*
import assertk.assertions.*
import it.neckar.open.isUndefined
import it.neckar.react.common.toast.*
import kotlin.test.Ignore
import kotlin.test.Test

class ToastrTest {
  @Test
  @Ignore // Requires browser environment (toastr library not available in Node.js)
  fun name() {
    //Ensure the toastr object has been loaded
    requireNotNull(toastr)
  }

  @Test
  @Ignore // Requires browser environment (toastr library not available in Node.js)
  fun testIt() {
    Toast.info("The info")
  }

  @Ignore // Requires browser environment
  @Test
  fun testResource() {
    println("1st")
    assertThat(Toast.isUndefined()).isFalse()
    println("2st")
    assertThat(isUndefined(toastrStyles)).isFalse()
    println("3st")
    assertThat(isUndefined(toastr)).isFalse()
    println("4st")
  }
}
