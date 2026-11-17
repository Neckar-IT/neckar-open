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
package it.neckar.rest.openapi

import assertk.*
import assertk.assertions.*
import io.ktor.http.*
import it.neckar.open.http.UrlParameterName
import it.neckar.open.http.UrlPathSegments
import it.neckar.open.http.UrlPattern
import org.junit.jupiter.api.Test

/**
 *
 */
class OperationIdGeneratorTest {
  @Test
  fun testPathSegment() {
    val projectIdParameterName = UrlParameterName("projectId")
    val configurationIdParameterName = UrlParameterName("projectConfigurationId")
    val additionalInstallationsIdParameterName = UrlParameterName("additionalInstallationsIdParameterName")

    val pathPattern = UrlPattern.relative("/api/projects/") + projectIdParameterName + "configurations" + configurationIdParameterName + "additionalInstallations" + additionalInstallationsIdParameterName

    val pathSegments = UrlPathSegments.parse(pathPattern.value)
    assertThat(OperationIdGenerator.generateOperationId(HttpMethod.Get, pathSegments).value).isEqualTo("GetAdditionalInstallationForConfigurationForProject")
  }

  @Test
  fun testSimplePath() {
    val pathPattern = UrlPattern.relative("/api/projects/")

    val pathSegments = UrlPathSegments.parse(pathPattern.value)
    assertThat(OperationIdGenerator.generateOperationId(HttpMethod.Post, pathSegments).value).isEqualTo("PostProjects")
  }

  @Test
  fun testSimplePathOneParameter() {
    val pathPattern = UrlPattern.relative("/api/projects/") + UrlParameterName("projectId")

    val pathSegments = UrlPathSegments.parse(pathPattern.value)
    assertThat(OperationIdGenerator.generateOperationId(HttpMethod.Put, pathSegments).value).isEqualTo("PutProject")
  }

  @Test
  fun testBug() {
    assertThat(OperationIdGenerator.generateOperationId(HttpMethod.Put, UrlPathSegments.parse("/foobars/{MyParam}/bars/{SecondParam}")).value).isEqualTo("PutBarForFoobar")
    assertThat(OperationIdGenerator.generateOperationId(HttpMethod.Put, UrlPathSegments.parse("/foobars/{MyParam}/bars")).value).isEqualTo("PutBarsForFoobar")
    assertThat(OperationIdGenerator.generateOperationId(HttpMethod.Put, UrlPathSegments.parse("/api/hub/production-lines/{ProductionLineId}/conveyors/{ConveyorId}")).value).isEqualTo("PutConveyorForProductionLineForHub")
    assertThat(OperationIdGenerator.generateOperationId(HttpMethod.Put, UrlPathSegments.parse("/api/hub/batches/{BatchId}/images")).value).isEqualTo("PutImagesForBatchForHub")
  }

  @Test
  fun testNoPlural() {
    assertThat(OperationIdGenerator.generateOperationId(HttpMethod.Put, UrlPathSegments.parse("/foobar/{MyParam}/bar/{SecondParam}")).value).isEqualTo("PutSecondParamForBarForMyParamForFoobar")
  }

  @Test
  fun testDashes() {
    assertThat(OperationIdGenerator.generateOperationId(HttpMethod.Put, UrlPathSegments.parse("/api/example/jvm/start-parameters")).value).isEqualTo("PutStartParametersForJvmForExample")
  }
}
