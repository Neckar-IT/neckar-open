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
import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Test

class ForbiddenPackageDependencyTest {

  private val environment = createEnvironment()

  private fun rule(vararg forbidden: String, exemptFiles: List<String> = emptyList()) = ForbiddenPackageDependency(
    TestConfig(
      "forbidden" to forbidden.toList(),
      "exemptFiles" to exemptFiles,
    ),
  )

  @Test
  fun `flags a forbidden import and the type reference it enables`() {
    val findings = rule("com.example.core -> java.io").lintWithContext(
      environment,
      """
      package com.example.core

      import java.io.File

      class Decision(val file: File)
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
    assertThat(findings[0].message).contains("java.io")
  }

  /**
   * The reason this rule exists next to a source scan: a fully-qualified reference has no import
   * line, so reading imports alone never sees it.
   */
  @Test
  fun `flags a fully qualified reference without an import`() {
    val findings = rule("com.example.core -> java.io").lintWithContext(
      environment,
      """
      package com.example.core

      class Decision(val file: java.io.File)
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("java.io.File")
  }

  @Test
  fun `allows a package that is not covered by any entry`() {
    val findings = rule("com.example.core -> java.io").lintWithContext(
      environment,
      """
      package com.example.other

      import java.io.File

      class Consumer(val file: File)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `matches on package segments, never on a shared name prefix`() {
    val findings = rule("com.example.core -> java.i").lintWithContext(
      environment,
      """
      package com.example.core

      import java.io.File

      class Decision(val file: File)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `exempts the mapping seam by file name`() {
    val findings = rule("com.example.core -> java.io", exemptFiles = listOf("dummy.kt")).lintWithContext(
      environment,
      """
      package com.example.core

      import java.io.File

      class Mapper(val file: File)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `an empty configuration forbids nothing`() {
    val findings = ForbiddenPackageDependency(Config.empty).lintWithContext(
      environment,
      """
      package com.example.core

      import java.io.File

      class Decision(val file: File)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
