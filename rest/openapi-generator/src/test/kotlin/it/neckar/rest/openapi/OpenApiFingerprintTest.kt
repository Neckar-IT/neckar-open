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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import it.neckar.rest.version.ProtocolFingerprint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * The fingerprint changes with everything that changes the generated client.
 * It stays the same across documentation, source locations, navigation, the `info` block, the servers and the security schemes.
 */
class OpenApiFingerprintTest {

  @Test
  fun `an added field changes the fingerprint`() {
    val extended = spec(jobSchema = """{"properties":{"id":{"type":"string"},"prompt":{"type":"string"}}}""")

    assertThat(fingerprint(extended)).isNotEqualTo(fingerprint(baseline))
  }

  @Test
  fun `a changed type changes the fingerprint`() {
    val retyped = spec(jobSchema = """{"properties":{"id":{"type":"integer"}}}""")

    assertThat(fingerprint(retyped)).isNotEqualTo(fingerprint(baseline))
  }

  @Test
  fun `an added path changes the fingerprint`() {
    val withExtraPath = spec(extraPaths = ""","/runs":{"get":{"operationId":"GetRuns"}}""")

    assertThat(fingerprint(withExtraPath)).isNotEqualTo(fingerprint(baseline))
  }

  @Test
  fun `a renamed component changes the fingerprint`() {
    val renamed = spec(componentName = "BadResponse.VersionConflict")

    assertThat(fingerprint(renamed)).isNotEqualTo(fingerprint(baseline))
  }

  @Test
  fun `a renamed operationId changes the fingerprint`() {
    val renamed = spec(operationId = "GetJobDetails")

    assertThat(fingerprint(renamed)).isNotEqualTo(fingerprint(baseline))
  }

  @Test
  fun `a changed example value changes the fingerprint`() {
    // The value carries a field called `description`: an example value is data down to its fields.
    val changed = spec(exampleValue = """{"id":"job-1","description":"other"}""")

    assertThat(fingerprint(changed)).isNotEqualTo(fingerprint(baseline))
  }

  @Test
  fun `a changed example type reference changes the fingerprint`() {
    // `x-type-ref` names the interface the generated example file is typed with.
    val retyped = spec(exampleTypeRef = "OtherJob")

    assertThat(fingerprint(retyped)).isNotEqualTo(fingerprint(baseline))
  }

  @Test
  fun `a changed parameter changes the fingerprint`() {
    val optional = spec(operationExtra = ""","parameters":[{"name":"limit","in":"query","required":false,"schema":{"type":"integer"}}]""")
    val required = spec(operationExtra = ""","parameters":[{"name":"limit","in":"query","required":true,"schema":{"type":"integer"}}]""")

    assertThat(fingerprint(required)).isNotEqualTo(fingerprint(optional))
  }

  @Test
  fun `a renamed discriminator value changes the fingerprint`() {
    val first = spec(jobSchema = """{"properties":{"id":{"type":"string"}},"discriminator":{"propertyName":"type","mapping":{"a":"#/components/schemas/Job"}}}""")
    val renamed = spec(jobSchema = """{"properties":{"id":{"type":"string"}},"discriminator":{"propertyName":"type","mapping":{"b":"#/components/schemas/Job"}}}""")

    assertThat(fingerprint(renamed)).isNotEqualTo(fingerprint(first))
  }

  @Test
  fun `a changed method changes the fingerprint`() {
    val posted = spec(method = "post")

    assertThat(fingerprint(posted)).isNotEqualTo(fingerprint(baseline))
  }

  @Test
  fun `a required field changes the fingerprint`() {
    val required = spec(jobSchema = """{"required":["id"],"properties":{"id":{"type":"string"}}}""")

    assertThat(fingerprint(required)).isNotEqualTo(fingerprint(baseline))
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      """"format":"uuid"""",
      """"maxLength":36""",
      """"pattern":"^job-"""",
      """"enum":["job-1"]""",
      """"${AdditionalOpenApiKeys.SourceRef.Unit}":"s"""",
      """"${AdditionalOpenApiKeys.ValueClassMeta.UnitSymbol}":"s"""",
      """"${AdditionalOpenApiKeys.ValueClassMeta.UnitName}":"second"""",
    ]
  )
  fun `a format, constraint, enum or unit changes the fingerprint`(keyValue: String) {
    val constrained = spec(jobSchema = """{"properties":{"id":{"type":"string",$keyValue}}}""")

    assertThat(fingerprint(constrained)).isNotEqualTo(fingerprint(baseline))
  }

  @Test
  fun `a field named like a documentation key counts like any other`() {
    // The trap: in the spec a DTO field and the schema's own prose are both spelled "description".
    // Dropping the one by name drops the other, and a removed field would pass unnoticed.
    val withDescriptionField = spec(jobSchema = """{"properties":{"id":{"type":"string"},"description":{"type":"string"}}}""")

    assertThat(fingerprint(withDescriptionField)).isNotEqualTo(fingerprint(baseline))
  }

  @Test
  fun `a discriminator value named like a documentation key counts like any other`() {
    val withMapping = spec(jobSchema = """{"properties":{"id":{"type":"string"}},"discriminator":{"propertyName":"type","mapping":{"a":"#/components/schemas/Job"}}}""")
    val withDescriptionMapping = spec(jobSchema = """{"properties":{"id":{"type":"string"}},"discriminator":{"propertyName":"type","mapping":{"a":"#/components/schemas/Job","description":"#/components/schemas/Job"}}}""")

    assertThat(fingerprint(withDescriptionMapping)).isNotEqualTo(fingerprint(withMapping))
  }

  @Test
  fun `a component named like a documentation key counts like any other`() {
    // The reference stays on Job: only the key under `components.schemas` tells the two apart.
    val summary = spec(componentsExtra = ""","summary":{"type":"string"}""")
    val description = spec(componentsExtra = ""","description":{"type":"string"}""")

    assertThat(fingerprint(description)).isNotEqualTo(fingerprint(summary))
  }

  @Test
  fun `an example named like a documentation key counts like any other`() {
    val first = spec(exampleId = "summary")
    val changed = spec(exampleId = "summary", exampleValue = """{"id":"job-2"}""")

    assertThat(fingerprint(changed)).isNotEqualTo(fingerprint(first))
  }

  @Test
  fun `an example named default is an example entry like any other`() {
    // Under `examples`, `default` is an example id: its summary is documentation.
    val plain = spec(exampleId = "default")
    val documented = spec(exampleId = "default", exampleExtra = ""","summary":"First job"""")

    assertThat(fingerprint(documented)).isEqualTo(fingerprint(plain))
  }

  @Test
  fun `the default response's description leaves the fingerprint alone`() {
    // Under `responses`, `default` is a status: its description is documentation.
    val first = spec(responseStatus = "default", responseDescription = "Any other outcome.")
    val reworded = spec(responseStatus = "default", responseDescription = "Every other outcome.")

    assertThat(fingerprint(reworded)).isEqualTo(fingerprint(first))
  }

  @Test
  fun `a header named like a documentation key counts like any other`() {
    val summary = spec(responseExtra = ""","headers":{"summary":{"schema":{"type":"string"}}}""")
    val description = spec(responseExtra = ""","headers":{"description":{"schema":{"type":"string"}}}""")

    assertThat(fingerprint(description)).isNotEqualTo(fingerprint(summary))
  }

  @Test
  fun `a default value is data down to its fields`() {
    val first = spec(jobSchema = """{"properties":{"id":{"type":"string"}},"default":{"description":"first"}}""")
    val second = spec(jobSchema = """{"properties":{"id":{"type":"string"}},"default":{"description":"second"}}""")

    assertThat(fingerprint(second)).isNotEqualTo(fingerprint(first))
  }

  @Test
  fun `a field named like a data key is a schema like any other`() {
    // Under `properties`, `enum` is a property name: its schema's description is documentation.
    val plain = spec(jobSchema = """{"properties":{"enum":{"type":"string"}}}""")
    val documented = spec(jobSchema = """{"properties":{"enum":{"type":"string","description":"The enum."}}}""")

    assertThat(fingerprint(documented)).isEqualTo(fingerprint(plain))
  }

  @Test
  fun `an example's own documentation leaves the fingerprint alone`() {
    // The generated example file carries only `value` and `x-type-ref`.
    val documented = spec(
      exampleExtra = ""","summary":"First job","description":"A job.","${AdditionalOpenApiKeys.SourceRef.KotlinType}":"it.neckar.Job"""",
    )

    assertThat(fingerprint(documented)).isEqualTo(fingerprint(baseline))
  }

  @Test
  fun `a moved source location leaves the fingerprint alone`() {
    // A line inserted above a route shifts every location below it; the generated client stays the same.
    val shifted = spec(sourceLocation = "PatternAuthRouting.kt:56")

    assertThat(fingerprint(shifted)).isEqualTo(fingerprint(baseline))
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "description",
      "summary",
      AdditionalOpenApiKeys.SourceRef.KotlinType,
      AdditionalOpenApiKeys.ValueClassMeta.GermanTerm,
      AdditionalOpenApiKeys.EnumDescriptions,
      AdditionalOpenApiKeys.EnumGermanTerms,
      AdditionalOpenApiKeys.DeprecatedReason,
      "deprecated",
      "tags",
      AdditionalOpenApiKeys.TagGroups,
      AdditionalOpenApiKeys.DisplayName,
      AdditionalOpenApiKeys.IgnoredProperties,
    ]
  )
  fun `an added documentation key leaves the fingerprint alone`(key: String) {
    val documented = spec(
      jobSchema = """{"$key":"reworded","properties":{"id":{"$key":"reworded","type":"string"}}}""",
      operationExtra = ""","$key":"reworded"""",
      topLevelExtra = ""","$key":"reworded"""",
    )

    assertThat(fingerprint(documented)).isEqualTo(fingerprint(baseline))
  }

  @Test
  fun `the document's own title and origin leave the fingerprint alone`() {
    // `servers` is filled from the request origin — the same service asked through another host
    // must not look like another protocol.
    val elsewhere = spec(topLevelExtra = ""","info":{"title":"other","version":"2.0.0"},"servers":[{"url":"https://elsewhere.neckar.it"}]""")

    assertThat(fingerprint(elsewhere)).isEqualTo(fingerprint(baseline))
  }

  @Test
  fun `the deployment's OAuth2 scheme leaves the fingerprint alone`() {
    // Token URL and client id differ per deployment; a client of one deployment works against every other.
    val withOAuth2 = JsonObject(baseline + ("components" to JsonObject(baseline.getValue("components").jsonObject + ("securitySchemes" to oAuth2Scheme))))

    assertThat(fingerprint(withOAuth2)).isEqualTo(fingerprint(baseline))
  }

  @Test
  fun `key order leaves the fingerprint alone`() {
    val reordered = baseline.withReversedKeys().jsonObject

    assertThat(fingerprint(reordered)).isEqualTo(fingerprint(baseline))
  }

  @Test
  fun `a document moved to another category changes the fingerprint`() {
    // Each category is its own generated client file.
    val default = openApiFingerprint(mapOf(OpenApiMetaData.Category.Default to baseline))
    val meta = openApiFingerprint(mapOf(OpenApiMetaData.Category.Meta to baseline))

    assertThat(meta).isNotEqualTo(default)
  }

  @Test
  fun `an added category document changes the fingerprint`() {
    val single = openApiFingerprint(mapOf(OpenApiMetaData.Category.Default to baseline))
    val withMeta = openApiFingerprint(mapOf(OpenApiMetaData.Category.Default to baseline, OpenApiMetaData.Category.Meta to spec(componentName = "Status")))

    assertThat(withMeta).isNotEqualTo(single)
  }

  private val baseline: JsonObject = spec()

  private val oAuth2Scheme: JsonObject = Json.decodeFromString(
    JsonObject.serializer(),
    """{"oauth2":{"type":"oauth2","flows":{"authorizationCode":{"tokenUrl":"https://auth.neckar.it/token","x-scalar-client-id":"agents","scopes":{"openid":"OpenID"}}}}}""",
  )

  private fun fingerprint(spec: JsonObject): ProtocolFingerprint = openApiFingerprint(mapOf(OpenApiMetaData.Category.Default to spec))

  /** [this] with the keys of every object in reverse order, nested objects included. */
  private fun JsonElement.withReversedKeys(): JsonElement = when (this) {
    is JsonObject -> JsonObject(entries.reversed().associate { (key, value) -> key to value.withReversedKeys() })
    is JsonArray -> JsonArray(map { it.withReversedKeys() })
    is JsonPrimitive -> this
  }

  private fun spec(
    componentName: String = "Job",
    jobSchema: String = """{"properties":{"id":{"type":"string"}}}""",
    operationId: String = "GetJob",
    sourceLocation: String = "PatternAuthRouting.kt:55",
    exampleValue: String = """{"id":"job-1","description":"first"}""",
    exampleId: String = "first",
    exampleTypeRef: String = componentName,
    exampleExtra: String = "",
    method: String = "get",
    operationExtra: String = "",
    responseStatus: String = "200",
    responseDescription: String = "The job.",
    responseExtra: String = "",
    componentsExtra: String = "",
    extraPaths: String = "",
    topLevelExtra: String = "",
  ): JsonObject = Json.decodeFromString(
    JsonObject.serializer(),
    """
    {
      "paths":{
        "/jobs":{"$method":{
          "operationId":"$operationId",
          "${AdditionalOpenApiKeys.SourceRef.SourceInfo}":{"location":"$sourceLocation"},
          "responses":{"$responseStatus":{"description":"$responseDescription","content":{"application/json":{
            "schema":{"${'$'}ref":"#/components/schemas/$componentName"},
            "examples":{"$exampleId":{"value":$exampleValue,"${AdditionalOpenApiKeys.TypeRef}":"$exampleTypeRef"$exampleExtra}}
          }}$responseExtra}}
          $operationExtra
        }}
        $extraPaths
      },
      "components":{"schemas":{"$componentName":$jobSchema$componentsExtra}}
      $topLevelExtra
    }
  """.trimIndent(),
  )
}
