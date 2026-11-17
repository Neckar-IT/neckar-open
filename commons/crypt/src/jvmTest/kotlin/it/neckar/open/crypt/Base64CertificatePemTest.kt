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
package it.neckar.open.crypt

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Tests for [Base64CertificatePem] value class validation.
 */
@OptIn(ExperimentalEncodingApi::class)
class Base64CertificatePemTest {

  @Test
  fun `should reject blank input`() {
    val exception = assertThrows<IllegalArgumentException> {
      Base64CertificatePem("")
    }
    assertThat(exception.message).isNotNull().contains("must not be blank")
  }

  @Test
  fun `should reject whitespace-only input`() {
    val exception = assertThrows<IllegalArgumentException> {
      Base64CertificatePem("   ")
    }
    assertThat(exception.message).isNotNull().contains("must not be blank")
  }

  @Test
  fun `should reject invalid Base64`() {
    val exception = assertThrows<IllegalArgumentException> {
      Base64CertificatePem("not-valid-base64!!!")
    }
    assertThat(exception.message).isNotNull().contains("Invalid Base64 encoding")
  }

  @Test
  fun `should accept valid Base64-encoded PEM certificate`() {
    // Create a valid PEM certificate and Base64-encode it
    val pemContent = """
      -----BEGIN CERTIFICATE-----
      MIIFKTCCA5GgAwIBAgIUB6Ou2wPXsJ88tkYhciA45/d6CbEwDQYJKoZIhvcNAQEN
      BQAwgZoxCzAJBgNVBAYTAkRFMQswCQYDVQQIDAJCVzESMBAGA1UEBwwJU3R1dHRn
      YXJ0MR0wGwYDVQQKDBRBdGxhcyBDb3BjbyBJQVMgR21iSDEdMBsGA1UECwwUU29m
      dHdhcmUgRGV2ZWxvcG1lbnQxLDAqBgNVBAMMI0F0bGFzIENvcGNvIElBUyBEZXZl
      bG9wbWVudCBDQSBSb290MCAXDTI1MDIwMjIyMjY1NFoYDzIxMjUwMTA5MjIyNjU0
      WjCBmjELMAkGA1UEBhMCREUxCzAJBgNVBAgMAkJXMRIwEAYDVQQHDAlTdHV0dGdh
      cnQxHTAbBgNVBAoMFEF0bGFzIENvcGNvIElBUyBHbWJIMR0wGwYDVQQLDBRTb2Z0
      d2FyZSBEZXZlbG9wbWVudDEsMCoGA1UEAwwjQXRsYXMgQ29wY28gSUFTIERldmVs
      b3BtZW50IENBIFJvb3QwggGiMA0GCSqGSIb3DQEBAQUAA4IBjwAwggGKAoIBgQCp
      iiNm80kszdID3rk27w6wC99ecvSyVgwTcY5MjyAoUVKnkNf6O78Ofep3sO2Qv1Md
      qH4ZUvVN2KvQMwG/gfzNDQEAu9FdXi2uGvk2VAGW6jZs3dr8HUOu20Rdvci+fmL6
      -----END CERTIFICATE-----
    """.trimIndent()

    val base64Encoded = Base64.encode(pemContent.toByteArray())

    // Should not throw
    val cert = Base64CertificatePem(base64Encoded)
    assertThat(cert.value).isEqualTo(base64Encoded)
  }

  @Test
  fun `should reject PEM content with invalid header`() {
    // Create content that starts with -----BEGIN but doesn't have CERTIFICATE
    val invalidPemContent = """
      -----BEGIN PRIVATE KEY-----
      some content here
      -----END PRIVATE KEY-----
    """.trimIndent()

    val base64Encoded = Base64.encode(invalidPemContent.toByteArray())

    val exception = assertThrows<IllegalArgumentException> {
      Base64CertificatePem(base64Encoded)
    }
    assertThat(exception.message).isNotNull().contains("valid certificate header")
  }

  @Test
  fun `should accept Base64-encoded trusted certificate`() {
    val trustedPemContent = """
      -----BEGIN TRUSTED CERTIFICATE-----
      MIIFKTCCA5GgAwIBAgIUB6Ou2wPXsJ88tkYhciA45/d6CbEwDQYJKoZIhvcNAQEN
      BQAwgZoxCzAJBgNVBAYTAkRFMQswCQYDVQQIDAJCVzESMBAGA1UEBwwJU3R1dHRn
      -----END TRUSTED CERTIFICATE-----
    """.trimIndent()

    val base64Encoded = Base64.encode(trustedPemContent.toByteArray())

    // Should not throw
    val cert = Base64CertificatePem(base64Encoded)
    assertThat(cert.value).isEqualTo(base64Encoded)
  }

  @Test
  fun `companion object constants should have correct values`() {
    assertThat(Base64CertificatePem.BeginCertificate).isEqualTo("-----BEGIN CERTIFICATE-----")
    assertThat(Base64CertificatePem.EndCertificate).isEqualTo("-----END CERTIFICATE-----")
    assertThat(Base64CertificatePem.BeginTrustedCertificate).isEqualTo("-----BEGIN TRUSTED CERTIFICATE-----")
    assertThat(Base64CertificatePem.EndTrustedCertificate).isEqualTo("-----END TRUSTED CERTIFICATE-----")
  }

  @Test
  fun `decode should return raw bytes`() {
    val pemContent = """
      -----BEGIN CERTIFICATE-----
      MIIFKTCCA5GgAwIBAgIUB6Ou2wPXsJ88tkYhciA45/d6CbEwDQYJKoZIhvcNAQEN
      -----END CERTIFICATE-----
    """.trimIndent()

    val base64Encoded = Base64.encode(pemContent.toByteArray())
    val cert = Base64CertificatePem(base64Encoded)

    val decoded = cert.decode()
    assertThat(decoded).isEqualTo(pemContent.toByteArray())
  }

  @Test
  fun `decodeToString should return PEM content as string`() {
    val pemContent = """
      -----BEGIN CERTIFICATE-----
      MIIFKTCCA5GgAwIBAgIUB6Ou2wPXsJ88tkYhciA45/d6CbEwDQYJKoZIhvcNAQEN
      -----END CERTIFICATE-----
    """.trimIndent()

    val base64Encoded = Base64.encode(pemContent.toByteArray())
    val cert = Base64CertificatePem(base64Encoded)

    val decodedString = cert.decodeToString()
    assertThat(decodedString).isEqualTo(pemContent)
  }
}
