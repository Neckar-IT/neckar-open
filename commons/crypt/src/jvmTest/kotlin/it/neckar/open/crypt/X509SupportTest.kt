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
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import it.neckar.open.resources.getResourceSafe
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.security.cert.X509Certificate
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Tests for [X509Support] companion object methods.
 */
@OptIn(ExperimentalEncodingApi::class)
class X509SupportTest {

  @Test
  fun `readCertificate should load certificate from Base64-encoded PEM`() {
    val pemContent = javaClass.getResourceSafe("/test.crt").readText()
    val base64Encoded = Base64.encode(pemContent.toByteArray())
    val base64Cert = Base64CertificatePem(base64Encoded)

    val certificate = X509Support.readCertificate(base64Cert)

    assertThat(certificate).isNotNull()
    assertThat(certificate).isInstanceOf(X509Certificate::class)
    assertThat(certificate.subjectX500Principal.name).isEqualTo("CN=invoices-cedarsoft,L=Gomaringen,ST=BW,C=DE")
  }

  @Test
  fun `readCertificate from Path should load certificate`() {
    val certUrl = javaClass.getResourceSafe("/test.crt")
    val certificate = X509Support.readCertificate(certUrl)

    assertThat(certificate).isNotNull()
    assertThat(certificate).isInstanceOf(X509Certificate::class)
    assertThat(certificate.subjectX500Principal.name).isEqualTo("CN=invoices-cedarsoft,L=Gomaringen,ST=BW,C=DE")
  }

  @Test
  fun `readCertificates should load multiple certificates from multi-cert PEM`() {
    val certPath = Path.of(javaClass.getResourceSafe("/multi-cert.pem").toURI())
    val certificates = X509Support.readCertificates(certPath)

    assertThat(certificates).hasSize(2)
    assertThat(certificates[0].subjectX500Principal.name).isEqualTo("CN=invoices-cedarsoft,L=Gomaringen,ST=BW,C=DE")
    assertThat(certificates[1].subjectX500Principal.name).isEqualTo("CN=invoices-cedarsoft,L=Gomaringen,ST=BW,C=DE")
  }

  @Test
  fun `readCertificates should work with single certificate PEM`() {
    val certPath = Path.of(javaClass.getResourceSafe("/test.crt").toURI())
    val certificates = X509Support.readCertificates(certPath)

    assertThat(certificates).hasSize(1)
    assertThat(certificates[0].subjectX500Principal.name).isEqualTo("CN=invoices-cedarsoft,L=Gomaringen,ST=BW,C=DE")
  }

  @Test
  fun `readCertificates from byte array should load multiple certificates`() {
    val pemBytes = javaClass.getResourceSafe("/multi-cert.pem").readBytes()
    val certificates = X509Support.readCertificates(pemBytes)

    assertThat(certificates).hasSize(2)
    assertThat(certificates[0].subjectX500Principal.name).isEqualTo("CN=invoices-cedarsoft,L=Gomaringen,ST=BW,C=DE")
    assertThat(certificates[1].subjectX500Principal.name).isEqualTo("CN=invoices-cedarsoft,L=Gomaringen,ST=BW,C=DE")
  }
}
