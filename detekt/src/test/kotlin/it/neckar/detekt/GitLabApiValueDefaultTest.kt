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

class GitLabApiValueDefaultTest {

  private val rule = GitLabApiValueDefault(Config.empty)

  @Test
  fun `flags orEmpty on a getter of a gitlab4j model`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.MergeRequest
      fun titleOf(mr: MergeRequest): String = mr.title.orEmpty()
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("`mr.title.orEmpty()`")
  }

  @Test
  fun `flags orEmpty on a bare property inside an extension on a gitlab4j model`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.MergeRequest
      fun MergeRequest.labelNames(): List<String> = labels.orEmpty()
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags each elvis default on a getter of a gitlab4j model`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.MergeRequest
      fun read(mrs: List<MergeRequest>): Long? {
        for (mr: MergeRequest in mrs) {
          val title: String = mr.title ?: ""
          val draft: Boolean = mr.draft ?: false
          val labels: List<String> = mr.labels ?: emptyList()
          val author: String = mr.author?.username ?: "unknown"
          val url: String = mr.getWebUrl() ?: continue
          val count: Long = mr.userNotesCount?.toLong() ?: 0L
          val position: Int = mr.approvalsRequired ?: -1
          return mr.iid ?: return null
        }
        return null
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(8)
  }

  @Test
  fun `flags a default on this inside an extension on a gitlab4j model`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.Pipeline
      fun Pipeline.refOrEmpty(): String = this.ref ?: ""
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags a getter called without receiver inside an extension on a gitlab4j model`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.Branch
      fun fallback(): String? = null
      fun Branch.isProtectedBranch(): Boolean = getProtected() ?: false
      fun Branch.label(): String = fallback() ?: ""
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("`getProtected() ?: false`")
  }

  @Test
  fun `flags break, null, an empty collection, a parenthesized default and a run block ending in return`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.MergeRequest
      fun read(mrs: List<MergeRequest>): List<String?> {
        val titles: MutableList<String?> = mutableListOf()
        for (mr: MergeRequest in mrs) {
          titles += mr.title ?: null
          titles += mr.description ?: ("")
          titles += mr.labels ?: listOf()
          titles += mr.sha ?: break
          titles += mr.webUrl ?: run {
            titles.clear()
            return titles
          }
          titles += mr.sourceBranch ?: run { mr.targetBranch }
        }
        return titles
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(5)
  }

  @Test
  fun `flags a default behind a chain of gitlab4j reads`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.MergeRequest
      fun shaOf(mr: MergeRequest): String = mr.mergeCommitSha ?: mr.sha ?: ""
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("`mr.mergeCommitSha ?: mr.sha ?: \"\"`")
  }

  @Test
  fun `flags a read behind a call with arguments that starts at this`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.GitLabApiException
      class Lookup(private val client: Client) {
        fun description(iid: Long): String = this.client.mergeRequests.get(iid).description.orEmpty()
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags a read behind a lookup inside the chain`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.MergeRequest
      fun assignee(mr: MergeRequest): String = mr.assignees.firstOrNull()?.username ?: "unknown"
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `decides by the declared type of a top-level property, a constructor parameter and a destructured name`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.MergeRequest
      class Settings(val name: String?)
      val shared: MergeRequest = MergeRequest()
      class Holder(private val held: MergeRequest) {
        val own: Settings = Settings(null)
        fun read(pairs: List<Pair<MergeRequest, Settings>>): String {
          val (mr: MergeRequest, settings: Settings) = pairs.first()
          return shared.title.orEmpty() + held.title.orEmpty() + mr.title.orEmpty() + settings.name.orEmpty() + own.name.orEmpty()
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(3)
  }

  @Test
  fun `flags a default on a value without a declared type and behind a call with arguments`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.MergeRequest
      class Lookup(private val client: Client) {
        fun description(iid: Long): String {
          val mergeRequest = client.mergeRequests.get(iid)
          val title: String = mergeRequest.title ?: ""
          return title + client.mergeRequests.get(iid).description.orEmpty()
        }
        fun titles(mrs: List<MergeRequest>): List<String> = mrs.map { it.title.orEmpty() }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(3)
  }

  @Test
  fun `flags a default behind a client call in a file importing no gitlab4j model`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.GitLabApiException
      class Notifier(private val client: Client, private val failure: GitLabApiException) {
        fun labels(iid: Long): List<String> = client.mergeRequests.get(iid).labels.orEmpty()
        fun reason(): String = failure.message.orEmpty()
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("`client.mergeRequests.get(iid).labels.orEmpty()`")
  }

  @Test
  fun `stays silent on the suppressed statement and reports its sibling`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.Diff
      fun patches(diff: Diff): Pair<String, String> {
        // GitLab sends no diff for a binary file.
        @Suppress("GitLabApiValueDefault")
        val patch: String = diff.diff.orEmpty()
        val path: String = diff.newPath.orEmpty()
        return patch to path
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("`diff.newPath.orEmpty()`")
  }

  @Test
  fun `stays silent on a throw and on an explicit branch`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.MergeRequest
      fun <T> T?.requireNotNull(lazyMessage: () -> Any): T = this ?: throw IllegalArgumentException(lazyMessage().toString())
      fun read(mr: MergeRequest): String {
        val iid: Long = mr.iid ?: error("merge request carries no iid")
        val title: String = mr.title.requireNotNull { "!${'$'}iid carries no title" }
        val sha: String = mr.sha ?: throw IllegalStateException("!${'$'}iid carries no sha")
        val description: String? = mr.description
        if (description == null) return title
        return title + sha + description
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `stays silent on a lookup`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.Pipeline
      fun newest(pipelines: List<Pipeline>, byRef: Map<String, List<Pipeline>>, pattern: Regex): Pipeline? {
        val first: Pipeline = pipelines.firstOrNull { it.ref == "main" } ?: return null
        val newest: Pipeline = pipelines.firstOrNull() ?: return null
        val onMain: List<Pipeline> = byRef["main"].orEmpty()
        val count: Long = byRef["main"]?.size?.toLong() ?: 0L
        val match: MatchResult = pattern.find(first.ref.toString()) ?: return null
        val outcome: Pipeline = first as? Pipeline ?: return null
        return onMain.firstOrNull() ?: newest
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `stays silent on a nullable model the own code hands in`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.Pipeline
      fun shaOf(pipeline: Pipeline?): String? {
        val present: Pipeline = pipeline ?: return null
        return present.sha
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `stays silent on a class literal and on a call without arguments outside a model extension`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.GitLabApiException
      class Settings(val name: String?)
      fun settings(): Settings = Settings(null)
      fun describe(failure: GitLabApiException): String {
        val kind: String = failure::class.simpleName ?: "unknown"
        val reason: String = failure.message ?: failure::class.simpleName ?: "unknown error"
        return kind + reason + (settings().name ?: "")
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `stays silent on a value with a declared own type`() {
    val findings = rule.lint(
      """
      import org.gitlab4j.api.models.Pipeline
      class Settings(val baseUrl: String?, val owner: Owner)
      class Owner(val name: String?)
      class Reporter(private val settings: Settings, private val failure: Exception) {
        fun describe(pipeline: Pipeline, local: Settings): String? {
          val baseUrl: String = settings.baseUrl ?: return null
          val owner: String = local.owner.name.orEmpty()
          return baseUrl + owner + failure.message.orEmpty()
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `stays silent in a file without an import from gitlab4j`() {
    val findings = rule.lint(
      """
      class Entry(val title: String?)
      fun titleOf(entry: Entry): String = entry.title.orEmpty()
      fun titles(entries: List<Entry>): List<String> = entries.map { it.title ?: "" }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
