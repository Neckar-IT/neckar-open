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
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

/**
 *
 */
class UrlPatternTest {
  @Test
  fun `appending a parameterless pattern keeps a pattern without variables`() {
    val base: UrlPattern.Relative0 = UrlPattern.relative("api")
    //Typed as the interface, so overload resolution picks plus(UrlPattern) — which built a
    //RelativeMulti, and that requires at least one variable.
    val leaf: UrlPattern.Relative = UrlPattern.relative("health")

    val combined: UrlPattern.Relative = base + leaf

    assertThat(combined.value).isEqualTo("api/health")
    assertThat(combined.parameterNames).isEmpty()
  }

  @Test
  fun testResolveApi() {
    Url.pattern("foo-bar").let {
      assertThat(it.toString()).isEqualTo("foo-bar")
      assertThat(it.resolve().toString()).isEqualTo("foo-bar")
    }

    (Url.pattern("foo-bar") + UrlParameterName("bar")).let {
      assertThat(it.toString()).isEqualTo("foo-bar/{bar}")
      assertThat(it.resolve("val1").toString()).isEqualTo("foo-bar/val1")
      it.resolve(Uuid.random())
    }

    (Url.pattern("foo-bar") + UrlParameterName("bar") + UrlParameterName("foo")).let {
      assertThat(it.toString()).isEqualTo("foo-bar/{bar}/{foo}")
      assertThat(it.resolve("val1", "val2").toString()).isEqualTo("foo-bar/val1/val2")
      it.resolve(Uuid.random(), Uuid.random())
    }

    (Url.pattern("foo-bar") + UrlParameterName("one") + UrlParameterName("two") + UrlParameterName("three")).let {
      assertThat(it).isInstanceOf(UrlPattern.Relative3::class)
      assertThat(it.toString()).isEqualTo("foo-bar/{one}/{two}/{three}")
      assertThat(it.resolve("v1", "v2", "v3").toString()).isEqualTo("foo-bar/v1/v2/v3")
    }

    // A fourth parameter keeps the arity in the type instead of falling back to RelativeMulti:
    // the sub-resource of a sub-resource of an embedded row takes exactly four ids.
    (Url.pattern("foo-bar") + UrlParameterName("one") + UrlParameterName("two") + UrlParameterName("three") + UrlParameterName("four")).let {
      assertThat(it).isInstanceOf(UrlPattern.Relative4::class)
      assertThat(it.toString()).isEqualTo("foo-bar/{one}/{two}/{three}/{four}")
      assertThat(it.parameterNames).hasSize(4)
      assertThat(it.resolve("v1", "v2", "v3", "v4").toString()).isEqualTo("foo-bar/v1/v2/v3/v4")
      assertThat((it + "leaf").toString()).isEqualTo("foo-bar/{one}/{two}/{three}/{four}/leaf")
      it.resolve(Uuid.random(), Uuid.random(), Uuid.random(), Uuid.random())
    }
  }

  @Test
  fun testBuilder() {
    assertThat(
      UrlPattern.builder().build()
    ).isEqualTo(UrlPattern.relative(""))

    assertThat(
      UrlPattern.builder()
        .append("foo")
        .build()
    ).isEqualTo(UrlPattern.relative("foo"))

    run {
      val pattern = UrlPattern.builder()
        .append("foo")
        .append(UrlParameterName("bar"))
        .append("bla")
        .build()

      assertThat(pattern.toString()).isEqualTo("foo/{bar}/bla")
      assertThat(pattern.parameterNames).hasSize(1)
      assertThat(pattern).isInstanceOf(UrlPattern.Relative1::class)
    }

    run {
      val pattern = UrlPattern.builder()
        .append("foo")
        .append(UrlParameterName("bar"))
        .append(UrlParameterName("bar2"))
        .append("bla")
        .build()

      assertThat(pattern.toString()).isEqualTo("foo/{bar}/{bar2}/bla")
      assertThat(pattern.parameterNames).hasSize(2)
      assertThat(pattern).isInstanceOf(UrlPattern.Relative2::class)
    }
  }
}
