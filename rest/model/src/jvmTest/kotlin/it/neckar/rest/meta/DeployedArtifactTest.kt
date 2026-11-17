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
package it.neckar.rest.meta

import assertk.assertThat
import assertk.assertions.isEqualTo
import it.neckar.open.version.GitProperty
import it.neckar.open.version.VersionInformation
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DeployedArtifactTest {
  @Test
  fun `serializes as a plain string`() {
    val deployedArtifact = DeployedArtifact("2026-07-15 - 1d79fb128769")
    assertThat(Json.encodeToString(deployedArtifact)).isEqualTo(""""2026-07-15 - 1d79fb128769"""")
    assertThat(Json.decodeFromString<DeployedArtifact>(""""2026-07-15 - 1d79fb128769"""")).isEqualTo(deployedArtifact)
  }

  @Test
  fun `blank values are rejected`() {
    assertThrows<IllegalArgumentException> {
      DeployedArtifact(" ")
    }
  }

  @Test
  fun `unresolved deploy metadata stays recognizable`() {
    //Tests run without injected deploy metadata — no faked date, no faked hash, no monorepo version
    GitProperty.entries.forEach { gitProperty ->
      assumeTrue(System.getenv(gitProperty.envVar) == null)
    }
    assumeTrue(System.getenv(VersionInformation.ImageDigestEnvVar) == null)
    assertThat(DeployedArtifact.fromVersionInformation()).isEqualTo(DeployedArtifact(VersionInformation.UnknownValue))
  }

  @Test
  fun `a pinned image digest names the artifact`() {
    assertThat(DeployedArtifact.of("sha256:ad2453b02334597670fc64befad42c10ec0870d092c58b8ae0ca6ef1e903d221", "2026-07-15 - 1d79fb128769"))
      .isEqualTo(DeployedArtifact("ad2453b02334"))
  }

  @Test
  fun `without an image digest the commit names the artifact`() {
    assertThat(DeployedArtifact.of(null, "2026-07-15 - 1d79fb128769")).isEqualTo(DeployedArtifact("2026-07-15 - 1d79fb128769"))
  }
}
