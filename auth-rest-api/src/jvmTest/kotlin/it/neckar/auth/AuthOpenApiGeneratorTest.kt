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
package it.neckar.auth

import it.neckar.auth.rest.messages.ImpersonateRequest
import it.neckar.auth.rest.messages.ImpersonationTargetsResponse
import it.neckar.auth.rest.messages.JwtRefreshTokenResponse
import it.neckar.auth.rest.messages.LoginRequest
import it.neckar.auth.rest.messages.LoginResponse
import it.neckar.auth.rest.messages.LoginWithTokenRequest
import it.neckar.auth.rest.messages.PasswordResetResponse
import it.neckar.auth.rest.messages.RegisterRequest
import it.neckar.auth.rest.messages.RegisterResponse
import it.neckar.auth.rest.messages.UpdatePasswordRequest
import it.neckar.open.kotlin.serializers.toStringPretty
import it.neckar.open.test.utils.LocalResourceGenerationSupport
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.user.rest.messages.CreateNewUserResponse
import it.neckar.user.rest.messages.ImpersonationCandidatesResponse
import it.neckar.user.rest.messages.MeResponse
import it.neckar.user.rest.messages.UpdateUserRequest
import it.neckar.user.rest.messages.UsersResponse
import it.neckar.user.rest.views.ImpersonationCandidate
import it.neckar.user.rest.views.UserDetails
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Generates the OpenAPI spec for auth-related REST classes.
 *
 * The auth module owns the `/auth/...` endpoint surface. User-related DTOs live in the
 * user module physically but are part of the auth API (e.g. `PUT /auth/users/me`),
 * so they are referenced here to produce a single, consistent `openapi-auth.json`.
 *
 * The list holds every type the frontend imports by name, including views a message only carries as
 * a property — see `internal/patterns/docs/kotlin/openapi-generation.md` for the rule and for the
 * other three specs this one is merged with.
 */
class AuthOpenApiGeneratorTest {
  @Test
  fun testGenerate() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.usingHarvestedSpecs())

    val generated = generator.generate(
      routes = emptyList(),
      additionalTypes = listOf(
        SchemaDescriptor.of(CreateNewUserResponse::class),
        SchemaDescriptor.of(ImpersonateRequest::class),
        SchemaDescriptor.of(ImpersonationCandidate::class),
        SchemaDescriptor.of(ImpersonationCandidatesResponse::class),
        SchemaDescriptor.of(ImpersonationTargetsResponse::class),
        SchemaDescriptor.of(JwtRefreshTokenResponse::class),
        SchemaDescriptor.of(LoginRequest::class),
        SchemaDescriptor.of(LoginResponse::class),
        SchemaDescriptor.of(LoginWithTokenRequest::class),
        SchemaDescriptor.of(MeResponse::class),
        SchemaDescriptor.of(PasswordResetResponse::class),
        SchemaDescriptor.of(RegisterRequest::class),
        SchemaDescriptor.of(RegisterResponse::class),
        SchemaDescriptor.of(UpdatePasswordRequest::class),
        SchemaDescriptor.of(UpdateUserRequest::class),
        SchemaDescriptor.of(UsersResponse::class),
        SchemaDescriptor.of(UserDetails::class),
      ),
      metaData = OpenApiMetaData("Application Errors", "Contains application errors that might be provided with 4xx and 5xx status codes")
    )

    LocalResourceGenerationSupport(
      target = File("build/generated/openapi/main/openapi-auth.json"),
    ).generate(generated.toStringPretty())
  }
}
