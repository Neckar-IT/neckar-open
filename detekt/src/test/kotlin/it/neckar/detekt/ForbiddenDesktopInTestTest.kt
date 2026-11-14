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
package it.neckar.detekt

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class ForbiddenDesktopInTestTest {

  private val rule = ForbiddenDesktopInTest(Config.empty)

  @Test
  fun `flags the import`() {
    val findings = rule.lint(
      """
      import java.awt.Desktop
      import java.io.File

      fun show(file: File) {
        Desktop.getDesktop().browse(file.toURI())
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("java.awt.Desktop")
  }

  @Test
  fun `flags the import behind an alias`() {
    val findings = rule.lint(
      """
      import java.awt.Desktop as Viewer
      import java.io.File

      fun show(file: File) {
        Viewer.getDesktop().open(file)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags the fully qualified reference`() {
    val findings = rule.lint(
      """
      fun show(file: java.io.File) {
        java.awt.Desktop.getDesktop().open(file)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags every simple name behind the star import`() {
    val findings = rule.lint(
      """
      import java.awt.*
      import java.io.File

      fun show(file: File) {
        if (Desktop.isDesktopSupported()) {
          Desktop.getDesktop().open(file)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
  }

  @Test
  fun `flags the import of a member`() {
    val findings = rule.lint(
      """
      import java.awt.Desktop.getDesktop
      import java.io.File

      fun show(file: File) {
        getDesktop().open(file)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags the fully qualified type`() {
    val findings = rule.lint(
      """
      fun show(desktop: java.awt.Desktop, file: java.io.File) {
        desktop.open(file)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `ignores a qualified type of another package behind the star import`() {
    val findings = rule.lint(
      """
      import java.awt.*

      fun show(desktop: com.example.Desktop) = desktop
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores a Desktop declared in the file behind the star import`() {
    val findings = rule.lint(
      """
      import java.awt.*

      class Screen {
        class Desktop

        fun create(): Desktop = Desktop()
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores the name in a string and in KDoc`() {
    val findings = rule.lint(
      """
      /**
       * Never calls [java.awt.Desktop]
       */
      val hint = "java.awt.Desktop"
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags the star import of the members`() {
    val findings = rule.lint(
      """
      import java.awt.Desktop.*

      fun supported() = isDesktopSupported()
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags a class literal behind the star import`() {
    val findings = rule.lint(
      """
      import java.awt.*

      val type = Desktop::class
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags the simple name outside the class that declares its own Desktop`() {
    val findings = rule.lint(
      """
      import java.awt.*

      class Screen {
        class Desktop
      }

      fun supported() = Desktop.isDesktopSupported()
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `ignores a safe call and a callable reference named Desktop`() {
    val findings = rule.lint(
      """
      import java.awt.*

      class Holder(val Desktop: String)

      fun name(holder: Holder?) = holder?.Desktop
      val getter = Holder::Desktop
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags the simple type behind the star import`() {
    val findings = rule.lint(
      """
      import java.awt.*

      fun show(desktop: Desktop) = desktop
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags a nested type of the fully qualified Desktop`() {
    val findings = rule.lint(
      """
      fun supports(action: java.awt.Desktop.Action) = action
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `ignores a local class and a type parameter named Desktop behind the star import`() {
    val findings = rule.lint(
      """
      import java.awt.*

      fun create(): Any {
        class Desktop
        return Desktop()
      }

      fun <Desktop> same(value: Desktop): Desktop = value
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores a top-level class and a type alias named Desktop behind the star import`() {
    val classFindings = rule.lint(
      """
      import java.awt.*

      class Desktop

      val desktop = Desktop()
      """.trimIndent(),
    )
    val aliasFindings = rule.lint(
      """
      import java.awt.*

      typealias Desktop = String

      fun name(value: Desktop) = value
      """.trimIndent(),
    )

    assertThat(classFindings).isEmpty()
    assertThat(aliasFindings).isEmpty()
  }

  @Test
  fun `ignores a constructor property, a companion class and a top-level function named Desktop behind the star import`() {
    val findings = rule.lint(
      """
      import java.awt.*

      class Holder(val Desktop: String) {
        fun length() = Desktop.length
      }

      class Screen {
        companion object {
          class Desktop
        }

        fun create() = Desktop()
      }

      fun Desktop() = "own"

      val own = Desktop()
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores another Desktop class`() {
    val findings = rule.lint(
      """
      import java.awt.*
      import com.example.Desktop

      fun show() {
        Desktop.getDesktop()
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores a Desktop without import of java awt`() {
    val findings = rule.lint(
      """
      class Desktop {
        fun open() = Unit
      }

      fun show() {
        Desktop().open()
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
