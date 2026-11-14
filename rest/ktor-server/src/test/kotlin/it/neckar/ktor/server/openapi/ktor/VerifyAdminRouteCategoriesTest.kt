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
package it.neckar.ktor.server.openapi.ktor

import assertk.*
import assertk.assertions.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.ktor.collectRoutes
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.openapi.OpenApiException
import it.neckar.rest.openapi.OpenApiMetaData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisableLogging
class VerifyAdminRouteCategoriesTest {
  @Test
  fun testFailsForAdminPathWithoutAdminCategory() = testApplication {
    installOpenApiPluginForTest()

    routing {
      route("api/admin") {
        get("tenants") {
        }.doc {
          summary = "An administrative endpoint left in the default category"
          noResponseBody()
        }
      }
    }

    startApplication()

    val exception = assertThrows<OpenApiException> {
      application.verifyAdminRouteCategories()
    }

    assertThat(exception.message).isNotNull().contains("GET /api/admin/tenants: category 'default', expected 'admin' for a path under /api/admin")
  }

  @Test
  fun testFailsForAdminCategoryOutsideAdminPath() = testApplication {
    installOpenApiPluginForTest()

    routing {
      get("api/tenants") {
      }.doc {
        summary = "A production endpoint put into the admin category"
        category = OpenApiMetaData.Category.Admin
        noResponseBody()
      }
    }

    startApplication()

    val exception = assertThrows<OpenApiException> {
      application.verifyAdminRouteCategories()
    }

    assertThat(exception.message).isNotNull().contains("GET /api/tenants: category 'admin', expected a path under /api/admin")
  }

  @Test
  fun testMessageNamesEveryMiscategorizedRouteOrderedByPathThenMethod() = testApplication {
    installOpenApiPluginForTest()

    routing {
      route("api/admin") {
        get("tenants") {
        }.doc {
          summary = "An administrative endpoint left in the default category"
          noResponseBody()
        }
        delete("tenants") {
        }.doc {
          summary = "An administrative endpoint put into the meta category"
          category = OpenApiMetaData.Category.Meta
          noResponseBody()
        }
      }
      get("api/tenants") {
      }.doc {
        summary = "A production endpoint put into the admin category"
        category = OpenApiMetaData.Category.Admin
        noResponseBody()
      }
    }

    startApplication()

    val exception = assertThrows<OpenApiException> {
      application.verifyAdminRouteCategories()
    }

    assertThat(exception.problem).isInstanceOf<OpenApiException.Problem.MiscategorizedRoutes>()
      .transform { it.routes.map { miscategorizedRoute -> miscategorizedRoute.route.value } }
      .containsExactly("DELETE /api/admin/tenants", "GET /api/admin/tenants", "GET /api/tenants")

    assertThat(exception.message).isNotNull().contains("DELETE /api/admin/tenants: category 'meta'")
  }

  @Test
  fun testPassesForAnUndocumentedAdminRoute() = testApplication {
    installOpenApiPluginForTest()

    routing {
      route("api/admin") {
        get("tenants") {
        }
      }
    }

    startApplication()

    assertThat(application.collectRoutes().routesWithoutDoc()).isNotEmpty()
    application.verifyAdminRouteCategories()
  }

  @Test
  fun testMessageNamesTheSourceLocationOfTheDocBlock() = testApplication {
    installOpenApiPluginForTest()

    routing {
      route("api/admin") {
        get("tenants") {
        }.doc {
          summary = "An administrative endpoint left in the default category"
          noResponseBody()
        }
      }
    }

    startApplication()

    val exception = assertThrows<OpenApiException> {
      application.verifyAdminRouteCategories()
    }

    assertThat(exception.message).isNotNull().contains("(VerifyAdminRouteCategoriesTest.kt:")
  }

  @Test
  fun testPassesForAPathThatOnlyStartsWithTheAdminSegment() = testApplication {
    installOpenApiPluginForTest()

    routing {
      get("api/administration/tenants") {
      }.doc {
        summary = "A production endpoint whose second segment starts with the admin segment"
        noResponseBody()
      }
    }

    startApplication()

    assertThat(application.documentedPaths()).contains("/api/administration/tenants")
    application.verifyAdminRouteCategories()
  }

  @Test
  fun testFailsForThePrefixRouteItself() = testApplication {
    installOpenApiPluginForTest()

    routing {
      get("api/admin") {
      }.doc {
        summary = "The entry point of the administrative routes, left in the default category"
        noResponseBody()
      }
    }

    startApplication()

    val exception = assertThrows<OpenApiException> {
      application.verifyAdminRouteCategories()
    }

    assertThat(exception.message).isNotNull().contains("GET /api/admin: category 'default'")
  }

  @Test
  fun testPassesWhenPathAndCategoryAgree() = testApplication {
    installOpenApiPluginForTest()

    routing {
      route("api/admin") {
        get("tenants") {
        }.doc {
          summary = "An administrative endpoint in the admin category"
          category = OpenApiMetaData.Category.Admin
          noResponseBody()
        }
      }
      get("api/tenants") {
      }.doc {
        summary = "A production endpoint in the default category"
        noResponseBody()
      }
    }

    startApplication()

    assertThat(application.documentedPaths()).containsAll("/api/admin/tenants", "/api/tenants")
    application.verifyAdminRouteCategories()
  }

  private fun Application.documentedPaths(): List<String> {
    return openApiRoutes().map { it.path.format() }
  }

  private fun ApplicationTestBuilder.installOpenApiPluginForTest() {
    install(OpenApiPlugin) {
      metadata = OpenApiMetaData(
        title = "Test API",
        description = "Da Test API",
      )
      authenticationRequirement = AuthenticationRequirement.None
      scalarUi = ScalarUiMode.Disabled
    }
  }
}
