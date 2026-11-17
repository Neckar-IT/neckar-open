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
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class GitLabAccessThroughClientTest {

  private val rule = GitLabAccessThroughClient(Config.empty)

  @Test
  fun `flags an accessor property in a product module`() {
    val findings = rule.lint(
      """
      package it.neckar.tools.gitlab.companion

      import org.gitlab4j.api.GitLabApi

      fun approve(gitLabApi: GitLabApi) {
        gitLabApi.mergeRequestApi.approveMergeRequest("neckarit/neckar-hub", 20669L, null)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags an accessor getter and a safe call`() {
    val findings = rule.lint(
      """
      package it.neckar.tools.gitlab.companion

      fun read(api: org.gitlab4j.api.GitLabApi?) {
        api?.projectApi
        api!!.getUserApi().currentUser
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
  }

  @Test
  fun `flags a raw REST call through AbstractApi`() {
    val findings = rule.lint(
      """
      package it.neckar.tools.gitlabtools

      import org.gitlab4j.api.AbstractApi
      import org.gitlab4j.api.GitLabApi

      class NotificationSettingsClient(api: GitLabApi) : AbstractApi(api)
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `passes the package of GitLabClient`() {
    val findings = rule.lint(
      """
      package it.neckar.tools.gitlab.api.client

      import org.gitlab4j.api.AbstractApi
      import org.gitlab4j.api.GitLabApi

      class GitLabMergeRequestOperations(private val api: GitLabApi) : AbstractApi(api) {
        fun approve() = api.mergeRequestApi.approveMergeRequest("neckarit/neckar-hub", 20669L, null)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `passes a test fake that overrides an accessor`() {
    val findings = rule.lint(
      """
      package it.neckar.tools.gitlab.companion

      import org.gitlab4j.api.GitLabApi
      import org.gitlab4j.api.MergeRequestApi

      class FakeGitLabApi : GitLabApi("https://git.example.com", "token") {
        override fun getMergeRequestApi(): MergeRequestApi = MergeRequestApi(this)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
