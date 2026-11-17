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
package it.neckar.open.http

import assertk.*
import assertk.assertions.*
import it.neckar.projects.common.Port
import org.junit.jupiter.api.Test

/**
 *
 */
@Suppress("HttpUrlsUsage")
class UrlTest {
  @Test
  fun testPort() {
    assertThat(Url.absolute("https://neckar.it:8080/some/path").port()).isEqualTo(Port(8080))
    assertThat(Url.absolute("http://neckar.it:8080/some/path").port()).isEqualTo(Port(8080))
    assertThat(Url.absolute("http://neckar.it:8080/").port()).isEqualTo(Port(8080))
    assertThat(Url.absolute("http://neckar.it:8080").port()).isEqualTo(Port(8080))
    assertThat(Url.absolute("https://neckar.it/some/path").port()).isEqualTo(Port.Https)
    assertThat(Url.absolute("http://neckar.it/some/path").port()).isEqualTo(Port.Http)
    assertThat(Url.absolute("http://neckar.it").port()).isEqualTo(Port.Http)
    assertThat(Url.absolute("http://neckar.it/").port()).isEqualTo(Port.Http)
  }

  @Test
  fun testWithoutPath() {
    assertThat(Url.absolute("https://neckar.it:8080/some/path").withoutPath()).isEqualTo("https://neckar.it:8080")
    assertThat(Url.absolute("http://neckar.it:8080/some/path").withoutPath()).isEqualTo("http://neckar.it:8080")
    assertThat(Url.absolute("https://neckar.it/some/path").withoutPath()).isEqualTo("https://neckar.it")
    assertThat(Url.absolute("http://neckar.it/some/path").withoutPath()).isEqualTo("http://neckar.it")
  }

  @Test
  fun testProtocolAndHost() {
    assertThat(Url.absolute("https://neckar.it:8080/some/path").protocolAndHost()).isEqualTo("https://neckar.it")
    assertThat(Url.absolute("http://neckar.it:8080/some/path").protocolAndHost()).isEqualTo("http://neckar.it")
    assertThat(Url.absolute("https://neckar.it/some/path").protocolAndHost()).isEqualTo("https://neckar.it")
    assertThat(Url.absolute("http://neckar.it/some/path").protocolAndHost()).isEqualTo("http://neckar.it")
  }

  @Test
  fun testProtocol() {
    assertThat(Url.absolute("https://neckar.it:8080/some/path").protocol()).isEqualTo("https")
    assertThat(Url.absolute("http://neckar.it:8080/some/path").protocol()).isEqualTo("http")
    assertThat(Url.absolute("https://neckar.it/some/path").protocol()).isEqualTo("https")
    assertThat(Url.absolute("http://neckar.it/some/path").protocol()).isEqualTo("http")
  }

  @Test
  fun testPathPart() {
    assertThat(Url.absolute("https://neckar.it:8080/some/path").pathPart()).isEqualTo("some/path")
    assertThat(Url.absolute("https://neckar.it/some/path").pathPart()).isEqualTo("some/path")
    assertThat(Url.absolute("https://neckar.it/some/path").pathPart()).isEqualTo("some/path")
    assertThat(Url.absolute("https://neckar.it:80/some/path").pathPart()).isEqualTo("some/path")
    assertThat(Url.absolute("https://neckar.it:80/").pathPart()).isEqualTo("")
    assertThat(Url.absolute("https://neckar.it:80").pathPart()).isEqualTo("")
    assertThat(Url.absolute("https://neckar.it").pathPart()).isEqualTo("")
    assertThat(Url.absolute("https://neckar.it/").pathPart()).isEqualTo("")
  }

  @Test
  fun testHostPartWithPort(){
    assertThat(Url.absolute("https://neckar.it:8080/some/path").hostPartWithPort()).isEqualTo("neckar.it:8080")
    assertThat(Url.absolute("https://neckar.it/some/path").hostPartWithPort()).isEqualTo("neckar.it")
    assertThat(Url.absolute("http://neckar.it/some/path").hostPartWithPort()).isEqualTo("neckar.it")
    assertThat(Url.absolute("http://neckar.it:80/some/path").hostPartWithPort()).isEqualTo("neckar.it:80")
  }

  @Test
  fun testHostPart() {
    assertThat(Url.absolute("https://neckar.it/some/path").hostPart()).isEqualTo("neckar.it")
    assertThat(Url.absolute("https://neckar.it:8080/some/path").hostPart()).isEqualTo("neckar.it")
    assertThat(Url.absolute("http://neckar.it/some/path").hostPart()).isEqualTo("neckar.it")
    assertThat(Url.absolute("http://neckar.it:80/some/path").hostPart()).isEqualTo("neckar.it")
  }

  @Test
  fun testHasPort() {
    assertThat(Url.absolute("https://neckar.it:8080/some/path").hasExplicitPort()).isTrue()
    assertThat(Url.absolute("https://neckar.it:80/some/path").hasExplicitPort()).isTrue()
    assertThat(Url.absolute("http://neckar.it:80/some/path").hasExplicitPort()).isTrue()
    assertThat(Url.absolute("https://neckar.it:443/some/path").hasExplicitPort()).isTrue()

    assertThat(Url.absolute("https://neckar.it/some/path").hasExplicitPort()).isFalse()
    assertThat(Url.absolute("http://neckar.it/some/path").hasExplicitPort()).isFalse()

    //A colon in a path or a query value is no port, and port() reads the host part too.
    assertThat(Url.absolute("https://neckar.it/x?time=12:30").hasExplicitPort()).isFalse()
    assertThat(Url.absolute("https://neckar.it/a:b").hasExplicitPort()).isFalse()
  }

  @Test
  fun `a path segment after a query or fragment is rejected for every url kind`() {
    //appendUrlStrings would put the segment inside the query value.
    assertFailure { Url.absolute("https://neckar.it/search?q=a") + "page" }
      .isInstanceOf(IllegalArgumentException::class)
    assertFailure { Url.rootRelative("/search#top") + "page" }
      .isInstanceOf(IllegalArgumentException::class)
    assertFailure { Url.relative("search?q=a") + "page" }
      .isInstanceOf(IllegalArgumentException::class)

    //A further parameter and a fragment are appended as what they are.
    assertThat((Url.absolute("https://neckar.it/search?q=a") + "&page=2").value).isEqualTo("https://neckar.it/search?q=a&page=2")
    assertThat((Url.absolute("https://neckar.it/search?q=a") + "#top").value).isEqualTo("https://neckar.it/search?q=a#top")
    assertThat((Url.absolute("https://neckar.it/search") + "page").value).isEqualTo("https://neckar.it/search/page")
  }

  @Test
  fun testReplacePort() {
    assertThat(Url.absolute("https://neckar.it:8080/some/path").withForcedPort(Port(9090)).toString())
      .isEqualTo("https://neckar.it:9090/some/path")

    assertThat(Url.absolute("https://neckar.it/some/path").withForcedPort(Port(8080)).toString())
      .isEqualTo("https://neckar.it:8080/some/path")
  }

  @Test
  fun testRelativeResolveApi() {
    val relative = Url.Relative("bla")
    val template1 = relative + UrlParameterName("uuid")
    assertThat(template1.toString()).isEqualTo("bla/{uuid}")

    template1.resolve("the-uuid").let {
      assertThat(it.toString()).isEqualTo("bla/the-uuid")
    }

    val template2 = template1 + "fasel" + UrlParameterName("other")
    assertThat(template2.toString()).isEqualTo("bla/{uuid}/fasel/{other}")

    template2.resolve("the-uuid", "the-other").let {
      assertThat(it.toString()).isEqualTo("bla/the-uuid/fasel/the-other")
    }

    val template3 = template2 + "blub" + UrlParameterName("blub")
    assertThat(template3.toString()).isEqualTo("bla/{uuid}/fasel/{other}/blub/{blub}")

    template3.resolve("the-uuid", "the-other", "the-blub").let {
      assertThat(it.toString()).isEqualTo("bla/the-uuid/fasel/the-other/blub/the-blub")
    }
  }

  @Test
  fun testRelative1() {
    val uuidParameterName = UrlParameterName("uuid")

    val relativeTemplate = UrlPattern.Relative1("bla/{uuid}", uuidParameterName)
    assertThat(relativeTemplate.toString()).isEqualTo("bla/{uuid}")
    assertThat(relativeTemplate.resolve("the-uuid").toString())
      .isEqualTo("bla/the-uuid")

    val relativeTemplate2 = relativeTemplate + "fasel" + UrlParameterName("other")
    assertThat(relativeTemplate2.toString()).isEqualTo("bla/{uuid}/fasel/{other}")
    assertThat(relativeTemplate2.resolve("the-uuid", "the-other").toString())
      .isEqualTo("bla/the-uuid/fasel/the-other")

    val relativeTemplate3 = relativeTemplate2 + "blub" + UrlParameterName("blub")
    assertThat(relativeTemplate3.toString()).isEqualTo("bla/{uuid}/fasel/{other}/blub/{blub}")
    assertThat(relativeTemplate3.resolve("the-uuid", "the-other", "the-blub").toString())
      .isEqualTo("bla/the-uuid/fasel/the-other/blub/the-blub")
  }

  @Test
  fun testParameterizedToRelativeSingleParam() {
    val uuidParameterName = UrlParameterName("uuid")

    val relativeTemplate = UrlPattern.relative("bla/{uuid}", listOf(uuidParameterName))
    assertThat(relativeTemplate.toString()).isEqualTo("bla/{uuid}")

    relativeTemplate.resolve(
      mapOf(uuidParameterName to "123")
    ).let {
      assertThat(it.toString()).isEqualTo("bla/123")
    }
  }

  @Test
  fun testParameterizedToRelativeMoreApi() {
    val uuidParameterName = UrlParameterName("uuid")
    val otherParameterName = UrlParameterName("other")

    val relativeTemplate = Url.relative("bla") + uuidParameterName + "fasel" + otherParameterName
    assertThat(relativeTemplate.toString()).isEqualTo("bla/{uuid}/fasel/{other}")
    assertThat(relativeTemplate.parameterNames).containsExactly(uuidParameterName, otherParameterName)

    relativeTemplate.resolve(
      mapOf(
        uuidParameterName to "123",
        otherParameterName to "the-other",
      )
    ).let {
      assertThat(it.toString()).isEqualTo("bla/123/fasel/the-other")
    }

    relativeTemplate.resolve(
      "123", "the-other"
    ).let {
      assertThat(it.toString()).isEqualTo("bla/123/fasel/the-other")
    }
  }

  @Test
  fun testParameterizedUrl() {
    val uuidParameterName = UrlParameterName("uuid")

    assertThat(UrlPattern.relative("bla/{uuid}", listOf(uuidParameterName)).toString()).isEqualTo("bla/{uuid}")

    assertFailure {
      UrlPattern.relative("bla/{uuid}", listOf())
    }.hasMessage("The URL [bla/{uuid}] must contain at least one variable - but no variables provided")

    assertFailure {
      UrlPattern.relative("bla/{uuid}", listOf(UrlParameterName("uuid2")))
    }.hasMessage("The URL [bla/{uuid}] must contain the variable [uuid2]")


    val idParameterName = UrlParameterName("id")

    val relativeTemplate = UrlPattern.relative("bla/$uuidParameterName/foobar/$idParameterName", listOf(uuidParameterName, idParameterName))

    relativeTemplate.resolve(
      mapOf(
        uuidParameterName to "123",
        idParameterName to "the-id",
      )
    ).toString().let {
      assertThat(it).isEqualTo("bla/123/foobar/the-id")
    }
  }

  @Test
  fun testRelativeTemplateAppend() {
    val idParameterName = UrlParameterName("id")
    val uuidParameterName = UrlParameterName("uuid")

    val relativeTemplate = UrlPattern.relative("bla/$uuidParameterName/foobar/$idParameterName", listOf(uuidParameterName, idParameterName))

    (relativeTemplate + "blafasel").let {
      assertThat(it.toString()).isEqualTo("bla/{uuid}/foobar/{id}/blafasel")
      assertThat(it.parameterNames).containsExactly(uuidParameterName, idParameterName)
    }

    val appended = relativeTemplate + UrlPattern.relative("222/$uuidParameterName", listOf(uuidParameterName))

    assertThat(appended.toString()).isEqualTo("bla/{uuid}/foobar/{id}/222/{uuid}")
    assertThat(appended.parameterNames).containsExactly(uuidParameterName, idParameterName, uuidParameterName)
  }

  @Test
  fun testAppendBug() {
    val result = Url.relative("") + Url.relative("version")
    assertThat(result.value).isEqualTo("version")
  }

  @Test
  fun testAppendEmpty() {
    val appended = Url.relative("") + "foobar"
    assertThat(appended.toString()).isEqualTo("foobar")
  }

  @Test
  fun testAppendAll() {
    val relative = Url.Relative("index.html")
    val rootRelative = Url.rootRelative("/foo/bar")
    val absolute = Url.absolute("https://neckar.it")

    assertThat(absolute + rootRelative).isEqualTo(Url.absolute("https://neckar.it/foo/bar"))
    assertThat(absolute + relative).isEqualTo(Url.absolute("https://neckar.it/index.html"))
    assertThat(rootRelative + relative).isEqualTo(Url.rootRelative("/foo/bar/index.html"))
    assertThat(relative + relative).isEqualTo(Url.Relative("index.html/index.html"))
  }

  @Test
  fun testAllKinds() {
    assertThat(Url.absolute("https://neckar.it").toString()).isEqualTo("https://neckar.it")
    assertThat(Url.rootRelative("/index.html").toString()).isEqualTo("/index.html")
    assertThat(Url.Relative("index.html").toString()).isEqualTo("index.html")
    assertThat(Url.data("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAABaElEQVR42mNk").toString()).startsWith("data:image/png")
  }

  @Test
  fun testAppend() {
    val url = Url.absolute("https://neckar.it")
    assertThat(url + "/index.html").isEqualTo(Url.absolute("https://neckar.it/index.html"))
    assertThat(url + "index.html").isEqualTo(Url.absolute("https://neckar.it/index.html"))

    assertThat(url + Url.rootRelative("/index.html")).isEqualTo(Url.absolute("https://neckar.it/index.html"))
    assertThat(url + Url.Relative("index.html")).isEqualTo(Url.absolute("https://neckar.it/index.html"))
  }

  @Test
  fun testIt() {
    val url = Url.absolute("https://neckar.it")
    assertThat(url.toString()).isEqualTo("https://neckar.it")
  }

  @Test
  fun testAppend2() {
    val url = Url.absolute("file:///home/johannes/projects/com.cedarsoft.monorepo/internal/open/meistercharts/meistercharts-e2e-tests/../meistercharts-demos/meistercharts-demos-js/build/dist/js/productionExecutable/index.html")
    assertThat((url + "?a=b").value).isEqualTo("file:///home/johannes/projects/com.cedarsoft.monorepo/internal/open/meistercharts/meistercharts-e2e-tests/../meistercharts-demos/meistercharts-demos-js/build/dist/js/productionExecutable/index.html?a=b")
  }

  @Test
  fun testRelativeAbsolute() {
    val url = Url.absolute("https://neckar.it")
    assertThat(url.toString()).isEqualTo("https://neckar.it")

    val relative = Url.Relative("index.html")
    assertThat(relative.toString()).isEqualTo("index.html")
  }

  @Test
  fun testApendRelative() {
    val url: Url.Relative = Url.Relative("foo") + Url.Relative("bar")
    assertThat(url.toString()).isEqualTo("foo/bar")
  }

  // Edge Case Tests

  @Test
  fun testQueryParameters() {
    // Single query parameter
    val url1 = Url.absolute("https://example.com/path") + "?key=value"
    assertThat(url1.value).isEqualTo("https://example.com/path?key=value")

    // Multiple query parameters
    val url2 = Url.absolute("https://example.com/path") + "?key1=value1&key2=value2"
    assertThat(url2.value).isEqualTo("https://example.com/path?key1=value1&key2=value2")

    // Appending to existing query parameter
    val url3 = Url.absolute("https://example.com/path?existing=param") + "&new=param"
    assertThat(url3.value).isEqualTo("https://example.com/path?existing=param&new=param")

    // Query parameter with special characters
    val url4 = Url.absolute("https://example.com/path") + "?search=hello world"
    assertThat(url4.value).isEqualTo("https://example.com/path?search=hello world")
  }

  @Test
  fun testFragmentIdentifier() {
    // Fragment identifier (anchor)
    val url1 = Url.absolute("https://example.com/path") + "#section"
    assertThat(url1.value).isEqualTo("https://example.com/path#section")

    // Fragment with special characters
    val url2 = Url.absolute("https://example.com/path") + "#section-1.2.3"
    assertThat(url2.value).isEqualTo("https://example.com/path#section-1.2.3")
  }

  @Test
  fun testQueryParameterAndFragment() {
    // Query parameter followed by fragment
    val url1 = Url.absolute("https://example.com/path") + "?key=value" + "#section"
    assertThat(url1.value).isEqualTo("https://example.com/path?key=value#section")

    // All at once
    val url2 = Url.absolute("https://example.com/path") + "?key=value#section"
    assertThat(url2.value).isEqualTo("https://example.com/path?key=value#section")
  }

  @Test
  fun testPathParametersInTemplate() {
    // Path parameters with query
    val idParam = UrlParameterName("id")
    val template = Url.relative("users") + idParam
    val resolved = template.resolve("123")

    val withQuery = Url.absolute("https://api.example.com") + resolved + "?include=profile"
    assertThat(withQuery.value).isEqualTo("https://api.example.com/users/123?include=profile")
  }

  @Test
  fun testSpecialCharactersInPath() {
    // Spaces (should they be encoded?)
    val url1 = Url.absolute("https://example.com") + "path with spaces"
    assertThat(url1.value).isEqualTo("https://example.com/path with spaces")

    // Special characters
    val url2 = Url.absolute("https://example.com") + "path/with/special-chars_123"
    assertThat(url2.value).isEqualTo("https://example.com/path/with/special-chars_123")

    // Percent-encoded characters
    val url3 = Url.absolute("https://example.com") + "path%20with%20encoding"
    assertThat(url3.value).isEqualTo("https://example.com/path%20with%20encoding")
  }

  @Test
  fun testMultipleSlashes() {
    // Double slash in path
    val url1 = Url.absolute("https://example.com/path/") + "/subpath"
    assertThat(url1.value).isEqualTo("https://example.com/path/subpath")

    // Triple slash
    val url2 = Url.absolute("https://example.com/path//") + "//subpath"
    assertThat(url2.value).isEqualTo("https://example.com/path/subpath")

    // No slashes at boundary
    val url3 = Url.absolute("https://example.com/path") + "subpath"
    assertThat(url3.value).isEqualTo("https://example.com/path/subpath")
  }

  @Test
  fun testTrailingSlashes() {
    // Trailing slash preservation
    val url1 = Url.absolute("https://example.com/path/")
    assertThat(url1.value).isEqualTo("https://example.com/path/")

    // Appending to trailing slash
    val url2 = Url.absolute("https://example.com/path/") + "subpath"
    assertThat(url2.value).isEqualTo("https://example.com/path/subpath")

    // Root relative with trailing slash
    val url3 = Url.rootRelative("/path/")
    assertThat(url3.value).isEqualTo("/path/")

    val url4 = url3 + "subpath"
    assertThat(url4.value).isEqualTo("/path/subpath")
  }

  @Test
  fun testEmptyPathSegments() {
    // Empty string appending
    val url1 = Url.absolute("https://example.com/path") + ""
    assertThat(url1.value).isEqualTo("https://example.com/path")

    // Multiple empty strings
    val url2 = Url.relative("path") + "" + "" + "subpath"
    assertThat(url2.value).isEqualTo("path/subpath")
  }

  @Test
  fun testComplexScenarios() {
    // Complex URL with everything
    val url1 = Url.absolute("https://api.example.com:8080/v1")
    val idParam = UrlParameterName("id")
    val resourceTemplate = Url.relative("resources") + idParam + "details"

    val resolved = resourceTemplate.resolve("abc-123")
    val fullUrl = url1 + resolved + "?fields=name,age&format=json" + "#summary"

    assertThat(fullUrl.value).isEqualTo("https://api.example.com:8080/v1/resources/abc-123/details?fields=name,age&format=json#summary")
  }

  @Test
  fun testQueryParameterEdgeCases() {
    // Empty value
    val url1 = Url.absolute("https://example.com/path") + "?key="
    assertThat(url1.value).isEqualTo("https://example.com/path?key=")

    // No value at all
    val url2 = Url.absolute("https://example.com/path") + "?key"
    assertThat(url2.value).isEqualTo("https://example.com/path?key")

    // Special characters in query values
    val url3 = Url.absolute("https://example.com/path") + "?email=test@example.com"
    assertThat(url3.value).isEqualTo("https://example.com/path?email=test@example.com")

    // Equals sign in value
    val url4 = Url.absolute("https://example.com/path") + "?expression=a=b"
    assertThat(url4.value).isEqualTo("https://example.com/path?expression=a=b")
  }

  @Test
  fun testRootRelativeWithQuery() {
    val url1 = Url.rootRelative("/api/users") + "?active=true"
    assertThat(url1.value).isEqualTo("/api/users?active=true")

    val url2 = Url.rootRelative("/api/users?page=1") + "&limit=10"
    assertThat(url2.value).isEqualTo("/api/users?page=1&limit=10")
  }

  @Test
  fun testRelativeWithQuery() {
    val url1 = Url.relative("users") + "?filter=active"
    assertThat(url1.value).isEqualTo("users?filter=active")

    // Appending path after query should fail or be handled
    // This might be a bug scenario
    assertFailure {
      Url.relative("users?filter=active") + "details"
    }
  }

  @Test
  fun testPathWithDots() {
    // Relative path components
    val url1 = Url.absolute("https://example.com/path") + "../other"
    assertThat(url1.value).isEqualTo("https://example.com/path/../other")

    val url2 = Url.absolute("https://example.com/path") + "./subpath"
    assertThat(url2.value).isEqualTo("https://example.com/path/./subpath")

    // Hidden files
    val url3 = Url.absolute("https://example.com") + ".hidden"
    assertThat(url3.value).isEqualTo("https://example.com/.hidden")
  }

  @Test
  fun testPortInPathSegment() {
    // Port-like pattern in path (should not be confused with actual port)
    val url1 = Url.absolute("https://example.com") + "server:8080/status"
    assertThat(url1.value).isEqualTo("https://example.com/server:8080/status")
    assertThat(url1.port()).isEqualTo(Port.Https)
  }

  @Test
  fun testProtocolInPath() {
    // Protocol-like pattern in path
    val url1 = Url.absolute("https://example.com") + "http://redirect"
    assertThat(url1.value).isEqualTo("https://example.com/http://redirect")
  }

  @Test
  fun testConsecutiveQueryParameters() {
    // Multiple consecutive '?'
    val url1 = Url.absolute("https://example.com/path") + "??key=value"
    assertThat(url1.value).isEqualTo("https://example.com/path??key=value")

    // Multiple consecutive '&'
    val url2 = Url.absolute("https://example.com/path?a=1") + "&&b=2"
    assertThat(url2.value).isEqualTo("https://example.com/path?a=1&&b=2")
  }

  @Test
  fun testUnicodeInUrl() {
    // Unicode characters
    val url1 = Url.absolute("https://example.com") + "users/Müller"
    assertThat(url1.value).isEqualTo("https://example.com/users/Müller")

    // Emoji
    val url2 = Url.absolute("https://example.com") + "emoji/😀"
    assertThat(url2.value).isEqualTo("https://example.com/emoji/😀")
  }

  @Test
  fun testMixedSlashTypes() {
    // Backslash (should probably be converted or rejected)
    val url1 = Url.absolute("https://example.com") + "path\\subpath"
    assertThat(url1.value).isEqualTo("https://example.com/path\\subpath")
  }

  @Test
  fun testUrlFromWithStandardPortOmission() {
    // HTTP: standard port 80 omitted, non-standard included
    assertThat(Url.http("example.com", Port(80)).value).isEqualTo("http://example.com")
    assertThat(Url.http("example.com", Port(8080)).value).isEqualTo("http://example.com:8080")
    assertThat(Url.from(scheme = "http", host = "example.com", port = Port(80)).value).isEqualTo("http://example.com")
    assertThat(Url.from(scheme = "http", host = "example.com", port = Port(8080)).value).isEqualTo("http://example.com:8080")

    // HTTPS: standard port 443 omitted, non-standard included
    assertThat(Url.https("example.com", Port(443)).value).isEqualTo("https://example.com")
    assertThat(Url.https("example.com", Port(8443)).value).isEqualTo("https://example.com:8443")
    assertThat(Url.from(scheme = "https", host = "example.com", port = Port(443)).value).isEqualTo("https://example.com")
    assertThat(Url.from(scheme = "https", host = "example.com", port = Port(8443)).value).isEqualTo("https://example.com:8443")

    // Other schemes: port always included
    assertThat(Url.from(scheme = "ftp", host = "example.com", port = Port(21)).value).isEqualTo("ftp://example.com:21")
    assertThat(Url.from(scheme = "ws", host = "example.com", port = Port(8080)).value).isEqualTo("ws://example.com:8080")
  }
}
