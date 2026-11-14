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
package it.neckar.auth.rest

import it.neckar.open.annotations.Effective
import it.neckar.open.http.Url
import it.neckar.open.http.UrlPattern

/**
 * Auth endpoints that can be bound to different base URLs.
 *
 * Execute `AuthEndpointsEffectiveTest` to update the @Effective annotations
 */
object AuthEndpoints {
  @Effective("register")
  val Register: UrlPattern.Relative0 = Url.pattern("register")

  @Effective("login")
  val Login: UrlPattern.Relative0 = Url.pattern("login")

  @Effective("login-with-token")
  val LoginWithToken: UrlPattern.Relative0 = Url.pattern("login-with-token")

  @Effective("logout")
  val Logout: UrlPattern.Relative0 = Url.pattern("logout")

  @Effective("refresh-access-token")
  val RefreshAccessToken: UrlPattern.Relative0 = Url.pattern("refresh-access-token")

  @Effective("verify")
  val Verify: UrlPattern.Relative0 = Url.pattern("verify")

  @Effective("verify/access-token")
  val Verify_AccessToken: UrlPattern.Relative0 = Verify + "access-token"

  @Effective("verify/refresh-token")
  val Verify_RefreshToken: UrlPattern.Relative0 = Verify + "refresh-token"

  @Effective("dump")
  val Dump: UrlPattern.Relative0 = Url.pattern("dump")

  @Effective("dump/access-token")
  val Dump_AccessToken: UrlPattern.Relative0 = Dump + "access-token"

  @Effective("dump/refresh-token")
  val Dump_RefreshToken: UrlPattern.Relative0 = Dump + "refresh-token"

  /**
   * Creates auth endpoints bound to a specific base URL.
   * Use this for service-specific auth endpoint paths.
   *
   * Example:
   * ```
   * val emAuth = AuthEndpoints.forBaseUrl(Url.pattern("api/elektromeister/auth"))
   * routing {
   *   get(emAuth.Login) { ... }
   * }
   * ```
   */
  fun forBaseUrl(baseUrl: UrlPattern.Relative0): Paths = Paths(baseUrl)

  class Paths(private val baseUrl: UrlPattern.Relative0) {
    val Register: UrlPattern.Relative0 = baseUrl + AuthEndpoints.Register
    fun registerUrl(): Url.Relative = Register.resolve()

    val Login: UrlPattern.Relative0 = baseUrl + AuthEndpoints.Login
    fun loginUrl(): Url.Relative = Login.resolve()

    val LoginWithToken: UrlPattern.Relative0 = baseUrl + AuthEndpoints.LoginWithToken
    fun loginWithTokenUrl(): Url.Relative = LoginWithToken.resolve()

    val Logout: UrlPattern.Relative0 = baseUrl + AuthEndpoints.Logout
    fun logoutUrl(): Url.Relative = Logout.resolve()

    val RefreshAccessToken: UrlPattern.Relative0 = baseUrl + AuthEndpoints.RefreshAccessToken
    fun refreshAccessTokenUrl(): Url.Relative = RefreshAccessToken.resolve()

    /**
     * The `Path` of the refresh-token cookie: the browser sends the refresh token to the refresh
     * endpoint and nowhere else, so no other route ever sees it.
     */
    val refreshTokenCookiePath: Url.RootRelative = Url.root + refreshAccessTokenUrl()

    val Verify: UrlPattern.Relative0 = baseUrl + AuthEndpoints.Verify
    fun verifyUrl(): Url.Relative = Verify.resolve()

    val Verify_AccessToken: UrlPattern.Relative0 = baseUrl + AuthEndpoints.Verify_AccessToken
    fun verifyAccessTokenUrl(): Url.Relative = Verify_AccessToken.resolve()

    val Verify_RefreshToken: UrlPattern.Relative0 = baseUrl + AuthEndpoints.Verify_RefreshToken
    fun verifyRefreshTokenUrl(): Url.Relative = Verify_RefreshToken.resolve()

    val Dump: UrlPattern.Relative0 = baseUrl + AuthEndpoints.Dump
    fun dumpUrl(): Url.Relative = Dump.resolve()

    val Dump_AccessToken: UrlPattern.Relative0 = baseUrl + AuthEndpoints.Dump_AccessToken
    fun dumpAccessTokenUrl(): Url.Relative = Dump_AccessToken.resolve()

    val Dump_RefreshToken: UrlPattern.Relative0 = baseUrl + AuthEndpoints.Dump_RefreshToken
    fun dumpRefreshTokenUrl(): Url.Relative = Dump_RefreshToken.resolve()
  }
}
