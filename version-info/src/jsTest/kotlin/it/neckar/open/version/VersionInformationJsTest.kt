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
import kotlin.test.AfterTest
import kotlin.test.Test

class VersionInformationJsTest {
  @AfterTest
  fun clearInjectedGitInfo() {
    js("delete globalThis.__APP_GIT_INFO__")
  }

  @Test
  fun testStableValues() {
    // Pinning the major version makes every major bump fail here (monorepo.version 9 -> 10 did).
    // What is worth asserting is that the generated constant carries a version at all.
    assertThat(VersionInformation.version).matches(Regex("""\d+\.\d+\.\d+(-SNAPSHOT)?"""))
  }

  @Test
  fun testFallsBackToUnknownWithoutInjection() {
    //Tests run without serve-time injection — the resolver falls back without throwing
    assertThat(resolveGitProperty(GitProperty.Hash)).isEqualTo(VersionInformation.UnknownValue)
    assertThat(resolveGitProperty(GitProperty.CommitDateTime)).isEqualTo(VersionInformation.UnknownValue)
  }

  @Test
  fun testResolvesFromAppGitInfoObject() {
    js("globalThis.__APP_GIT_INFO__ = { gitHash: 'cafebabe000000000000000000000000000000ff', gitCommitDateTime: '2026-07-11T00:00:00Z' }")
    assertThat(resolveGitProperty(GitProperty.Hash)).isEqualTo("cafebabe000000000000000000000000000000ff")
    assertThat(resolveGitProperty(GitProperty.CommitDateTime)).isEqualTo("2026-07-11T00:00:00Z")
  }

  @Test
  fun testIgnoresEmptyInjectedValues() {
    //An unfilled placeholder (empty string) must not shadow the fallback
    js("globalThis.__APP_GIT_INFO__ = { gitHash: '' }")
    assertThat(resolveGitProperty(GitProperty.Hash)).isEqualTo(VersionInformation.UnknownValue)
  }

  @Test
  fun testIgnoresUnreplacedPlaceholders() {
    //HTML served without the injecting entrypoint (local webpack dev server) still carries the
    //raw ${APP_GIT_HASH} placeholder — must resolve to the fallback, not the placeholder string
    js("globalThis.__APP_GIT_INFO__ = { gitHash: '\\u0024{APP_GIT_HASH}' }")
    assertThat(resolveGitProperty(GitProperty.Hash)).isEqualTo(VersionInformation.UnknownValue)
  }

  @Test
  fun testFailsOnAnInjectedValueOfAnotherType() {
    js("globalThis.__APP_GIT_INFO__ = { gitHash: 42 }")
    assertFailure { resolveGitProperty(GitProperty.Hash) }.messageContains("globalThis.__APP_GIT_INFO__.gitHash holds number instead of a string")
  }

  @Test
  fun testGitHashShortIsDerivedFromGitHash() {
    assertThat(VersionInformation.gitHashShort).isEqualTo(VersionInformation.gitHash.take(12))
  }

  @Test
  fun testImageDigestFallsBackToNullWithoutMetaTag() {
    assertThat(resolveImageDigest()).isNull()
  }

  @Test
  fun testResolvesTheImageDigestFromItsMetaTag() {
    withImageDigestMeta(ImageDigest) {
      assertThat(resolveImageDigest()).isEqualTo(ImageDigest)
      assertThat(pinnedImageDigestOrNull(resolveImageDigest())).isEqualTo(ImageDigest)
    }
  }

  @Test
  fun testMalformedImageDigestMetaTagCountsAsNotInjected() {
    withImageDigestMeta("sha256:not-hex") {
      assertThat(resolveImageDigest()).isNull()
      assertThat(pinnedImageDigestOrNull(resolveImageDigest())).isNull()
    }
  }

  @Test
  fun testUnpinnedImageDigestMetaTagCountsAsNotInjected() {
    withImageDigestMeta(VersionInformation.UnpinnedImageDigest) {
      assertThat(pinnedImageDigestOrNull(resolveImageDigest())).isNull()
    }
  }

  /**
   * Runs [block] with `<meta name="imageDigest" content="[content]">` in place: in the real document under a browser,
   * in a stand-in document under Node, which has none.
   */
  private fun withImageDigestMeta(content: String, block: () -> Unit) {
    val global: dynamic = js("globalThis")
    global.testImageDigestContent = content
    js(
      """
      if (typeof document === 'undefined') {
        globalThis.testImageDigestStandInDocument = true;
        globalThis.document = { querySelector: function (selector) {
          return selector === "meta[name='imageDigest']" ? { getAttribute: function () { return globalThis.testImageDigestContent; } } : null;
        } };
      } else {
        var meta = document.createElement('meta');
        meta.setAttribute('name', 'imageDigest');
        meta.setAttribute('content', globalThis.testImageDigestContent);
        meta.setAttribute('id', 'test-image-digest-meta');
        document.head.appendChild(meta);
      }
      """
    )
    try {
      block()
    } finally {
      js(
        """
        if (globalThis.testImageDigestStandInDocument) {
          delete globalThis.document;
          delete globalThis.testImageDigestStandInDocument;
        } else {
          document.getElementById('test-image-digest-meta').remove();
        }
        delete globalThis.testImageDigestContent;
        """
      )
    }
  }

  private companion object {
    const val ImageDigest: String = "sha256:ad2453b02334597670fc64befad42c10ec0870d092c58b8ae0ca6ef1e903d221"
  }
}
