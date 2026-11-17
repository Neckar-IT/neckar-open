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
package it.neckar.rest

import it.neckar.adjustable.AdjustableValue
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.lifeCycle.ArchiveState
import it.neckar.open.kotlin.serializers.toStringPretty
import it.neckar.open.test.utils.LocalResourceGenerationSupport
import it.neckar.rest.archive.ArchiveFilter
import it.neckar.problem.ApplicationError
import it.neckar.rest.error.GlobalHealthResponse
import it.neckar.rest.health.HealthResponse
import it.neckar.rest.jwt.JwtTokenPair
import it.neckar.rest.message.UrlResponse
import it.neckar.rest.meta.RuntimeContextDetails
import it.neckar.rest.draft.ConfirmationStatus
import it.neckar.rest.PlatformHeader
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.pagination.PaginationInfo
import it.neckar.rest.patch.Patchable
import it.neckar.rest.patch.PatchableRequired
import it.neckar.rest.sorting.Sort
import it.neckar.rest.transition.GenericValidationError
import it.neckar.rest.transition.ValidationResult
import org.junit.jupiter.api.Test
import java.io.File

/**
 *
 */
class RestModelOpenApiGeneratorTest {
  @Test
  fun testGenerate() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.usingHarvestedSpecs())

    val generated = generator.generate(
      routes = emptyList(),
      additionalTypes = listOf(
        SchemaDescriptor.of(ApplicationError::class),
        SchemaDescriptor.of(PaginationInfo::class),

        SchemaDescriptor.of(Sort.Order::class),

        SchemaDescriptor.of(UrlResponse::class),
        SchemaDescriptor.of(GlobalHealthResponse::class),
        // Body of GET /health, the endpoint the AppConnectivityProvider of every frontend polls.
        SchemaDescriptor.of(HealthResponse::class),
        SchemaDescriptor.of(NoContentResponse::class),

        //Double, String, Boolean
        SchemaDescriptor.of<AdjustableValue<Double>>(),
        SchemaDescriptor.of<AdjustableValue<String>>(),
        SchemaDescriptor.of<AdjustableValue<Boolean>>(),

        SchemaDescriptor.of(ConfirmationStatus::class),
        SchemaDescriptor.of(ArchiveFilter::class),

        // Emitted by the shared base spec so a frontend that consumes neither the customer nor the
        // business-domain spec still narrows against one generated definition of each.
        SchemaDescriptor.of(ArchiveState::class),
        SchemaDescriptor.of(JwtTokenPair::class),
        SchemaDescriptor.of(PriceWithProfit::class),

        // Base ValidationResult shape (Valid/Invalid discriminator) for the shared spec.
        // Instantiated with the shared GenericValidationError since no product-specific error type
        // is available here; products generate their own ValidationResult<Xxx.ValidationError>.
        SchemaDescriptor.of<ValidationResult<GenericValidationError>>(),

        SchemaDescriptor.of(PlatformHeader::class),

        // Pulls DeploymentStage, ExecutionEnvironment and Hostname into the shared spec, so every
        // frontend narrows the serve-time injected stage against one generated enum instead of the
        // copy its own meta endpoint happens to produce.
        SchemaDescriptor.of(RuntimeContextDetails::class),

        // Patchable types for TypeScript
        SchemaDescriptor.of<Patchable<String>>(),
        SchemaDescriptor.of<PatchableRequired<String>>(),
      ),
      metaData = OpenApiMetaData("Application Errors", "Contains application errors that might be provided with 4xx and 5xx status codes")
    )

    LocalResourceGenerationSupport(
      target = File("build/generated/openapi/main/openapi-rest-model.json"),
    ).generate(generated.toStringPretty())
  }
}


