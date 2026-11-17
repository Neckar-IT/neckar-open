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
import it.neckar.rest.RestResponse
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test

/**
 * Verifies that schema generation enforces the RestResponse discriminator identity rule:
 * - [RestResponse.GoodResponse] variants must declare exactly `@SerialName("good")`
 * - [RestResponse.BadResponse] variants must declare exactly `"bad/" + simpleName` - plain `bad` is forbidden
 *
 * A violation fails every `*OpenApiTest` at build time instead of surfacing in the frontend at runtime.
 */
class RestResponseDiscriminatorValidationTest {

  private fun generateSchema(schemaDescriptor: SchemaDescriptor) {
    check(schemaDescriptor is SchemaDescriptor.ComponentSchema) {
      "Test fixtures must resolve to a ComponentSchema, was ${schemaDescriptor::class.simpleName}"
    }
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    // The generated JSON is irrelevant - building the schema runs the discriminator validation.
    buildJsonObject {
      generator.buildSchemaType(schemaDescriptor) {
        //Do nothing
      }
    }.let { }
  }

  @Test
  fun `bad variant with contract-violating SerialName fails schema generation`() {
    assertFailure {
      generateSchema(SchemaDescriptor.of(WrongBadNameResponse::class))
    }.all {
      isInstanceOf(IllegalStateException::class)
      messageContains("alreadyExists")
      messageContains("WrongBadNameResponse")
      messageContains("bad/AlreadyExists")
    }
  }

  @Test
  fun `bad variant whose subtype does not match the class name fails schema generation`() {
    assertFailure {
      generateSchema(SchemaDescriptor.of(MismatchedSubtypeResponse::class))
    }.all {
      isInstanceOf(IllegalStateException::class)
      messageContains("bad/NotFound")
      messageContains("MismatchedSubtypeResponse")
      messageContains("bad/Missing")
    }
  }

  @Test
  fun `good variant with contract-violating SerialName fails schema generation`() {
    assertFailure {
      generateSchema(SchemaDescriptor.of(WrongGoodNameResponse::class))
    }.all {
      isInstanceOf(IllegalStateException::class)
      messageContains("available")
      messageContains("WrongGoodNameResponse")
      messageContains("\"good\"")
    }
  }

  @Test
  fun `subtype-less bad discriminator fails schema generation`() {
    assertFailure {
      generateSchema(SchemaDescriptor.of(SubtypeLessBadResponse::class))
    }.all {
      isInstanceOf(IllegalStateException::class)
      messageContains("SubtypeLessBadResponse")
      messageContains("Plain \"bad\" is forbidden")
      messageContains("bad/NotFound")
    }
  }

  @Test
  fun `bad discriminator with empty subtype fails schema generation`() {
    assertFailure {
      generateSchema(SchemaDescriptor.of(EmptySubtypeBadResponse::class))
    }.all {
      isInstanceOf(IllegalStateException::class)
      messageContains("EmptySubtypeBadResponse")
      messageContains("bad/NotFound")
    }
  }

  @Test
  fun `variant implementing neither marker interface fails schema generation`() {
    assertFailure {
      generateSchema(SchemaDescriptor.of(NeitherMarkerResponse::class))
    }.all {
      isInstanceOf(IllegalStateException::class)
      messageContains("Unclassifiable")
      messageContains("neither RestResponse.GoodResponse nor RestResponse.BadResponse")
    }
  }

  @Test
  fun `conformant discriminators pass schema generation`() {
    generateSchema(SchemaDescriptor.of(ConformantResponse::class))
  }

  @Test
  fun `sealed hierarchies outside RestResponse are not restricted`() {
    generateSchema(SchemaDescriptor.of(UnrelatedSealed::class))
  }

  @Serializable
  private sealed interface WrongBadNameResponse : RestResponse<Unit> {
    @Serializable
    @SerialName("good")
    data object Good : WrongBadNameResponse, RestResponse.GoodResponse<Unit> {
      override val data: Unit = Unit
    }

    @Serializable
    @SerialName("alreadyExists")
    data object AlreadyExists : WrongBadNameResponse, RestResponse.BadResponse<Unit>
  }

  @Serializable
  private sealed interface WrongGoodNameResponse : RestResponse<Unit> {
    @Serializable
    @SerialName("available")
    data object Available : WrongGoodNameResponse, RestResponse.GoodResponse<Unit> {
      override val data: Unit = Unit
    }

    @Serializable
    @SerialName("bad/AlreadyExists")
    data object AlreadyExists : WrongGoodNameResponse, RestResponse.BadResponse<Unit>
  }

  @Serializable
  private sealed interface MismatchedSubtypeResponse : RestResponse<Unit> {
    @Serializable
    @SerialName("good")
    data object Good : MismatchedSubtypeResponse, RestResponse.GoodResponse<Unit> {
      override val data: Unit = Unit
    }

    @Serializable
    @SerialName("bad/NotFound")
    data object Missing : MismatchedSubtypeResponse, RestResponse.BadResponse<Unit>
  }

  @Serializable
  private sealed interface SubtypeLessBadResponse : RestResponse<Unit> {
    @Serializable
    @SerialName("good")
    data object Good : SubtypeLessBadResponse, RestResponse.GoodResponse<Unit> {
      override val data: Unit = Unit
    }

    @Serializable
    @SerialName("bad")
    data object Bad : SubtypeLessBadResponse, RestResponse.BadResponse<Unit>
  }

  @Serializable
  private sealed interface EmptySubtypeBadResponse : RestResponse<Unit> {
    @Serializable
    @SerialName("good")
    data object Good : EmptySubtypeBadResponse, RestResponse.GoodResponse<Unit> {
      override val data: Unit = Unit
    }

    @Serializable
    @SerialName("bad/")
    data object Bad : EmptySubtypeBadResponse, RestResponse.BadResponse<Unit>
  }

  @Serializable
  private sealed interface NeitherMarkerResponse : RestResponse<Unit> {
    @Serializable
    @SerialName("good")
    data object Good : NeitherMarkerResponse, RestResponse.GoodResponse<Unit> {
      override val data: Unit = Unit
    }

    /** Implements the sealed response interface but neither marker - unclassifiable for frontends. */
    @Serializable
    @SerialName("bad/Unclassifiable")
    data object Unclassifiable : NeitherMarkerResponse
  }

  @Serializable
  private sealed interface ConformantResponse : RestResponse<Unit> {
    @Serializable
    @SerialName("good")
    data object Good : ConformantResponse, RestResponse.GoodResponse<Unit> {
      override val data: Unit = Unit
    }

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : ConformantResponse, RestResponse.BadResponse<Unit>

    @Serializable
    @SerialName("bad/NotFound")
    data object NotFound : ConformantResponse, RestResponse.BadResponse<Unit>
  }

  @Serializable
  private sealed interface UnrelatedSealed {
    @Serializable
    @SerialName("someArbitraryName")
    data object SomeVariant : UnrelatedSealed
  }
}
