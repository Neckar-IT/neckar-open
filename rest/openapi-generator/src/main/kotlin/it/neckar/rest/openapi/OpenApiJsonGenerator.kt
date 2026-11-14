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
@file:Suppress("IdentifierGrammar")
@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)

package it.neckar.rest.openapi

import it.neckar.open.annotations.NestingLevel
import it.neckar.open.collections.fastForEach
import it.neckar.open.http.Url
import it.neckar.open.kotlin.serializers.putNotNull
import it.neckar.rest.openapi.schema.SchemaDescriptor.ComponentSchema.ReferenceType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.reflect.KClass
import kotlin.reflect.KType
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.OpenApiExample
import it.neckar.rest.openapi.dsl.OpenApiRoute
import it.neckar.rest.openapi.dsl.OperationId
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.SchemaPropertyConstraints
import it.neckar.rest.openapi.transform.OpenApiDocument
import it.neckar.rest.openapi.transform.OpenApiDocumentTransformer
import it.neckar.rest.openapi.transform.OpenApiDocumentTransformer.Companion.applyTo


/**
 * Generates the JSON for the openapi spec.
 *
 * Naming conventions:
 * * `generate*`: Generates [JsonElement]s that can then be added/used
 * * ``build*`: Extension methods on [JsonElement] that add something to the element directly. Does *not* return a [JsonElement]
 *
 * The methods should indicate in which depths they are used:
 *
 * Methods that generate only parts of the JSON are marked as internal.
 */
class OpenApiJsonGenerator(
  /**
   * Callback that provides the (optional) operation id for the generated operation.
   */
  val operationIdProvider: (route: OpenApiRoute) -> OperationId? = { route ->
    route.operationId ?: OperationIdGenerator.generateOperationId(route.method, route.path)
  },

  /**
   * The documentation provider that is used to resolve the documentation that is added to the generated openapi file.
   */
  val documentationProvider: DocumentationProvider,

  /**
   * Callback that provides the (optional) tag for a route. When non-null, the route
   * gets a `tags` array and the tag is included in the top-level `tags` section with
   * an `x-displayName` for Scalar's sidebar navigation.
   *
   * Default: no tags (for backwards compatibility with existing spec tests).
   */
  val tagProvider: (route: OpenApiRoute) -> OpenApiTag? = { null },

  /**
   * The deviations from the default mapping, applied in order to every generated document.
   */
  val documentTransformers: List<OpenApiDocumentTransformer> = OpenApiDocumentTransformer.Defaults,
) {

  /**
   * The collaborators form a small graph rather than routing through this facade:
   *
   * ```
   * metadataWriter      (leaf)
   * propertyGenerator   -> metadataWriter, schemaGenerator (cycle, wired via lateinit)
   * schemaGenerator     -> metadataWriter, propertyGenerator
   * responseGenerator   -> metadataWriter, schemaGenerator
   * pathGenerator       -> schemaGenerator, responseGenerator
   * ```
   *
   * Property and schema generation are mutually recursive, so [OpenApiPropertyGenerator.schemaGenerator]
   * is injected after both are constructed. Every other edge is a plain constructor dependency.
   */
  private val metadataWriter: OpenApiMetadataWriter = OpenApiMetadataWriter(documentationProvider)

  private val propertyGenerator: OpenApiPropertyGenerator = OpenApiPropertyGenerator(documentationProvider, metadataWriter)

  private val schemaGenerator: OpenApiSchemaGenerator = OpenApiSchemaGenerator(documentationProvider, metadataWriter, propertyGenerator)
    .also { propertyGenerator.schemaGenerator = it }

  private val responseGenerator: OpenApiResponseGenerator = OpenApiResponseGenerator(schemaGenerator, metadataWriter)

  private val pathGenerator: OpenApiPathGenerator = OpenApiPathGenerator(schemaGenerator, responseGenerator, operationIdProvider, tagProvider)

  /*
   * Low-level generation API.
   *
   * These methods forward to the individual collaborators. They are the deliberate seam that the
   * generator's own fine-grained test suite (and the ktor bridge module) exercises to assert the
   * schema/path/response building blocks in isolation. Production code only calls [generate].
   */

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putKotlinSourceRef(schemaType: SchemaDescriptor) = metadataWriter.putKotlinSourceRef(schemaType)

  internal fun generatePropertyStatementForSchema(
    context: OpenApiPropertyGenerator.NamedPropertyCreationContext,
    propertyType: KType,
    propertyDescriptor: SerialDescriptor,
    forceType: KClass<*>?,
    schemaConstraints: SchemaPropertyConstraints,
    registerCompositeType: (type: SchemaDescriptor) -> Unit,
  ) = propertyGenerator.generatePropertyStatementForSchema(context, propertyType, propertyDescriptor, forceType, schemaConstraints, registerCompositeType)

  context(jsonObjectBuilder: JsonObjectBuilder)
  fun buildSealedClassPropertyElement(elementDescriptor: SerialDescriptor, forcedType: KClass<*>?, propertyType: KType, schemaConstraints: SchemaPropertyConstraints, registerCompositeType: (type: SchemaDescriptor) -> Unit) =
    propertyGenerator.buildSealedClassPropertyElement(elementDescriptor, forcedType, propertyType, schemaConstraints, registerCompositeType)

  context(jsonObjectBuilder: JsonObjectBuilder)
  fun buildComponentsSchemas(initialSchemaTypes: List<SchemaDescriptor>) = schemaGenerator.buildComponentsSchemas(initialSchemaTypes)

  context(jsonObjectBuilder: JsonObjectBuilder)
  fun buildSchemaType(componentSchema: SchemaDescriptor.ComponentSchema, registerCompositeType: (type: SchemaDescriptor) -> Unit) =
    schemaGenerator.buildSchemaType(componentSchema, registerCompositeType)

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildSchemaTypeForObject(clazz: KClass<*>, requiredPropertiesMode: RequiredPropertiesMode, registerCompositeType: (type: SchemaDescriptor) -> Unit) =
    schemaGenerator.buildSchemaTypeForObject(clazz, requiredPropertiesMode, registerCompositeType)

  context(jsonObjectBuilder: JsonObjectBuilder)
  fun buildSchemaTypeUsingSerialDescriptor(type: KType, registerCompositeType: (type: SchemaDescriptor) -> Unit) =
    schemaGenerator.buildSchemaTypeUsingSerialDescriptor(type, registerCompositeType)

  internal fun collectTypesToGenerate(routes: List<OpenApiRoute>): List<SchemaDescriptor> =
    schemaGenerator.collectTypesToGenerate(routes)

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putRef(type: SchemaDescriptor.ComponentSchema, schemaConstraints: SchemaPropertyConstraints, referenceType: ReferenceType) =
    schemaGenerator.putRef(type, schemaConstraints, referenceType)

  context(jsonObjectBuilder: JsonObjectBuilder)
  fun putSchemaTypeOrReference(schemaType: SchemaDescriptor) = schemaGenerator.putSchemaTypeOrReference(schemaType)

  fun generateSchemaTypeOrReference(schemaType: SchemaDescriptor): JsonObject =
    schemaGenerator.generateSchemaTypeOrReference(schemaType)

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildSchemaTypeForClass(type: KType, registerCompositeType: (type: SchemaDescriptor) -> Unit) =
    schemaGenerator.buildSchemaTypeForClass(type, registerCompositeType)

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildSchemaTypeForDataClass(type: KType, registerCompositeType: (type: SchemaDescriptor) -> Unit) =
    schemaGenerator.buildSchemaTypeForDataClass(type, registerCompositeType)

  fun generatePathEntry(routesForSamePath: List<OpenApiRoute>): JsonElement =
    pathGenerator.generatePathEntry(routesForSamePath)

  fun generateRouteDetails(route: OpenApiRoute): JsonElement =
    pathGenerator.generateRouteDetails(route)

  fun generateSchemaTypeRef(schemaType: SchemaDescriptor, examples: List<OpenApiExample>): JsonObject =
    responseGenerator.generateSchemaTypeRef(schemaType, examples)

  /**
   * Generates a JSON for the provided routes.
   *
   * The generated JSON consists of the following sections:
   * * - `openapi`: The openapi version
   * * - `info`: The meta-data for the openapi file
   * * - `servers`: The servers that are used for the API
   * * - `paths`: The paths section that contains the routes (and references to the types in the components section)
   * * - `components`: The components section that contains the security schemes and the schemas (which are referenced in the paths section)
   *
   * The document then passes [documentTransformers].
   */
  @NestingLevel(0)
  fun generate(
    /**
     * The routes that are added to the openapi spec.
     * The referenced types are also added to the spec.
     */
    routes: List<OpenApiRoute>,
    /**
     * Additional schema types - that will be generated, too.
     * This is useful if the routes do not directly reference the types.
     */
    additionalTypes: List<SchemaDescriptor> = emptyList(),
    /**
     * The meta-data for the open api file
     */
    metaData: OpenApiMetaData,
  ): JsonElement {
    val document = buildJsonObject {
      //Updates for 3.1.0: https://beeceptor.com/docs/concepts/openapi-what-is-new-3.1.0/
      put("openapi", "3.1.0")

      putJsonObject("info") {
        put("title", metaData.title)
        putNotNull("summary", metaData.summary)
        put("description", metaData.description)
        put("version", metaData.version.format())
        putNotNull("termsOfService", metaData.termsOfService)
        metaData.contact?.let { contact ->
          putJsonObject("contact") {
            putNotNull("name", contact.name)
            putNotNull("url", contact.url?.value)
            putNotNull("email", contact.email)
          }
        }
        metaData.license?.let { license ->
          putJsonObject("license") {
            put("name", license.name)
            license.url?.let { put("url", it.value) }
          }
        }
      }
      putJsonArray("servers") {
        metaData.servers.fastForEach {
          addJsonObject {
            put("url", it.value)
          }
        }
      }

      put("paths", pathGenerator.generatePathsSection(routes))

      val routesWithTags = routes.mapNotNull { route ->
        tagProvider(route)?.let { tag -> route to tag }
      }

      val tags = routesWithTags.map { it.second }
        .distinctBy { it.name }
        .sortedBy { it.name }

      if (tags.isNotEmpty()) {
        putJsonArray("tags") {
          tags.forEach { tag ->
            addJsonObject {
              put("name", tag.name)
              put(AdditionalOpenApiKeys.DisplayName, tag.displayName)
            }
          }
        }

        val tagsByCategory: Map<OpenApiMetaData.Category, List<String>> = routesWithTags
          .map { (route, tag) -> route.category to tag.name }
          .distinct()
          .groupBy({ it.first }, { it.second })
          .mapValues { (_, tagNames) -> tagNames.distinct().sorted() }

        // Only emit x-tagGroups for categories with 2+ tags.
        // Single-tag categories would just produce redundant nesting (e.g. "Meta" → "Meta").
        val multiTagCategories = tagsByCategory.filter { it.value.size >= 2 }

        if (multiTagCategories.isNotEmpty()) {
          // Collect all tags that are already covered by a group
          val groupedTagNames = multiTagCategories.values.flatten().toSet()
          // Tags not in any group get their own single-tag group so Scalar doesn't hide them
          val ungroupedTags = tags.filter { it.name !in groupedTagNames }

          putJsonArray(AdditionalOpenApiKeys.TagGroups) {
            multiTagCategories.entries.sortedBy { it.key.value }.forEach { (category, tagNames) ->
              addJsonObject {
                put("name", category.toDisplayName())
                putJsonArray("tags") {
                  tagNames.forEach { add(it) }
                }
              }
            }
            ungroupedTags.forEach { tag ->
              addJsonObject {
                put("name", tag.displayName)
                putJsonArray("tags") {
                  add(tag.name)
                }
              }
            }
          }
        }
      }

      putJsonArray("security") {
        addJsonObject {
          putJsonArray("bearerAuth") {}
        }
      }

      putJsonObject("components") {
        putJsonObject("securitySchemes") {
          generateSecuritySchemas(metaData.oAuth2Config)
        }

        put("schemas", generateComponentsSchemasForRoutes(routes, additionalTypes))
      }
    }

    return documentTransformers.applyTo(OpenApiDocument(document)).json
  }

  /**
   * Generates the security schemas.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  private fun generateSecuritySchemas(oAuth2Config: OAuth2Config?) {
    jsonObjectBuilder.putJsonObject("bearerAuth") {
      put("type", "http")
      put("scheme", "bearer")
      put("bearerFormat", "JWT")
    }

    if (oAuth2Config != null) {
      jsonObjectBuilder.putJsonObject("oauth2") {
        put("type", "oauth2")
        putJsonObject("flows") {
          putJsonObject("authorizationCode") {
            put("authorizationUrl", oAuth2Config.authorizationUrl.value)
            put("tokenUrl", oAuth2Config.tokenUrl.value)
            put("x-scalar-client-id", oAuth2Config.clientId)
            putJsonObject("scopes") {
              oAuth2Config.scopes.forEach { (scope, description) ->
                put(scope, description)
              }
            }
          }
        }
      }
    }
  }

  /**
   * Generates the `components/schemas` section for the given routes plus any additional types.
   */
  internal fun generateComponentsSchemasForRoutes(
    /**
     * The routes that are used to identify the types that need to be generated.
     */
    routes: List<OpenApiRoute>,
    /**
     * Additional types that will be generated (even if not referenced in routes).
     */
    additionalTypes: List<SchemaDescriptor>,
  ): JsonObject {
    //The types that are referenced in the routes (directly)
    val typesFromRoutes = collectTypesToGenerate(routes)

    //Contains all types that need to be generated. The list is updated during the generation, if necessary
    val typesToGenerate = buildList {
      addAll(typesFromRoutes)
      addAll(additionalTypes)
    }.toMutableList()


    return generateComponentsSchemas(typesToGenerate)
  }

  /**
   * Generates *all* schemas that are referenced in the components section.
   *
   * Also creates schemas for types that are referenced by other types.
   */
  internal fun generateComponentsSchemas(initialSchemaTypes: List<SchemaDescriptor>): JsonObject {
    return buildJsonObject {
      buildComponentsSchemas(initialSchemaTypes)
    }
  }

  companion object {
    /**
     * Creates a new instance of [OpenApiJsonGenerator] with the provided base path.
     */
    operator fun invoke(
      basePath: Url.Relative,
      documentationProvider: DocumentationProvider,
      documentTransformers: List<OpenApiDocumentTransformer> = OpenApiDocumentTransformer.Defaults,
    ): OpenApiJsonGenerator {
      val basePathParts = basePath.value.split("/").toSet()

      return OpenApiJsonGenerator(
        operationIdProvider = { route ->
          route.operationId ?: OperationIdGenerator.generateOperationId(route.method, route.path) {
            basePathParts.contains(it)
          }
        },
        documentationProvider = documentationProvider,
        documentTransformers = documentTransformers,
      )
    }

    /**
     * Creates a new instance with tags enabled. The tags are derived from the route
     * path segments (first segment after skipping the [basePath] parts).
     *
     * Used by the ktor `OpenApiPlugin` to provide Scalar sidebar navigation.
     */
    fun withTags(
      basePath: Url.Relative,
      documentationProvider: DocumentationProvider,
      documentTransformers: List<OpenApiDocumentTransformer> = OpenApiDocumentTransformer.Defaults,
    ): OpenApiJsonGenerator {
      val basePathParts = basePath.value.split("/").toSet()
      val skipPrefixCondition: (String) -> Boolean = { basePathParts.contains(it) }

      return OpenApiJsonGenerator(
        operationIdProvider = { route ->
          route.operationId ?: OperationIdGenerator.generateOperationId(route.method, route.path, skipPrefixCondition)
        },
        documentationProvider = documentationProvider,
        tagProvider = { route ->
          route.tag ?: OpenApiTagGenerator.generateTag(route.path, skipPrefixCondition)
        },
        documentTransformers = documentTransformers,
      )
    }
  }

}

/**
 * Converts a category value to a human-readable display name for x-tagGroups.
 *
 * `default` → `API`, `admin` → `Admin`, `meta` → `Meta`
 */
private fun OpenApiMetaData.Category.toDisplayName(): String {
  return when (value) {
    "default" -> "API"
    else -> value.replaceFirstChar { it.uppercase() }
  }
}
