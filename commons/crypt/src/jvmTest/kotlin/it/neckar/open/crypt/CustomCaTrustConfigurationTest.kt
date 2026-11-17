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
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameInstanceAs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.security.KeyStore
import javax.net.ssl.SSLContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.io.path.readText

/**
 * Tests for [CustomCaTrustConfiguration]
 */
@OptIn(ExperimentalEncodingApi::class)
class CustomCaTrustConfigurationTest {

  @Test
  fun `addToJvmDefaultTrustStore from path sets JVM default`(@TempDir tempDir: Path) {
    val certPath: Path = createTestCertificate(tempDir)
    val originalDefault: SSLContext = SSLContext.getDefault()

    CustomCaTrustConfiguration.addToJvmDefaultTrustStore(certPath)

    val newDefault: SSLContext = SSLContext.getDefault()
    assertThat(newDefault).isNotSameInstanceAs(originalDefault)
    assertThat(newDefault.protocol).isEqualTo("TLS")
  }

  @Test
  fun `addToJvmDefaultTrustStore from Base64 sets JVM default`(@TempDir tempDir: Path) {
    val certPath: Path = createTestCertificate(tempDir)
    val pemContent: String = certPath.readText()
    val base64EncodedPem: String = Base64.encode(pemContent.toByteArray())

    CustomCaTrustConfiguration.addToJvmDefaultTrustStore(Base64CertificatePem(base64EncodedPem))

    val newDefault: SSLContext = SSLContext.getDefault()
    assertThat(newDefault.protocol).isEqualTo("TLS")
  }

  @Test
  fun `addToJvmDefaultTrustStore from X509Certificate sets JVM default`(@TempDir tempDir: Path) {
    val certPath: Path = createTestCertificate(tempDir)
    val certificate = X509Support.readCertificate(certPath)
    val originalDefault: SSLContext = SSLContext.getDefault()

    CustomCaTrustConfiguration.addToJvmDefaultTrustStore(certificate)

    val newDefault: SSLContext = SSLContext.getDefault()
    assertThat(newDefault).isNotSameInstanceAs(originalDefault)
    assertThat(newDefault.protocol).isEqualTo("TLS")
  }

  @Test
  fun `createSslContextWithCustomCa includes system CAs and custom CA`(@TempDir tempDir: Path) {
    val certPath: Path = createTestCertificate(tempDir)
    val certificate = X509Support.readCertificate(certPath)

    val systemCaCount: Int = CustomCaTrustConfiguration.loadSystemTrustStore().size()

    val sslContext: SSLContext = CustomCaTrustConfiguration.createSslContextWithCustomCa(certificate)

    assertThat(sslContext.protocol).isEqualTo("TLS")
    assertThat(sslContext.socketFactory).isNotNull()

    // createTrustStoreWithCustomCa should contain system CAs + 1 custom CA
    val combinedStore: KeyStore = CustomCaTrustConfiguration.createTrustStoreWithCustomCa(certificate)
    assertThat(combinedStore.size()).isEqualTo(systemCaCount + 1)
  }

  @Test
  fun `loadSystemTrustStore loads system CAs`() {
    val trustStore: KeyStore = CustomCaTrustConfiguration.loadSystemTrustStore()

    assertThat(trustStore.size()).isGreaterThan(0)
  }

  @Test
  fun `createTrustStoreWithCustomCa adds custom CA to system CAs`(@TempDir tempDir: Path) {
    val certPath: Path = createTestCertificate(tempDir)
    val certificate = X509Support.readCertificate(certPath)

    val systemCaCount: Int = CustomCaTrustConfiguration.loadSystemTrustStore().size()
    val trustStore: KeyStore = CustomCaTrustConfiguration.createTrustStoreWithCustomCa(certificate)

    assertThat(trustStore.size()).isEqualTo(systemCaCount + 1)
    assertThat(trustStore.getCertificate("custom-ca")).isEqualTo(certificate)
  }

  @Test
  fun `sequential addToJvmDefaultTrustStore calls do not accumulate previous custom CAs`(@TempDir tempDir: Path) {
    val systemCaCount: Int = CustomCaTrustConfiguration.loadSystemTrustStore().size()

    val cert1Path: Path = createTestCertificate(tempDir, "cert1")
    val cert2Path: Path = createTestCertificate(tempDir, "cert2")

    CustomCaTrustConfiguration.addToJvmDefaultTrustStore(cert1Path)

    // After second call, the trust store should contain system CAs + cert2 only, not system CAs + cert1 + cert2
    CustomCaTrustConfiguration.addToJvmDefaultTrustStore(cert2Path)

    val trustStoreAfterSecondCall: KeyStore = CustomCaTrustConfiguration.createTrustStoreWithCustomCa(X509Support.readCertificate(cert2Path))
    assertThat(trustStoreAfterSecondCall.size()).isEqualTo(systemCaCount + 1)
  }

  /**
   * Creates a self-signed test certificate using keytool. Returns the path to the PEM file.
   *
   * [name] is used to distinguish multiple certificates in the same test.
   */
  private fun createTestCertificate(tempDir: Path, name: String = "testca"): Path {
    val keystorePath: Path = tempDir.resolve("$name-keystore.p12")
    val certPath: Path = tempDir.resolve("$name.pem")

    val generateProcess: Process = ProcessBuilder(
      "keytool",
      "-genkeypair",
      "-keyalg", "RSA",
      "-keysize", "2048",
      "-validity", "1",
      "-dname", "CN=$name, O=Test, L=Test, ST=Test, C=DE",
      "-keystore", keystorePath.toString(),
      "-storetype", "PKCS12",
      "-storepass", "testpass",
      "-keypass", "testpass",
      "-alias", name,
    ).start()

    val generateExitCode: Int = generateProcess.waitFor()
    require(generateExitCode == 0) { "Failed to generate test certificate" }

    val exportProcess: Process = ProcessBuilder(
      "keytool",
      "-exportcert",
      "-alias", name,
      "-keystore", keystorePath.toString(),
      "-storetype", "PKCS12",
      "-storepass", "testpass",
      "-rfc",
      "-file", certPath.toString(),
    ).start()

    val exportExitCode: Int = exportProcess.waitFor()
    require(exportExitCode == 0) { "Failed to export test certificate" }

    return certPath
  }
}
