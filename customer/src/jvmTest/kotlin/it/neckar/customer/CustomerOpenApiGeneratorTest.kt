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
package it.neckar.customer

import assertk.*
import it.neckar.adjustable.AdjustableValue
import it.neckar.customer.company.Company
import it.neckar.customer.company.rest.views.CompanyContactPersonDetails
import it.neckar.customer.company.rest.views.CompanyDetails
import it.neckar.customer.rest.views.CustomerDetails
import it.neckar.open.kotlin.serializers.toStringPretty
import it.neckar.open.test.utils.LocalResourceGenerationSupport
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.schema.SchemaDescriptor
import org.junit.jupiter.api.Test
import java.io.File

/**
 *
 */
class CustomerOpenApiGeneratorTest {
  @Test
  fun testGenerate() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.usingHarvestedSpecs())

    val generated = generator.generate(
      routes = emptyList(),
      additionalTypes = listOf(
        SchemaDescriptor.of<Address>(),
        // The shared AdjustableValueAddressFormFields (typescript-react-ui) edits this instantiation; products storing
        // an AdjustableValue<Address.Draft> reuse the component instead of generating their own editor.
        SchemaDescriptor.of<AdjustableValue<Address.Draft>>(),
        SchemaDescriptor.of<CustomerDetails>(),
        SchemaDescriptor.of<CompanyDetails>(),
        SchemaDescriptor.of<CompanyContactPersonDetails>(),
      ),
      metaData = OpenApiMetaData("Application Errors", "Contains application errors that might be provided with 4xx and 5xx status codes")
    )

    LocalResourceGenerationSupport(
      target = File("build/generated/openapi/main/openapi-customer.json"),
    ).generate(generated.toStringPretty())
  }

  @Test
  fun testCustomer() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.usingHarvestedSpecs())

    val generated = generator.generate(
      routes = emptyList(),
      additionalTypes = listOf(
        SchemaDescriptor.of<Company>(),
      ),
      metaData = OpenApiMetaData("Application Errors", "Contains application errors that might be provided with 4xx and 5xx status codes")
    )

    assertThat(generated).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "openapi" : "3.1.0",
          "info" : {
            "title" : "Application Errors",
            "description" : "Contains application errors that might be provided with 4xx and 5xx status codes",
            "version" : "1.0.0"
          },
          "servers" : [ ],
          "paths" : { },
          "security" : [ {
            "bearerAuth" : [ ]
          } ],
          "components" : {
            "securitySchemes" : {
              "bearerAuth" : {
                "type" : "http",
                "scheme" : "bearer",
                "bearerFormat" : "JWT"
              }
            },
            "schemas" : {
              "Company" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.customer.company.Company",
                "properties" : {
                  "id" : {
                    "x-source-kotlin-type" : "it.neckar.customer.company.Company.Id",
                    "type" : "string",
                    "format" : "uuid"
                  },
                  "namespace" : {
                    "type" : "string"
                  },
                  "name" : {
                    "type" : "string"
                  },
                  "address" : {
                    "oneOf" : [ {
                      "$ref" : "#/components/schemas/Address.Confirmed"
                    }, {
                      "type" : "null"
                    } ]
                  },
                  "contactInformation" : {
                    "oneOf" : [ {
                      "$ref" : "#/components/schemas/ContactInformation"
                    }, {
                      "type" : "null"
                    } ]
                  },
                  "bankAccountInformation" : {
                    "oneOf" : [ {
                      "$ref" : "#/components/schemas/BankAccountInformation"
                    }, {
                      "type" : "null"
                    } ]
                  },
                  "legalInformation" : {
                    "oneOf" : [ {
                      "$ref" : "#/components/schemas/CompanyLegalInformation"
                    }, {
                      "type" : "null"
                    } ]
                  },
                  "archiveState" : {
                    "$ref" : "#/components/schemas/ArchiveState"
                  }
                },
                "required" : [ "id", "namespace", "name" ]
              },
              "ArchiveState" : {
                "type" : "string",
                "enum" : [ "Active", "Archived" ]
              },
              "CompanyLegalInformation" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.customer.company.CompanyLegalInformation",
                "properties" : {
                  "director" : {
                    "type" : "string"
                  },
                  "registerCourt" : {
                    "type" : "string"
                  },
                  "taxId" : {
                    "x-source-kotlin-type" : "it.neckar.customer.company.TaxId",
                    "type" : "string"
                  },
                  "registrationNumber" : {
                    "x-source-kotlin-type" : "it.neckar.customer.company.RegistrationNumber",
                    "type" : "string"
                  }
                },
                "required" : [ "director", "registerCourt", "taxId", "registrationNumber" ]
              },
              "BankAccountInformation" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.customer.company.BankAccountInformation",
                "properties" : {
                  "bankName" : {
                    "type" : "string"
                  },
                  "accountOwner" : {
                    "type" : "string"
                  },
                  "iban" : {
                    "x-source-kotlin-type" : "it.neckar.customer.company.IBAN",
                    "type" : "string"
                  },
                  "bic" : {
                    "x-source-kotlin-type" : "it.neckar.customer.company.BIC",
                    "type" : "string"
                  }
                },
                "required" : [ "bankName", "accountOwner", "iban", "bic" ]
              },
              "ContactInformation" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.customer.company.ContactInformation",
                "properties" : {
                  "mail" : {
                    "x-source-kotlin-type" : "it.neckar.contact.EmailAddress",
                    "type" : [ "string", "null" ]
                  },
                  "phone" : {
                    "x-source-kotlin-type" : "it.neckar.contact.PhoneNumber",
                    "type" : [ "string", "null" ]
                  },
                  "fax" : {
                    "x-source-kotlin-type" : "it.neckar.contact.PhoneNumber",
                    "type" : [ "string", "null" ]
                  },
                  "url" : {
                    "type" : [ "string", "null" ],
                    "format" : "uri"
                  }
                }
              },
              "Address.Confirmed.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.customer.Address.Confirmed",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/Address.Confirmed"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "Confirmed" ]
                  }
                },
                "required" : [ "type" ]
              },
              "Address.Confirmed" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.customer.Address.Confirmed",
                "properties" : {
                  "street" : {
                    "type" : "string"
                  },
                  "houseNumber" : {
                    "type" : "string"
                  },
                  "zipCode" : {
                    "type" : "string"
                  },
                  "city" : {
                    "type" : "string"
                  },
                  "country" : {
                    "type" : "string"
                  }
                },
                "required" : [ "street", "houseNumber", "zipCode", "city", "country" ]
              }
            }
          }
        }
      """.trimIndent()
    )

  }
}
