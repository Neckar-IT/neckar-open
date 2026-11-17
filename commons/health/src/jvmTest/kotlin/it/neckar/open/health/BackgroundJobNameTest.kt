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
package it.neckar.open.health

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

/**
 * The name becomes the `job.name` label of a metric, so one alert rule reads it across every service.
 * A space or a capital in a single service's name is enough to make the same rule render differently.
 */
class BackgroundJobNameTest {
  @Test
  fun `a kebab-case name is accepted as it is`() {
    assertThat(BackgroundJobName("deploy-check-target-polling").value).isEqualTo("deploy-check-target-polling")
  }

  @Test
  fun `a name with a space or a capital is rejected`() {
    assertFailure { BackgroundJobName("gitlab-companion-E1 Bug-Sweep") }.isInstanceOf(IllegalArgumentException::class)
    assertFailure { BackgroundJobName("HuddleTeamSync") }.isInstanceOf(IllegalArgumentException::class)
    assertFailure { BackgroundJobName("") }.isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  fun `of splits camel humps`() {
    assertThat(BackgroundJobName.of("gitlab-companion", "IssueSnapshotFullReconcile").value)
      .isEqualTo("gitlab-companion-issue-snapshot-full-reconcile")
  }

  @Test
  fun `of turns a schedule label into a name`() {
    assertThat(BackgroundJobName.of("gitlab-companion", "E1 Bug-Sweep").value).isEqualTo("gitlab-companion-e1-bug-sweep")
    assertThat(BackgroundJobName.of("agent-orchestrator", "H1 Token-Validation").value).isEqualTo("agent-orchestrator-h1-token-validation")
  }

  @Test
  fun `of leaves an already kebab-case part alone`() {
    assertThat(BackgroundJobName.of("huddle", "jitsi-sync").value).isEqualTo("huddle-jitsi-sync")
  }
}
