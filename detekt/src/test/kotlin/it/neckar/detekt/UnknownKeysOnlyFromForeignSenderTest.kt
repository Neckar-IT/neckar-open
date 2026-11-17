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

class UnknownKeysOnlyFromForeignSenderTest {

  private val rule = UnknownKeysOnlyFromForeignSender(Config.empty)

  @Test
  fun `flags strictMode false without a foreign sender`() {
    val findings = rule.lint(
      """
      import com.charleskorn.kaml.Yaml
      import com.charleskorn.kaml.YamlConfiguration

      object CatalogParser {
        private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("strictMode = false")
  }

  @Test
  fun `flags ignoreUnknownKeys true without a foreign sender`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.Json

      private val ProtocolJson: Json = Json {
        ignoreUnknownKeys = true
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("ignoreUnknownKeys = true")
  }

  @Test
  fun `passes strictMode false on a declaration naming its foreign sender`() {
    val findings = rule.lint(
      """
      import com.charleskorn.kaml.Yaml
      import com.charleskorn.kaml.YamlConfiguration
      import it.neckar.open.annotations.ForeignFormat

      @ForeignFormat(sender = "pnpm, which owns pnpm-workspace.yaml")
      private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `passes ignoreUnknownKeys true inside a class naming its foreign sender`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.Json
      import it.neckar.open.annotations.ForeignFormat

      @ForeignFormat(sender = "GitLab webhooks")
      class WebhookParser {
        fun parse(text: String): Any {
          val json = Json { ignoreUnknownKeys = true }
          return json.parseToJsonElement(text)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags a foreign sender left blank`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.Json
      import it.neckar.open.annotations.ForeignFormat

      @ForeignFormat(sender = "")
      private val GitLabJson: Json = Json { ignoreUnknownKeys = true }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `passes a declaration on the allowlist`() {
    val findings = rule.lint(
      """
      package it.neckar.softwarefactory.spec.parser

      import com.charleskorn.kaml.Yaml
      import com.charleskorn.kaml.YamlConfiguration

      enum class SpecStrictness(val yaml: Yaml) {
        Strict(Yaml(configuration = YamlConfiguration(strictMode = true))),
        Tolerant(Yaml(configuration = YamlConfiguration(strictMode = false))),
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `passes the strict settings`() {
    val findings = rule.lint(
      """
      import com.charleskorn.kaml.Yaml
      import com.charleskorn.kaml.YamlConfiguration
      import kotlinx.serialization.json.Json

      private val yaml = Yaml(configuration = YamlConfiguration(strictMode = true))
      private val json = Json { ignoreUnknownKeys = false }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
