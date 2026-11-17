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
package it.neckar.rest.openapi.transform

import it.neckar.open.kotlin.lang.checkNotNull
import it.neckar.rest.RestResponse
import it.neckar.rest.openapi.schema.SchemaDescriptor.ClassSchemaWithDiscriminatorObject
import it.neckar.rest.openapi.transform.OpenApiDocument.Companion.rewriteReferences
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Publishes every `bad/<Name>` variant that recurs across the responses of one document with one shape as one component `BadResponse.<Name>`
 * (plus `.WithType` for a data class), so Orval writes one TypeScript type per recurring error instead of one per response.
 *
 * The `oneOf` and `discriminator.mapping` of each response keep their entries, which keeps the exhaustive `switch` per endpoint.
 * A name with several shapes stays per response, e.g. `bad/ValidationFailure` with each entity's own error type.
 * Merging is decided per document: a category document may keep a variant per response that the full document merges.
 */
object MergeRecurringBadResponseVariants : OpenApiDocumentTransformer {
  private const val ComponentPrefix: String = "BadResponse."

  /**
   * `OpenApiSchemaGenerator.validateRestResponseDiscriminator` requires `"bad/" + simpleName` for a [RestResponse.BadResponse].
   */
  private const val BadDiscriminatorPrefix: String = "bad/"

  /**
   * The simple class name a [RestResponse.BadResponse] variant carries behind [BadDiscriminatorPrefix]; it becomes part of a `$ref`.
   */
  private val VariantSimpleName: Regex = Regex("[\\p{L}_][\\p{L}\\p{N}_]*")

  override fun transform(document: OpenApiDocument): OpenApiDocument {
    val renames = computeRenames(document.schemas)
    if (renames.isEmpty()) {
      return document
    }

    val rewritten = document.rewriteReferences(renames)
    val rewrittenSchemas = rewritten.schemas
    val membersByMergedName: Map<ComponentName, List<ComponentName>> = renames.keys.groupBy { renames.getValue(it) }

    val mergedSchemas = buildMap {
      rewrittenSchemas.forEach { (name, schema) ->
        val mergedName = renames[name]
        when {
          mergedName == null -> put(name, schema)
          mergedName in this -> Unit
          else -> put(mergedName, SchemaShape.merge(membersByMergedName.getValue(mergedName).map { rewrittenSchemas.getValue(it) }).jsonObject)
        }
      }
    }

    return rewritten.withSchemas(mergedSchemas)
  }

  /**
   * Maps the name of every component that moves to its merged name.
   */
  private fun computeRenames(schemas: Map<ComponentName, JsonObject>): Map<ComponentName, ComponentName> {
    val variantsByDiscriminator: Map<String, Set<ComponentName>> = schemas.values
      .asSequence()
      .mapNotNull { schema -> schema["discriminator"]?.jsonObject }
      .flatMap { discriminator -> discriminator["mapping"].checkNotNull { "A discriminator without mapping: $discriminator" }.jsonObject.entries }
      .filter { (discriminatorValue, _) -> discriminatorValue.startsWith(BadDiscriminatorPrefix) }
      .groupBy({ it.key }, { it.value.componentName() })
      .mapValues { (_, variantNames) -> variantNames.toSet() }

    return buildMap {
      variantsByDiscriminator.forEach { (discriminatorValue, variantNames) ->
        if (variantNames.size < 2) {
          return@forEach
        }

        val variantComponentsByName = variantNames.associateWith { variantName -> schemas.variantComponents(variantName) }
        if (variantComponentsByName.values.map { it.shape(schemas) }.distinct().size > 1) {
          return@forEach
        }

        val variantSimpleName = discriminatorValue.removePrefix(BadDiscriminatorPrefix)
        check(VariantSimpleName.matches(variantSimpleName)) {
          "The discriminator [$discriminatorValue] of ${variantNames.sortedBy { it.escaped }} does not name a class after [$BadDiscriminatorPrefix], so it cannot become the component name [$ComponentPrefix<Name>]. Declare [$BadDiscriminatorPrefix] + the simple class name, as RestResponse.BadResponse variants do."
        }
        val mergedBaseName = ComponentName.ofUnescaped(ComponentPrefix + variantSimpleName)
        val mergedDiscriminatedName = ComponentName.ofUnescaped(ComponentPrefix + variantSimpleName + ClassSchemaWithDiscriminatorObject.DefaultReferenceSuffix)
        val groupComponentNames = variantComponentsByName.values.flatMap { listOfNotNull(it.variantName, it.baseName) }
        check(listOf(mergedBaseName, mergedDiscriminatedName).none { it in schemas && it !in groupComponentNames }) {
          "The component [$mergedBaseName] or [$mergedDiscriminatedName] already exists in the schema, so the recurring variant [$discriminatorValue] of ${variantNames.sortedBy { it.escaped }} cannot be merged under it. Rename the class that publishes it."
        }

        variantComponentsByName.forEach { (variantName, variantComponents) ->
          val baseName = variantComponents.baseName
          if (baseName == null) {
            put(variantName, mergedBaseName)
          } else {
            put(variantName, mergedDiscriminatedName)
            put(baseName, mergedBaseName)
          }
        }
      }
    }
  }

  /**
   * The component a variant is published as, plus the component its `.WithType` wrapper extends.
   */
  private class VariantComponents(val variantName: ComponentName, val baseName: ComponentName?) {
    /**
     * The variant with its own base inlined; other references stay names.
     */
    fun shape(schemas: Map<ComponentName, JsonObject>): JsonElement {
      val variant = schemas.getValue(variantName)
      if (baseName == null) {
        return SchemaShape.of(variant)
      }
      return SchemaShape.of(variant.rewriteReferences(renames = emptyMap(), inline = { name -> if (name == baseName) schemas.getValue(baseName) else null }))
    }
  }

  /**
   * A data class variant is published as `.WithType` wrapper whose `allOf` holds its base alone; a data object is its own component.
   */
  private fun Map<ComponentName, JsonObject>.variantComponents(variantName: ComponentName): VariantComponents {
    val baseName = ComponentName(variantName.escaped.removeSuffix(ClassSchemaWithDiscriminatorObject.DefaultReferenceSuffix))
    val extendsOwnBase = baseName != variantName &&
      getValue(variantName)["allOf"] == JsonArray(listOf(JsonObject(mapOf($$"$ref" to JsonPrimitive(baseName.reference)))))

    if (extendsOwnBase.not()) {
      return VariantComponents(variantName, baseName = null)
    }

    check(baseName in this) { "The discriminated component [$variantName] has no base component [$baseName]" }
    return VariantComponents(variantName, baseName)
  }

  private fun JsonElement.componentName(): ComponentName {
    check(this is JsonPrimitive && isString) { "Not a component reference: [$this]" }
    return ComponentName.fromReference(content)
  }
}
