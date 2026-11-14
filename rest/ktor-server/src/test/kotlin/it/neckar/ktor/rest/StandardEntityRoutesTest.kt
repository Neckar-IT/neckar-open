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
package it.neckar.ktor.rest

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.getOrFail
import io.mockk.mockk
import it.neckar.ktor.archive.ArchiveResult
import it.neckar.ktor.archive.UnarchiveResult
import it.neckar.ktor.authorization.installCapabilityChecks
import it.neckar.ktor.installFeatureFlagSupport
import it.neckar.ktor.installJson
import it.neckar.ktor.server.openapi.ktor.openApiRoutes
import it.neckar.ktor.status.installStatusPages
import it.neckar.open.authorization.Capability
import it.neckar.open.authorization.CapabilityKey
import it.neckar.open.authorization.CoreCapability
import it.neckar.open.authorization.UserCapabilities
import it.neckar.open.http.Url
import it.neckar.open.http.UrlParameterName
import it.neckar.open.http.UrlPattern
import it.neckar.open.tenancy.Tenant
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.FoundResponse
import it.neckar.rest.draft.UsedBy
import it.neckar.rest.draft.UsedByCount
import it.neckar.rest.openapi.dsl.OpenApiRoute
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@DisableLogging
class StandardEntityRoutesTest {

  @Test
  fun `archive answers good with the referencing records`() = testApplication {
    application {
      installRoutes(archiveResult = ArchiveResult.Archived(UsedBy(listOf(UsedByCount(AuthorReference.Book, activeCount = 2)))))
    }

    val response = client.post("/api/authors/a1/archive")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    assertThat(response.bodyAsText()).contains("\"type\":\"good\"")
    assertThat(response.bodyAsText()).contains("\"type\":\"Book\",\"activeCount\":2")
  }

  @Test
  fun `archive answers each failure of the support`() {
    assertThat(archiveBody(ArchiveResult.AlreadyArchived)).contains("bad/AlreadyArchived")
    assertThat(archiveBody(ArchiveResult.NotFound)).contains("bad/NotFound")
  }

  @Test
  fun `archive passes the ID from the path and the tenant of the caller to the support`() = testApplication {
    var received: Pair<String, Tenant.Id>? = null
    application {
      installRoutes(archive = { id, tenantId ->
        received = id to tenantId
        ArchiveResult.NotFound
      })
    }

    client.post("/api/authors/a1/archive")

    assertThat(received).isEqualTo("a1" to CallerTenant)
  }

  @Test
  fun `unarchive answers each result of the support`() {
    assertThat(unarchiveBody(UnarchiveResult.Unarchived)).contains("\"type\":\"good\"")
    assertThat(unarchiveBody(UnarchiveResult.AlreadyActive)).contains("bad/AlreadyActive")
    assertThat(unarchiveBody(UnarchiveResult.NotFound)).contains("bad/NotFound")
  }

  @Test
  fun `unarchive passes the ID from the path and the tenant of the caller to the support`() = testApplication {
    var received: Pair<String, Tenant.Id>? = null
    application {
      installRoutes(unarchive = { id, tenantId ->
        received = id to tenantId
        UnarchiveResult.NotFound
      })
    }

    client.post("/api/authors/a1/unarchive")

    assertThat(received).isEqualTo("a1" to CallerTenant)
  }

  @Test
  fun `a caller without WriteData is answered 403 on both routes`() = testApplication {
    application {
      installRoutes(capabilities = UserCapabilities.Empty)
    }

    assertThat(client.post("/api/authors/a1/archive").status).isEqualTo(HttpStatusCode.Forbidden)
    assertThat(client.post("/api/authors/a1/unarchive").status).isEqualTo(HttpStatusCode.Forbidden)
  }

  @Test
  fun `a project capability guards both routes and stands in their descriptions`() = testApplication {
    application {
      installRoutes(capabilities = UserCapabilities(setOf(CoreCapability.WriteData.key)), capability = EditCatalog)
    }
    startApplication()

    assertThat(client.post("/api/authors/a1/archive").status).isEqualTo(HttpStatusCode.Forbidden)
    assertThat(client.post("/api/authors/a1/unarchive").status).isEqualTo(HttpStatusCode.Forbidden)
    assertThat(application.openApiRoute("Archive an author").description).isEqualTo(
      "Sets the archive state of the author to Archived. Reversible via POST /authors/{authorId}/unarchive. " +
        "Answers with the active records that still point at the author; they stay valid. Requires EditCatalog."
    )
    assertThat(application.openApiRoute("Unarchive an author").description).isEqualTo("Restores the archive state of the author to Active. Requires EditCatalog.")
  }

  @Test
  fun `a caller with the project capability alone reaches both routes`() = testApplication {
    application {
      installRoutes(capabilities = UserCapabilities(setOf(EditCatalog.key)), capability = EditCatalog)
    }

    assertThat(client.post("/api/authors/a1/archive").bodyAsText()).contains("bad/NotFound")
    assertThat(client.post("/api/authors/a1/unarchive").bodyAsText()).contains("bad/NotFound")
  }

  @Test
  fun `the archive route documents the way back and the referencing records`() = testApplication {
    application {
      installRoutes()
    }
    startApplication()

    val archive = application.openApiRoute("Archive an author")

    assertThat(archive.description).isEqualTo(
      "Sets the archive state of the author to Archived. Reversible via POST /authors/{authorId}/unarchive. " +
        "Answers with the active records that still point at the author; they stay valid. Requires WriteData."
    )
  }

  @Test
  fun `an entity without referencing types gets no usage sentence`() = testApplication {
    application {
      installServerPlugins(UserCapabilities(setOf(CoreCapability.WriteData.key)))
      routing {
        archiveEntityRoute<NoReference, String>(ArchivePattern, UnarchivePattern, AuthorLabel, { CallerTenant }, { getOrFail("authorId") }, { _, _ -> ArchiveResult.NotFound })
      }
    }
    startApplication()

    assertThat(application.openApiRoute("Archive an author").description).isEqualTo(
      "Sets the archive state of the author to Archived. Reversible via POST /authors/{authorId}/unarchive. Requires WriteData."
    )
  }

  @Test
  fun `an unarchive pattern outside api is rejected`() {
    val outsideApi = Url.pattern("authors") + UrlParameterName("authorId") + "unarchive"

    assertFailure { archiveEntityDescription(outsideApi, AuthorLabel, referencedBy = false, descriptionNote = null, capability = CoreCapability.WriteData) }.hasMessage("Unarchive pattern <authors/{authorId}/unarchive> does not start with api/")
  }

  @Test
  fun `the description note stands before the required capability`() = testApplication {
    application {
      installRoutes(descriptionNote = "Applies in every lifecycle state.")
    }
    startApplication()

    assertThat(application.openApiRoute("Archive an author").description).isEqualTo(
      "Sets the archive state of the author to Archived. Reversible via POST /authors/{authorId}/unarchive. " +
        "Answers with the active records that still point at the author; they stay valid. Applies in every lifecycle state. Requires WriteData."
    )
  }

  @Test
  fun `the unarchive route names the entity`() = testApplication {
    application {
      installRoutes()
    }
    startApplication()

    assertThat(application.openApiRoute("Unarchive an author").description).isEqualTo("Restores the archive state of the author to Active. Requires WriteData.")
  }

  @Test
  fun `preview answers good with the preview and NotFound without one`() {
    assertThat(previewBody(AuthorPreview(name = "Mark Twain"))).isEqualTo("""{"type":"good","data":{"name":"Mark Twain"}}""")
    assertThat(previewBody(null)).isEqualTo("""{"type":"bad/NotFound"}""")
  }

  @Test
  fun `preview passes the ID from the path and the tenant of the caller to the support`() = testApplication {
    var received: Pair<String, Tenant.Id>? = null
    application {
      installRoutes(findPreview = { id, tenantId ->
        received = id to tenantId
        null
      })
    }

    client.get("/api/authors/a1/preview")

    assertThat(received).isEqualTo("a1" to CallerTenant)
  }

  @Test
  fun `preview answers a caller without ReadData with 403`() = testApplication {
    application {
      installRoutes(capabilities = UserCapabilities(setOf(CoreCapability.WriteData.key)), findPreview = { _, _ -> AuthorPreview(name = "Mark Twain") })
    }

    assertThat(client.get("/api/authors/a1/preview").status).isEqualTo(HttpStatusCode.Forbidden)
  }

  @Test
  fun `preview answers a caller with ReadData alone`() = testApplication {
    application {
      installRoutes(capabilities = UserCapabilities(setOf(CoreCapability.ReadData.key)), findPreview = { _, _ -> AuthorPreview(name = "Mark Twain") })
    }

    assertThat(client.get("/api/authors/a1/preview").status).isEqualTo(HttpStatusCode.OK)
  }

  @Test
  fun `the preview route names the entity`() = testApplication {
    application {
      installRoutes()
    }
    startApplication()

    assertThat(application.openApiRoute("Get the preview of an author by ID").description).isEqualTo(
      "Returns the projection another feature embeds, for a caller that holds the author id alone. Requires ReadData."
    )
  }

  private fun previewBody(preview: AuthorPreview?): String = responseBody({ get("/api/authors/a1/preview") }) {
    installRoutes(findPreview = { _, _ -> preview })
  }

  private fun archiveBody(result: ArchiveResult<AuthorReference>): String = responseBody({ post("/api/authors/a1/archive") }) {
    installRoutes(archiveResult = result)
  }

  private fun unarchiveBody(result: UnarchiveResult): String = responseBody({ post("/api/authors/a1/unarchive") }) {
    installRoutes(unarchiveResult = result)
  }

  private fun responseBody(send: suspend HttpClient.() -> HttpResponse, configure: Application.() -> Unit): String {
    var body = ""
    testApplication {
      application(configure)
      body = client.send().bodyAsText()
    }
    return body
  }

  private fun Application.installRoutes(
    capabilities: UserCapabilities = UserCapabilities(setOf(CoreCapability.ReadData.key, CoreCapability.WriteData.key)),
    archiveResult: ArchiveResult<AuthorReference> = ArchiveResult.NotFound,
    archive: suspend (String, Tenant.Id) -> ArchiveResult<AuthorReference> = { _, _ -> archiveResult },
    unarchiveResult: UnarchiveResult = UnarchiveResult.NotFound,
    unarchive: suspend (String, Tenant.Id) -> UnarchiveResult = { _, _ -> unarchiveResult },
    descriptionNote: String? = null,
    capability: Capability = CoreCapability.WriteData,
    findPreview: suspend (String, Tenant.Id) -> AuthorPreview? = { _, _ -> null },
  ) {
    installServerPlugins(capabilities)

    routing {
      archiveEntityRoute<AuthorReference, String>(
        pattern = ArchivePattern,
        unarchivePattern = UnarchivePattern,
        label = AuthorLabel,
        tenantId = { CallerTenant },
        idFromPath = { getOrFail("authorId") },
        archive = archive,
        descriptionNote = descriptionNote,
        capability = capability,
      )

      unarchiveEntityRoute<String>(
        pattern = UnarchivePattern,
        label = AuthorLabel,
        tenantId = { CallerTenant },
        idFromPath = { getOrFail("authorId") },
        unarchive = unarchive,
        capability = capability,
      )

      previewEntityRoute<AuthorPreview, String>(
        pattern = PreviewPattern,
        label = AuthorLabel,
        tenantId = { CallerTenant },
        idFromPath = { getOrFail("authorId") },
        findPreview = findPreview,
        successExample = FoundResponse.good(AuthorPreview(name = "Mark Twain")),
      )
    }
  }

  private fun Application.installServerPlugins(capabilities: UserCapabilities) {
    installStatusPages(logger = mockk(relaxed = true))
    installJson { }
    installFeatureFlagSupport()
    installCapabilityChecks { capabilities }
  }

  private fun Application.openApiRoute(summary: String): OpenApiRoute = openApiRoutes().single { it.summary == summary }

  private enum class AuthorReference {
    Book,
  }

  private enum class NoReference

  private data object EditCatalog : Capability {
    override val key: CapabilityKey = CapabilityKey("test:edit-catalog")
  }

  @Serializable
  private data class AuthorPreview(val name: String)

  private companion object {
    val ById: UrlPattern.Relative1 = Url.pattern("api/authors") + UrlParameterName("authorId")
    val ArchivePattern: UrlPattern.Relative1 = ById + "archive"
    val UnarchivePattern: UrlPattern.Relative1 = ById + "unarchive"
    val PreviewPattern: UrlPattern.Relative1 = ById + "preview"
    val AuthorLabel: EntityRouteLabel = EntityRouteLabel(singular = "author", article = "an")
    val CallerTenant: Tenant.Id = Tenant.Id.random()
  }
}
