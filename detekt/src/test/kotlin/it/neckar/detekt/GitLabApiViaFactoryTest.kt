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

class GitLabApiViaFactoryTest {

  private val rule = GitLabApiViaFactory(Config.empty)

  @Test
  fun `flags the constructor behind an import`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.GitLabApi

      fun connect(url: String, token: String): GitLabApi {
        return GitLabApi(url, token)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("gitLabApi(")
  }

  @Test
  fun `flags the fully qualified constructor`() {
    val findings = rule.lint(
      """
      val api = org.gitlab4j.api.GitLabApi("https://git.example.com", "token")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags the constructor behind an import alias`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.GitLabApi as Client

      val api = Client("https://git.example.com", "token")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags the constructor behind a star import`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.*

      val api = GitLabApi("https://git.example.com", "token")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `stays silent in the file declaring the factory`() {
    val findings = rule.lint(
      """
      package it.neckar.tools.gitlab.api

      import org.gitlab4j.api.GitLabApi

      fun gitLabApi(serverUrl: String, accessToken: String): GitLabApi {
        return GitLabApi(serverUrl, accessToken)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores the factory and a GitLabApi type reference`() {
    val findings = rule.lint(
      """
      import it.neckar.tools.gitlab.api.gitLabApi
      import org.gitlab4j.api.GitLabApi

      class Reader(private val api: GitLabApi)

      val reader = Reader(gitLabApi(serverUrl, accessToken))
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores another class named GitLabApi`() {
    val findings = rule.lint(
      """
      import example.client.GitLabApi

      val api = GitLabApi("https://git.example.com")
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores a GitLabApi of the own package`() {
    val findings = rule.lint(
      """
      package example

      class GitLabApi(val url: String)

      val api = GitLabApi("https://git.example.com")
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
