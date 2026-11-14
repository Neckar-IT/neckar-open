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
package it.neckar.open.app.clikt.model

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import it.neckar.open.app.gitlab.GitLabProjectId
import kotlin.test.Test

/**
 * The credential and identifier value classes, verified on every target this module builds for.
 *
 * They carry the invariants that keep a blank token or a non-positive project id from ever
 * reaching an API call, and the masking that keeps a token out of a log line.
 */
class TokenValueClassesTest {

  @Test
  fun `masks the gitlab access token in its string form`() {
    assertThat(GitlabAccessToken("glpat-Ky7mQ2vX8nR4tL9wZ3aB").toString()).isEqualTo("****")
  }

  @Test
  fun `keeps the raw value reachable behind the mask`() {
    assertThat(GitlabAccessToken("glpat-Ky7mQ2vX8nR4tL9wZ3aB").value).isEqualTo("glpat-Ky7mQ2vX8nR4tL9wZ3aB")
  }

  @Test
  fun `rejects a blank token`() {
    assertFailure { GitlabAccessToken("   ") }.isInstanceOf(IllegalArgumentException::class)
    assertFailure { GitlabAccessToken("") }.isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  fun `prints the project path unchanged`() {
    assertThat(ProjectPath("neckarit/neckar-hub").toString()).isEqualTo("neckarit/neckar-hub")
  }

  @Test
  fun `rejects an empty project path`() {
    assertFailure { ProjectPath("") }.isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  fun `prints the gitlab project id as its number`() {
    assertThat(GitLabProjectId(47).toString()).isEqualTo("47")
  }

  @Test
  fun `rejects a project id that is not positive`() {
    assertFailure { GitLabProjectId(0) }.isInstanceOf(IllegalArgumentException::class)
    assertFailure { GitLabProjectId(-1) }.isInstanceOf(IllegalArgumentException::class)
  }
}
