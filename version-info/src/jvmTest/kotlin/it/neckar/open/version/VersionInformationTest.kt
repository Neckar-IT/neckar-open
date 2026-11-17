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
package it.neckar.open.version

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.RestoreSystemProperties

/**
 * [GitProperty.systemProperty] is no compile-time constant, so a test sets its key in the body instead of `@SetSystemProperty`.
 */
@RestoreSystemProperties
class VersionInformationTest {
  @Test
  fun testStableValues() {
    // Pinning the major version makes every major bump fail here (monorepo.version 9 -> 10 did).
    // What is worth asserting is that the generated constant carries a version at all.
    assertThat(VersionInformation.version).matches(Regex("""\d+\.\d+\.\d+(-SNAPSHOT)?"""))
    assertThat(VersionInformation.versionAsStringVerbose).contains(VersionInformation.version)
  }

  @Test
  fun testSystemPropertyHasHighestPriority() {
    System.setProperty(GitProperty.Hash.systemProperty, "cafebabe000000000000000000000000000000ff")
    assertThat(resolveGitProperty(GitProperty.Hash)).isEqualTo("cafebabe000000000000000000000000000000ff")
  }

  @Test
  fun testResolvesPerProperty() {
    System.setProperty(GitProperty.CommitDateTime.systemProperty, "2026-07-11T00:00:00Z")
    assertThat(resolveGitProperty(GitProperty.CommitDateTime)).isEqualTo("2026-07-11T00:00:00Z")
    //Hash is not set — resolves independently of CommitDateTime
    assumeTrue(System.getenv(GitProperty.Hash.envVar) == null)
    assertThat(resolveGitProperty(GitProperty.Hash)).isEqualTo(VersionInformation.UnknownValue)
  }

  @Test
  fun testFallsBackToUnknownWithoutAnySource() {
    //The env is not set in test JVMs
    GitProperty.entries.forEach { gitProperty ->
      assumeTrue(System.getenv(gitProperty.envVar) == null)
      assertThat(resolveGitProperty(gitProperty)).isEqualTo(VersionInformation.UnknownValue)
    }
  }

  @Test
  fun testGitHashShortIsDerivedFromGitHash() {
    assertThat(VersionInformation.gitHashShort).isEqualTo(VersionInformation.gitHash.take(12))
  }

  @Test
  fun testCommitDateAndShortHash() {
    assertThat(formatCommitDateAndShortHash("2026-07-15T09:41:03Z", "1d79fb128769")).isEqualTo("2026-07-15 - 1d79fb128769")
  }

  @Test
  fun testCommitDateAndShortHashIsUnknownWithoutAnyDeployMetadata() {
    assertThat(formatCommitDateAndShortHash(VersionInformation.UnknownValue, VersionInformation.UnknownValue))
      .isEqualTo(VersionInformation.UnknownValue)
  }

  @Test
  fun testCommitDateAndShortHashMarksTheUnresolvedComponent() {
    assertThat(formatCommitDateAndShortHash(VersionInformation.UnknownValue, "1d79fb128769")).isEqualTo("unknown - 1d79fb128769")
    assertThat(formatCommitDateAndShortHash("2026-07-15T09:41:03Z", VersionInformation.UnknownValue)).isEqualTo("2026-07-15 - unknown")
  }

  @Test
  fun testCommitDateAndShortHashOfThisTestJvmIsUnknown() {
    //Tests run without injected deploy metadata — no faked date, no faked hash
    GitProperty.entries.forEach { gitProperty ->
      assumeTrue(System.getenv(gitProperty.envVar) == null)
    }
    assertThat(VersionInformation.commitDateAndShortHash).isEqualTo(VersionInformation.UnknownValue)
  }

  @Test
  fun testImageDigestIsNullWithoutInjection() {
    assumeTrue(System.getenv(VersionInformation.ImageDigestEnvVar) == null)
    assertThat(VersionInformation.imageDigestOrNull).isNull()
  }

  @Test
  fun testUnpinnedImageDigestCountsAsNotInjected() {
    assertThat(pinnedImageDigestOrNull(VersionInformation.UnpinnedImageDigest)).isNull()
    assertThat(pinnedImageDigestOrNull(null)).isNull()
    assertThat(pinnedImageDigestOrNull(ImageDigest)).isEqualTo(ImageDigest)
  }

  @Test
  fun testMalformedImageDigestFails() {
    assertFailure { pinnedImageDigestOrNull("sha256:not-hex") }.messageContains("APP_IMAGE_DIGEST must be sha256:<64 hex characters> or unpinned but was <sha256:not-hex>")
  }

  @Test
  fun testShortImageDigestTakesTwelveHexCharactersBehindTheAlgorithm() {
    assertThat(ImageDigestText.shortOf(ImageDigest)).isEqualTo("ad2453b02334")
  }

  @Test
  fun testArtifactIdentityPrefersTheImageDigest() {
    assertThat(artifactIdentityOf(ImageDigest, ArtifactKey, GitHash)).isEqualTo(ImageDigest)
    assertThat(artifactIdentityShortOf(ImageDigest, ArtifactKey, GitHash)).isEqualTo("ad2453b02334")
  }

  @Test
  fun testArtifactIdentityOfAFatJarIsItsArtifactKey() {
    assertThat(artifactIdentityOf(null, ArtifactKey, GitHash)).isEqualTo(ArtifactKey)
    assertThat(artifactIdentityShortOf(null, ArtifactKey, GitHash)).isEqualTo("in-3668efd5ddda")
  }

  @Test
  fun testArtifactKeyComesFromTheFatJarResource() {
    assertThat(resolveArtifactKey()).isEqualTo(ArtifactKey)
  }

  @Test
  fun testArtifactIdentityFallsBackToTheGitHash() {
    assertThat(artifactIdentityOf(null, null, GitHash)).isEqualTo(GitHash)
    assertThat(artifactIdentityShortOf(null, null, GitHash)).isEqualTo("1d79fb128769")
  }

  private companion object {
    const val ImageDigest: String = "sha256:ad2453b02334597670fc64befad42c10ec0870d092c58b8ae0ca6ef1e903d221"
    const val GitHash: String = "1d79fb128769c3b1a2f0e4d5c6b7a8f9e0d1c2b3"
    const val ArtifactKey: String = "in-3668efd5ddda0006ec4e4a1b2c3d4e5f3668efd5ddda0006ec4e4a1b2c3d4e5f"
  }
}
