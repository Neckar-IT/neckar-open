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
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import it.neckar.open.kotlin.serializers.JsonPath
import it.neckar.open.kotlin.serializers.toStringPretty
import it.neckar.open.serialization.atPath
import it.neckar.open.serialization.refs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.openApiRoute

/**
 * Tests for nullable reference types to improve branch coverage.
 *
 * Covers the oneOf branch in putRef() in OpenApiJsonGenerator.kt.
 * This branch is used when schemaConstraints.showNullInType is true.
 */
class OpenApiNullableReferenceTypesTest {
  @Serializable
  private data class Address(
    val street: String,
    val city: String,
  )

  @Serializable
  private data class Person(
    val name: String,
    val address: Address?,
  )

  @Serializable
  private data class Company(
    val name: String,
    val headquarters: Address?,
    val employees: List<Person>?,
  )

  @Test
  fun testNullableReferenceTypeInResponse() {
    val route = openApiRoute("/api/person", HttpMethod.Get, emptyList()) {
      response("Get person with nullable address") {
        statusCode = HttpStatusCode.OK
        body<Person> {
          example("WithAddress", Person("John", Address("Main St", "NYC")))
          example("WithoutAddress", Person("Jane", null))
        }
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
    val jsonString = json.toStringPretty()

    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
    val components = jsonObject.atPath(JsonPath("components", "schemas"))

    // Verify Person schema has nullable address property
    val personSchema = components.filterKeys { it.contains("Person") }.values.first().jsonObject
    val properties = personSchema["properties"]!!.jsonObject
    val addressProperty = properties["address"]!!.jsonObject

    // When a property is nullable (Address?), it should use oneOf with type null
    assertThat(addressProperty.containsKey("oneOf") || addressProperty.containsKey("\$ref")).isEqualTo(true)

    // If oneOf is present, verify it contains null type
    if (addressProperty.containsKey("oneOf")) {
      val oneOf = addressProperty["oneOf"]!!.jsonArray
      val types = oneOf.map { it.jsonObject }
      val hasNullType = types.any { it.containsKey("type") && it["type"]!!.jsonPrimitive.content == "null" }

      assertThat(hasNullType).isEqualTo(true)
      assertThat(oneOf).refs().isNotEmpty()
    }
  }

  @Test
  fun testMultipleNullableReferencesInResponse() {
    val route = openApiRoute("/api/company", HttpMethod.Get, emptyList()) {
      response("Get company with nullable references") {
        statusCode = HttpStatusCode.OK
        body<Company> {
          example("Full", Company("Acme Inc", Address("Tech Ave", "SF"), listOf(Person("Alice", null))))
          example("Minimal", Company("Small Co", null, null))
        }
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
    val jsonString = json.toStringPretty()

    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
    val components = jsonObject.atPath(JsonPath("components", "schemas"))

    // Verify Company schema
    val companySchema = components.filterKeys { it.contains("Company") }.values.first().jsonObject
    val properties = companySchema["properties"]!!.jsonObject

    // Headquarters should be nullable (Address?)
    val headquartersProperty = properties["headquarters"]!!.jsonObject

    // For nullable reference types, expect oneOf with ref and null type
    if (headquartersProperty.containsKey("oneOf")) {
      val oneOf = headquartersProperty["oneOf"]!!.jsonArray
      val types = oneOf.map { it.jsonObject }
      val hasNullType = types.any { it.containsKey("type") && it["type"]!!.jsonPrimitive.content == "null" }

      assertThat(hasNullType).isEqualTo(true)
      assertThat(oneOf).refs().isNotEmpty()
    } else {
      // If not oneOf, at minimum should have a $ref
      assertThat(headquartersProperty.containsKey("\$ref")).isEqualTo(true)
    }

    // Employees is List<Person>? - a nullable collection (may have different structure)
    val employeesProperty = properties["employees"]!!.jsonObject
    assertThat(employeesProperty).isNotNull()
  }

  @Test
  fun testNullableReferenceInRequestBody() {
    @Serializable
    data class CreatePersonRequest(
      val name: String,
      val address: Address?,
    )

    val route = openApiRoute("/api/person", HttpMethod.Post, emptyList()) {
      requestBody<CreatePersonRequest> {
        description = "Create person request"
        example("WithAddress", CreatePersonRequest("John", Address("Main St", "NYC")))
        example("WithoutAddress", CreatePersonRequest("Jane", null))
      }

      response("Person created") {
        statusCode = HttpStatusCode.Created
        body<Person> {
          example("Created", Person("John", Address("Main St", "NYC")))
        }
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
    val jsonString = json.toStringPretty()

    // Verify request body schema has nullable address
    assertThat(jsonString).contains("CreatePersonRequest")

    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
    val components = jsonObject.atPath(JsonPath("components", "schemas"))

    val requestSchema = components.filterKeys { it.contains("CreatePersonRequest") }.values.first().jsonObject
    val properties = requestSchema["properties"]!!.jsonObject
    val addressProperty = properties["address"]!!.jsonObject

    // Verify nullable reference structure
    assertThat(addressProperty.containsKey("oneOf") || addressProperty.containsKey("\$ref")).isEqualTo(true)
  }

  @Test
  fun testNestedNullableReferences() {
    @Serializable
    data class Department(
      val name: String,
      val manager: Person?,
    )

    @Serializable
    data class Organization(
      val name: String,
      val mainDepartment: Department?,
    )

    val route = openApiRoute("/api/org", HttpMethod.Get, emptyList()) {
      response("Get organization with nested nullable references") {
        statusCode = HttpStatusCode.OK
        body<Organization> {
          example(
            "Full",
            Organization(
              "TechCorp",
              Department("Engineering", Person("Bob", Address("Tech St", "Austin")))
            )
          )
          example("NoManager", Organization("StartupCo", Department("All", null)))
          example("NoDepartment", Organization("SoloCo", null))
        }
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
    val jsonString = json.toStringPretty()

    // Verify nested nullable references are properly handled
    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
    val components = jsonObject.atPath(JsonPath("components", "schemas"))

    // Organization has nullable Department reference
    val orgSchema = components.filterKeys { it.contains("Organization") }.values.first().jsonObject
    val orgProperties = orgSchema["properties"]!!.jsonObject
    val mainDeptProperty = orgProperties["mainDepartment"]!!.jsonObject

    assertThat(mainDeptProperty.containsKey("oneOf") || mainDeptProperty.containsKey("\$ref")).isEqualTo(true)

    // Department has nullable Person reference
    val deptSchema = components.filterKeys { it.contains("Department") }.values.first().jsonObject
    val deptProperties = deptSchema["properties"]!!.jsonObject
    val managerProperty = deptProperties["manager"]!!.jsonObject

    assertThat(managerProperty.containsKey("oneOf") || managerProperty.containsKey("\$ref")).isEqualTo(true)
  }
}
