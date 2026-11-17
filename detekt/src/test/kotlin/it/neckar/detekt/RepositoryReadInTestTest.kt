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

class RepositoryReadInTestTest {

  private val rule = RepositoryReadInTest(Config.empty)

  @Test
  fun `flags a root-walking helper`() {
    val findings = rule.lint(
      """
      class SampleTest {
        fun read() = findRepoRoot().resolve("products-catalog.yaml").readText()
        private fun findRepoRoot(): java.io.File = java.io.File("")
      }
      """.trimIndent(),
    )

    // The call and the File("") that starts the walk.
    assertThat(findings).hasSize(2)
    assertThat(findings[0].message).contains("findRepoRoot()")
    assertThat(findings[0].message).contains("SharedFiles")
  }

  @Test
  fun `flags the working directory property`() {
    val findings = rule.lint(
      """
      val root = java.io.File(System.getProperty("user.dir"))
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("user.dir")
  }

  @Test
  fun `flags a path literal above the module`() {
    val findings = rule.lint(
      """
      val compose = java.io.File("../sibling-deployment/src/deployment/docker-compose.yml")
      val lock = java.nio.file.Path.of("..", "docker", "bake-image-input-fingerprints.lock")
      val sibling = java.io.File("x").resolve("../y")
      val other = java.io.File("x").toPath().resolveSibling("../y")
      val above = kotlin.io.path.Path("..")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(5)
    assertThat(findings[0].message).contains("`File(\"../…\")`")
    assertThat(findings[1].message).contains("`Path.of(\"../…\")`")
    assertThat(findings[2].message).contains("`resolve(\"../…\")`")
    assertThat(findings[3].message).contains("`resolveSibling(\"../…\")`")
    assertThat(findings[4].message).contains("`Path(\"../…\")`")
  }

  @Test
  fun `flags the parent of the module directory`() {
    val findings = rule.lint(
      """
      val root = rootDir.parentFile
      val parent = projectDir.parentFile
      val projectRoot = project.rootDir.parentFile
      val safe = rootDir?.parentFile
      val asserted = project.rootDir!!.parentFile
      """.trimIndent(),
    )

    assertThat(findings).hasSize(5)
  }

  @Test
  fun `flags the working directory as a path`() {
    val findings = rule.lint(
      """
      val here = java.io.File("")
      val cwd = java.nio.file.Paths.get("")
      val bare = File("")
      val bareKotlinPath = Path("")
      val empty = java.nio.file.Path.of("")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(5)
    assertThat(findings[0].message).contains("`File(\"\")`")
    assertThat(findings[1].message).contains("`Paths.get(\"\")`")
    assertThat(findings[3].message).contains("`Path(\"\")`")
    assertThat(findings[4].message).contains("`Path.of(\"\")`")
  }

  @Test
  fun `ignores a classpath resource and a path inside the module`() {
    val findings = rule.lint(
      """
      val lock = javaClass.getResource("/docker/bake-image-input-fingerprints.lock")
      val fixture = java.io.File("src/test/resources/fixture.json")
      val other = System.getProperty("java.io.tmpdir")
      val interpolated = java.io.File("${'$'}{root}/x")
      val sha = GitBlobSha.of("")
      val entry = map.get("")
      val child = directory.resolve("child")
      val violation = SchemaViolation.Path("")
      val relative = kotlinx.io.files.Path(".")
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
