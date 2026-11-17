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
package it.neckar.otel

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.ClearSystemProperty
import org.junit.jupiter.api.util.SetSystemProperty

@ClearSystemProperty(key = "DEPLOYMENT_STAGE")
@ClearSystemProperty(key = "EXECUTION_ENVIRONMENT")
@ClearSystemProperty(key = "SERVICE_HOST")
@ClearSystemProperty(key = "APP_IMAGE_DIGEST")
class NeckarRuntimeContextResourceProviderTest {

  private val emptyConfig: ConfigProperties = object : ConfigProperties {
    override fun getString(name: String): String? = null
    override fun getBoolean(name: String): Boolean? = null
    override fun getInt(name: String): Int? = null
    override fun getLong(name: String): Long? = null
    override fun getDouble(name: String): Double? = null
    override fun getDuration(name: String): java.time.Duration? = null
    override fun getList(name: String): List<String> = emptyList()
    override fun getMap(name: String): Map<String, String> = emptyMap()
  }

  @Test
  @SetSystemProperty(key = "DEPLOYMENT_STAGE", value = "Production")
  @SetSystemProperty(key = "EXECUTION_ENVIRONMENT", value = "Docker")
  @SetSystemProperty(key = "SERVICE_HOST", value = "patterns.neckar.it")
  fun `derives all axes from the runtime-context environment`() {
    val resource = NeckarRuntimeContextResourceProvider().createResource(emptyConfig)

    assertThat(resource.getAttribute(NeckarResourceAttributes.DeploymentEnvironment)).isEqualTo("production")
    assertThat(resource.getAttribute(NeckarResourceAttributes.ServiceNamespace)).isEqualTo("neckar-it")
    assertThat(resource.getAttribute(NeckarResourceAttributes.ExecutionEnvironment)).isEqualTo("Docker")
    assertThat(resource.getAttribute(NeckarResourceAttributes.ServiceHost)).isEqualTo("patterns.neckar.it")
  }

  @Test
  @SetSystemProperty(key = "DEPLOYMENT_STAGE", value = "Development")
  fun `deployment environment is lowercased`() {
    val resource = NeckarRuntimeContextResourceProvider().createResource(emptyConfig)

    assertThat(resource.getAttribute(NeckarResourceAttributes.DeploymentEnvironment)).isEqualTo("development")
  }

  @Test
  fun `always sets the service namespace, omits absent axes`() {
    val resource = NeckarRuntimeContextResourceProvider().createResource(emptyConfig)

    assertThat(resource.getAttribute(NeckarResourceAttributes.ServiceNamespace)).isEqualTo("neckar-it")
    assertThat(resource.getAttribute(NeckarResourceAttributes.DeploymentEnvironment)).isNull()
    assertThat(resource.getAttribute(NeckarResourceAttributes.ExecutionEnvironment)).isNull()
    assertThat(resource.getAttribute(NeckarResourceAttributes.ServiceHost)).isNull()
  }

  @Test
  @SetSystemProperty(key = "APP_IMAGE_DIGEST", value = "sha256:ad2453b02334597670fc64befad42c10ec0870d092c58b8ae0ca6ef1e903d221")
  fun `service version is the short image digest`() {
    val resource = NeckarRuntimeContextResourceProvider().createResource(emptyConfig)

    assertThat(resource.getAttribute(NeckarResourceAttributes.ServiceVersion)).isEqualTo("ad2453b02334")
  }

  @Test
  @SetSystemProperty(key = "APP_IMAGE_DIGEST", value = "unpinned")
  fun `an unpinned image sets no service version`() {
    val resource = NeckarRuntimeContextResourceProvider().createResource(emptyConfig)

    assertThat(resource.getAttribute(NeckarResourceAttributes.ServiceVersion)).isNull()
  }

  @Test
  @SetSystemProperty(key = "APP_IMAGE_DIGEST", value = "sha256:not-hex")
  fun `a malformed image digest sets no service version`() {
    val resource = NeckarRuntimeContextResourceProvider().createResource(emptyConfig)

    assertThat(resource.getAttribute(NeckarResourceAttributes.ServiceVersion)).isNull()
  }
}
