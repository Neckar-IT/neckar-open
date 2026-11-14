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
package it.neckar.open.app.env

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The `.env` parsing every target shares. Runs on JVM and Kotlin/Native, which is the point:
 * a native binary must read the same file the JVM services read, with the same rules.
 */
class EnvFileParserTest {

  @Test
  fun `reads plain key value lines`() {
    val parsed = EnvFileParser.parse(
      """
      GITLAB_ACCESS_TOKEN=glpat-Ky7mQ2vX8nR4tL9wZ3aB
      GITLAB_PROJECT=neckarit/neckar-hub
      """.trimIndent()
    )

    assertEquals("glpat-Ky7mQ2vX8nR4tL9wZ3aB", parsed["GITLAB_ACCESS_TOKEN"])
    assertEquals("neckarit/neckar-hub", parsed["GITLAB_PROJECT"])
  }

  @Test
  fun `skips comments and blank lines`() {
    val parsed = EnvFileParser.parse(
      """
      # the bot's token, rotated quarterly
      GITLAB_ACCESS_TOKEN=glpat-Ky7mQ2vX8nR4tL9wZ3aB

      #GITLAB_PROJECT=neckarit/disabled
      """.trimIndent()
    )

    assertEquals(1, parsed.size)
    assertEquals("glpat-Ky7mQ2vX8nR4tL9wZ3aB", parsed["GITLAB_ACCESS_TOKEN"])
    assertNull(parsed["GITLAB_PROJECT"])
  }

  @Test
  fun `skips an indented comment`() {
    val parsed = EnvFileParser.parse("  # indented, still a comment\nGITLAB_PROJECT=neckarit/neckar-hub")

    assertEquals(1, parsed.size)
    assertEquals("neckarit/neckar-hub", parsed["GITLAB_PROJECT"])
  }

  @Test
  fun `strips one layer of quotes`() {
    val parsed = EnvFileParser.parse(
      """
      DOUBLE="glpat-Ky7mQ2vX8nR4tL9wZ3aB"
      SINGLE='glpat-Ky7mQ2vX8nR4tL9wZ3aB'
      """.trimIndent()
    )

    assertEquals("glpat-Ky7mQ2vX8nR4tL9wZ3aB", parsed["DOUBLE"])
    assertEquals("glpat-Ky7mQ2vX8nR4tL9wZ3aB", parsed["SINGLE"])
  }

  @Test
  fun `keeps an equals sign inside the value`() {
    val parsed = EnvFileParser.parse("API_TOKEN=perm:am9oYW5uZXM=.NDUtMQ==.abc123")

    assertEquals("perm:am9oYW5uZXM=.NDUtMQ==.abc123", parsed["API_TOKEN"])
  }

  @Test
  fun `keeps a hash inside the value because only a whole line may be a comment`() {
    val parsed = EnvFileParser.parse("HETZNER_ROBOT_WS_USER=#ws+xVQzfBD8")

    assertEquals("#ws+xVQzfBD8", parsed["HETZNER_ROBOT_WS_USER"])
  }

  @Test
  fun `ignores a line without an equals sign`() {
    val parsed = EnvFileParser.parse(
      """
      GITLAB_ACCESS_TOKEN=glpat-Ky7mQ2vX8nR4tL9wZ3aB
      THIS_IS_NOT_AN_ASSIGNMENT
      """.trimIndent()
    )

    assertEquals(1, parsed.size)
    assertNull(parsed["THIS_IS_NOT_AN_ASSIGNMENT"])
  }

  @Test
  fun `keeps an empty value`() {
    val parsed = EnvFileParser.parse("GITLAB_ACCESS_TOKEN=")

    assertEquals("", parsed["GITLAB_ACCESS_TOKEN"])
  }
}
