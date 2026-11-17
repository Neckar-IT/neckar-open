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
package it.neckar.commons.kotlin.js

import assertk.*
import assertk.assertions.*
import it.neckar.commons.js.CookieName
import kotlin.test.Test

class CookiesSupportTest {
  @Test
  fun testEmptyCookies() {
    assertThat(CookiesSupport.findCookieValue("", CookieName("foobar"))).isNull()
    assertThat(CookiesSupport.findCookieValue("a=b", CookieName("foobar"))).isNull()
  }

  @Test
  fun testFindCookieValueSkipsInvalidTokenAndContinues() {
    // Two valid cookies with one malformed token in between (no '=', or extra '=' producing 3 parts).
    // Before the fix this threw IllegalStateException on the first invalid token, breaking every
    // subsequent lookup.
    val cookies = "first=alpha; broken; target=found; otherwise=tail"
    assertThat(CookiesSupport.findCookieValue(cookies, CookieName("target"))).isEqualTo("found")
    assertThat(CookiesSupport.findCookieValue(cookies, CookieName("first"))).isEqualTo("alpha")
    assertThat(CookiesSupport.findCookieValue(cookies, CookieName("missing"))).isNull()
  }

  @Test
  fun testFindCookieValueWithEqualsSignInValue() {
    // Cookie values legitimately contain '=' (base64 padding, JWTs). Splitting on every '=' used to
    // discard these as "invalid"; only the first '=' separates key from value.
    val cookies = "session=YWJjZA==; jwt=a.b.c=d"
    assertThat(CookiesSupport.findCookieValue(cookies, CookieName("session"))).isEqualTo("YWJjZA==")
    assertThat(CookiesSupport.findCookieValue(cookies, CookieName("jwt"))).isEqualTo("a.b.c=d")
  }
}
