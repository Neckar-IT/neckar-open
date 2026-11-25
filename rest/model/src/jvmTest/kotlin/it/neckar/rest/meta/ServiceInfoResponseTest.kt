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
package it.neckar.rest.meta

import assertk.*
import assertk.assertions.*
import it.neckar.gradle.git.GitHash
import it.neckar.gradle.git.GitHashShort
import it.neckar.open.time.TimeInstantExample
import it.neckar.rest.error.GlobalHealth
import it.neckar.rest.health.ServiceStateDto
import it.neckar.runtime.context.DeploymentStage
import it.neckar.runtime.context.ExecutionEnvironment
import it.neckar.runtime.context.Hostname
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.time.Instant

/**
 * Tests for [ServiceInfoResponse] serialization
 */
class ServiceInfoResponseTest {
  private val versionInfo: MetaTags = MetaTags(
    gitHash = GitHash("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"),
    gitHashShort = GitHashShort("a1b2c3d4e5f6"),
    gitCommitDateTime = Instant.parse("2026-01-20T09:15:00Z"),
    imageDigest = ImageDigest("sha256:ad2453b02334597670fc64befad42c10ec0870d092c58b8ae0ca6ef1e903d221"),
  )

  @Test
  fun testSerializationAndDeserialization() {
    val serviceInfo = ServiceInfo(
      serviceName = "Test Service",
      version = "1.0.0",
      versionInfo = versionInfo,
      runtimeContext = RuntimeContextDetails(
        deploymentStage = DeploymentStage.Development,
        executionEnvironment = ExecutionEnvironment.LocalDev,
        hostname = Hostname("localhost"),
        debugMode = false,
        inUnitTest = true,
      ),
      health = GlobalHealth.Healthy,
    )

    val response = ServiceInfoResponse.good(serviceInfo)
    assertThat(response).isNotNull()

    val data = response.asGood().data
    assertThat(data.serviceName).isEqualTo("Test Service")
    assertThat(data.version).isEqualTo("1.0.0")
    assertThat(data.versionInfo).isEqualTo(versionInfo)
    assertThat(data.runtimeContext.deploymentStage).isEqualTo(DeploymentStage.Development)
    assertThat(data.runtimeContext.executionEnvironment).isEqualTo(ExecutionEnvironment.LocalDev)
    assertThat(data.runtimeContext.hostname).isEqualTo(Hostname("localhost"))
    assertThat(data.runtimeContext.inUnitTest).isTrue()
    assertThat(data.health).isEqualTo(GlobalHealth.Healthy)
  }

  @Test
  fun testJsonSerialization() {
    val serviceInfo = ServiceInfo(
      serviceName = "Test Service",
      version = "1.0.0",
      versionInfo = versionInfo,
      runtimeContext = RuntimeContextDetails(
        deploymentStage = DeploymentStage.Production,
        executionEnvironment = ExecutionEnvironment.Docker,
        hostname = Hostname("app.turboangebot.de"),
        debugMode = false,
        inUnitTest = false,
      ),
      health = GlobalHealth.Healthy,
    )

    val response = ServiceInfoResponse.good(serviceInfo)

    val json = Json.encodeToString(response)
    assertThat(json).isNotEmpty()
    assertThat(json).contains("\"serviceName\":\"Test Service\"")
    assertThat(json).contains("\"version\":\"1.0.0\"")
    //The embedded views stay nested - they are not flattened into the enclosing object
    assertThat(json).contains("\"versionInfo\":{")
    assertThat(json).contains("\"runtimeContext\":{\"deploymentStage\":\"Production\",\"executionEnvironment\":\"Docker\"")

    val deserialized = Json.decodeFromString<ServiceInfoResponse>(json)
    assertThat(deserialized.asGood().data).isEqualTo(serviceInfo)
  }

  @Test
  fun testWithErrorHealth() {
    val serviceInfo = ServiceInfo(
      serviceName = "Test Service",
      version = "1.0.0",
      versionInfo = versionInfo,
      runtimeContext = RuntimeContextDetails(
        deploymentStage = DeploymentStage.Staging,
        executionEnvironment = ExecutionEnvironment.Docker,
        hostname = Hostname("staging.example.com"),
        debugMode = false,
        inUnitTest = false,
      ),
      health = GlobalHealth.Unhealthy(
        message = "Database connection failed",
        state = ServiceStateDto.DependencyUnavailable,
        errorType = "java.sql.SQLException",
        errorCount = 1,
        timestamp = TimeInstantExample._2026_04_24_03_10_00_000Z
      ),
    )

    val response = ServiceInfoResponse.good(serviceInfo)

    val data = response.asGood().data
    assertThat(data.health).isInstanceOf(GlobalHealth.Unhealthy::class)
    val errorHealth = data.health as GlobalHealth.Unhealthy
    assertThat(errorHealth.message).isEqualTo("Database connection failed")
  }
}
