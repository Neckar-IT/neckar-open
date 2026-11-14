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
package it.neckar.rest.jwt

import it.neckar.open.test.utils.JsonUtils
import org.junit.jupiter.api.Test

class JwtTokenTest {

  @Test
  fun testParsing() {
    // Synthetic token: neutral claims, signed HS256 with the test-only secret "test-only-signing-key".
    val tokenContent =
      "eyJraWQiOiJ0ZXN0LWtleS1pZCIsImFsZyI6IkhTMjU2IiwidHlwIjoiSldUIn0.eyJqdGkiOiIwMDAwMDAwMC0wMDAwLTQwMDAtODAwMC0wMDAwMDAwMDAwMDEiLCJpYXQiOjE3MDAwMDAwMDAsImlzcyI6ImF1dGguZXhhbXBsZS5jb20iLCJzdWIiOiJ0ZXN0LXVzZXIiLCJhdWQiOiJhcHAuZXhhbXBsZS5jb20iLCJjcCI6Ik06dGVzdC1jb21wYW55IiwiYXIiOlsiRWRpdEN1c3RvbWVycyIsIlZpZXdQcmljZUxpc3QiXSwibmJmIjoxNzAwMDAwMDAwLCJleHAiOjE3MDAwMDA2MDAsInR1IjoiQWNjcyJ9.w2vH8G5Op575TQE-hTj30ZCPdOn831Q7eq62acJyF6c\n" // gitleaks:allow

    val parsed = JwtToken(tokenContent).extractJwtPayload()

    JsonUtils.assertJsonEquals(
      //language=JSON
      """
      {
        "jti": "00000000-0000-4000-8000-000000000001",
        "iat": 1700000000,
        "iss": "auth.example.com",
        "sub": "test-user",
        "aud": "app.example.com",
        "cp": "M:test-company",
        "ar": [
          "EditCustomers",
          "ViewPriceList"
        ],
        "nbf": 1700000000,
        "exp": 1700000600,
        "tu": "Accs"
      }
    """.trimIndent(), parsed
    )
  }
}
