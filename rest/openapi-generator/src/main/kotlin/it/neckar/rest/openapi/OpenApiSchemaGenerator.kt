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

import it.neckar.open.collections.fastForEach
import it.neckar.open.kotlin.serializers.AnySerializableRawSerializer
import it.neckar.open.kotlin.lang.asEnumClass
import it.neckar.open.kotlin.lang.asKClass
import it.neckar.open.kotlin.lang.findKClass
import it.neckar.open.kotlin.lang.enumEntries
import it.neckar.open.kotlin.lang.getAllSealedSubclasses
import it.neckar.open.kotlin.lang.getValueForced
import it.neckar.open.kotlin.lang.hasBackingField
import it.neckar.open.kotlin.lang.isEnum
import it.neckar.open.kotlin.lang.isInterface
import it.neckar.open.kotlin.lang.isObject
import it.neckar.open.kotlin.lang.isTransient
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.kotlin.lang.simpleNameWithEnclosing
import it.neckar.open.kotlin.serializers.findSerialName
import it.neckar.open.kotlin.serializers.getSerialName
import it.neckar.open.kotlin.serializers.isAnnotatedAsSerializable
import it.neckar.open.kotlin.serializers.putNotEmpty
import it.neckar.open.kotlin.serializers.putNotNull
import it.neckar.open.kotlin.serializers.serializer
import it.neckar.rest.RestResponse
import it.neckar.rest.openapi.schema.SchemaDescriptor.ComponentSchema.ReferenceType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import it.neckar.rest.openapi.schema.ComponentSchemaReference
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.OpenApiExample
import it.neckar.rest.openapi.dsl.OpenApiRoute
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.SchemaPropertyConstraints
import it.neckar.rest.openapi.schema.SchemaTypeProcessingQueue
import it.neckar.rest.openapi.schema.TypeDiscriminator
import it.neckar.rest.openapi.schema.isInline
import it.neckar.rest.openapi.schema.makeNotNullable
import it.neckar.rest.openapi.documentation.requiredElementNamesForOpenApi
import it.neckar.rest.openapi.schema.isReferenced
import it.neckar.rest.openapi.schema.hasOpenApiBaseSchemaAnnotation

/**
 * Helper class for schema type generation in OpenAPI.
 *
 * This class handles:
 * - Schema type generation for classes, interfaces, objects
 * - Enum schema generation
 * - Sealed hierarchy handling
 * - Schema references and discriminators
 */
internal class OpenApiSchemaGenerator(
  private val documentationProvider: DocumentationProvider,
  private val metadataWriter: OpenApiMetadataWriter,
  private val propertyGenerator: OpenApiPropertyGenerator,
) {

  /**
   * Base schema properties collected over every implementing data class, keyed by the `@OpenApiBaseSchema` interface.
   * An implementer answering a property through a getter (`get() = null`) does not write it, so the first
   * implementer alone would publish whichever subset it happens to serialize.
   */
  private val cachedBaseSchemaProperties = mutableMapOf<KClass<*>, CachedBaseSchemaData>()

  private class CachedBaseSchemaData {
    val properties: MutableMap<String, JsonElement> = mutableMapOf()
    private val requiredNames: MutableSet<String> = mutableSetOf()

    /** Taken from an implementer that narrows the interface's nullable type — an implementer declaring it as the interface does replaces it. */
    private val tightenedNames: MutableSet<String> = mutableSetOf()

    val requiredPropertyNames: List<String>
      get() = properties.keys.filter { it in requiredNames }

    fun offer(name: String, schema: JsonElement, required: Boolean, tightened: Boolean) {
      val replaces = name !in properties || (name in tightenedNames && !tightened)
      if (!replaces) {
        return
      }
      properties[name] = schema
      if (required) requiredNames.add(name) else requiredNames.remove(name)
      if (tightened) tightenedNames.add(name) else tightenedNames.remove(name)
    }
  }

  context(jsonObjectBuilder: JsonObjectBuilder)
  fun buildComponentsSchemas(initialSchemaTypes: List<SchemaDescriptor>) {
    val queue = SchemaTypeProcessingQueue()
    queue.enqueueAllIfNecessary(initialSchemaTypes)

    //The class that claimed each component-schema name so far
    val schemaNameClaims = mutableMapOf<String, SchemaNameClaim>()

    //The class whose schema first referenced each registered type; an initial type has none
    val referencedBy = mutableMapOf<SchemaDescriptor, KClass<*>>()

    //The objects that were written as a message: a catalog schema of the same object must not overwrite it
    val writtenMessageObjects = mutableSetOf<KType>()

    //A base schema is written from its implementers, so it waits until every implementer has been processed
    val interfaceBaseSchemas = mutableListOf<SchemaDescriptor.InterfaceBaseSchema>()

    while (queue.hasNext) {
      val schemaType = queue.next()

      if ((schemaType is SchemaDescriptor.ComponentSchema).not()) {
        //Skip the non-referenced schema types
        continue
      }

      if (schemaType is SchemaDescriptor.MessageObjectSchema) {
        writtenMessageObjects.add(schemaType.type)
      } else if (schemaType is SchemaDescriptor.ConstantCatalogSchemaObject && writtenMessageObjects.contains(schemaType.type)) {
        continue
      }

      val referrer = claimSchemaName(schemaType, referencedBy[schemaType], schemaNameClaims) ?: referencedBy[schemaType]

      if (schemaType is SchemaDescriptor.InterfaceBaseSchema) {
        interfaceBaseSchemas.add(schemaType)
        continue
      }

      buildSchemaType(schemaType) {
        if (referrer != null) {
          referencedBy.putIfAbsent(it, referrer)
        }
        queue.enqueueIfNecessary(it)
      }
    }

    interfaceBaseSchemas.fastForEach { interfaceBaseSchema ->
      buildSchemaType(interfaceBaseSchema) {
        throw IllegalStateException("The base schema [${interfaceBaseSchema.reference.valueEscaped()}] registered [$it] - a base schema is written from its cached implementer properties and must not register further types.")
      }
    }
  }

  private fun SchemaDescriptor.ConstSchemaObject.requiredPropertiesMode(): RequiredPropertiesMode {
    return when (this) {
      is SchemaDescriptor.MessageObjectSchema -> RequiredPropertiesMode.OnlyDiscriminator
      is SchemaDescriptor.ConstantCatalogSchemaObject -> RequiredPropertiesMode.AllPropertiesWithValue
    }
  }

  private class SchemaNameClaim(val clazz: KClass<*>, val referencedBy: KClass<*>?) {
    fun describe(): String {
      return "[${clazz.qualifiedName}] " + if (referencedBy == null) "(named by an endpoint)" else "(referenced by [${referencedBy.qualifiedName}])"
    }
  }

  /**
   * Records which class publishes [componentSchema] under its name and throws when another class already
   * published under it: the second entry would overwrite the first, and the document would then describe
   * one type under the other's name.
   *
   * Returns the claiming class, or `null` for a wrapper that publishes nothing itself.
   */
  private fun claimSchemaName(componentSchema: SchemaDescriptor.ComponentSchema, referencedBy: KClass<*>?, schemaNameClaims: MutableMap<String, SchemaNameClaim>): KClass<*>? {
    val name: String
    val clazz: KClass<*>

    when (componentSchema) {
      //Publishes nothing itself, only registers the two schemas it wraps
      is SchemaDescriptor.DiscriminatedClassSchemaWrapper -> return null

      is SchemaDescriptor.ConstSchemaObject -> {
        name = componentSchema.reference(ReferenceType.Plain).valueEscaped()
        clazz = componentSchema.asClass()
      }

      is SchemaDescriptor.ClassSchemaObject -> {
        name = componentSchema.reference(ReferenceType.Plain).valueEscaped()
        clazz = componentSchema.asClass()
      }

      is SchemaDescriptor.ClassSchemaWithDiscriminatorObject -> {
        name = componentSchema.reference(ReferenceType.Discriminated).valueEscaped()
        clazz = componentSchema.baseSchema.asClass()
      }

      is SchemaDescriptor.InterfaceBaseSchema -> {
        name = componentSchema.reference(ReferenceType.Plain).valueEscaped()
        clazz = componentSchema.asClass()
      }

      is SchemaDescriptor.EnumSchema -> {
        name = componentSchema.reference(ReferenceType.Plain).valueEscaped()
        clazz = componentSchema.asClass()
      }

      is SchemaDescriptor.ComponentEnumSchemaObject -> {
        name = componentSchema.reference(ReferenceType.Plain).valueEscaped()
        clazz = componentSchema.asClass()
      }

      is SchemaDescriptor.EnumEntrySchema -> {
        name = componentSchema.reference(ReferenceType.Plain).valueEscaped()
        clazz = componentSchema.asClass()
      }
    }

    val claim = SchemaNameClaim(clazz, referencedBy)
    val previousClaim = schemaNameClaims.getOrPut(name) { claim }

    if (previousClaim.clazz != clazz) {
      throw IllegalStateException(
        "Two types claim the schema name [$name]: ${previousClaim.describe()} and ${claim.describe()}. Rename one of the two classes - a schema name describes exactly one type."
      )
    }

    return clazz
  }

  /**
   * Appends the type to the schemas' entry.
   * Is called with this JSON context: `components/schemas`
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  fun buildSchemaType(
    /**
     * The ref type to generate
     */
    componentSchema: SchemaDescriptor.ComponentSchema,
    /**
     * Is called for each new type detected during the generation of the code.
     * A reference to each type needs to be added.
     */
    registerCompositeType: (type: SchemaDescriptor) -> Unit,
  ) {
    when (componentSchema) {
      is SchemaDescriptor.ConstSchemaObject -> {
        val clazz = componentSchema.asClass()

        jsonObjectBuilder.put(clazz.simpleNameWithEnclosing, buildJsonObject {
          buildSchemaTypeForObject(clazz, componentSchema.requiredPropertiesMode(), registerCompositeType)
        })
      }

      is SchemaDescriptor.EnumSchema -> {
        val clazz = componentSchema.asClass()

        when {
          clazz.isEnum() -> {
            jsonObjectBuilder.put(clazz.simpleNameWithEnclosing, buildJsonObject {
              val entries = clazz.asEnumClass().enumEntries
              val values = entries.map { componentSchema.valueName(it) }
              // Per-value descriptions from the enum constants' KDoc, keyed by the emitted enum value.
              val descriptions = entries.mapNotNull { entry ->
                documentationProvider.getDocumentationForEnumEntry(clazz, entry.name)?.let { componentSchema.valueName(entry) to it }
              }.toMap()
              val germanTerms = entries.mapNotNull { entry ->
                documentationProvider.getGermanTermForEnumEntry(clazz, entry.name)?.let { componentSchema.valueName(entry) to it }
              }.toMap()
              propertyGenerator.putEnumType(values, descriptions, germanTerms)
              metadataWriter.putGermanTerm(documentationProvider.getGermanTermForClass(clazz))
            })
          }

          else -> {
            throw IllegalStateException("Expected an enum class but was $clazz")
          }
        }
      }

      is SchemaDescriptor.ComponentEnumSchemaObject -> {
        val enumClazz = componentSchema.asClass()

        jsonObjectBuilder.put(enumClazz.simpleNameWithEnclosing, buildJsonObject {
          putEnumAsObjectType(enumClazz as KClass<Enum<*>>, registerCompositeType)
        })
      }

      is SchemaDescriptor.EnumEntrySchema -> {
        val clazz: KClass<out Enum<*>> = componentSchema.asClass()

        jsonObjectBuilder.put(componentSchema.reference(referenceType = ReferenceType.Plain).valueEscaped(), buildJsonObject {
          putEnumEntryAsObjectType(clazz, componentSchema.enumEntry, registerCompositeType)
        })
      }

      is SchemaDescriptor.ClassSchemaObject -> {
        putClassSchemaObjectComponent(componentSchema.reference(referenceType = ReferenceType.Plain), componentSchema, registerCompositeType)
      }

      is SchemaDescriptor.DiscriminatedClassSchemaWrapper -> {
        //Schedule the schemas for generation
        registerCompositeType(componentSchema.baseSchema)
        registerCompositeType(componentSchema.schemaWithDiscriminator)
      }

      is SchemaDescriptor.ClassSchemaWithDiscriminatorObject -> {
        putClassSchemaWithDiscriminatorComponent(componentSchema.reference(referenceType = ReferenceType.Discriminated), componentSchema, registerCompositeType)
      }

      is SchemaDescriptor.InterfaceBaseSchema -> {
        putInterfaceBaseSchemaComponent(componentSchema, registerCompositeType)
      }
    }
  }

  context(jsonObjectBuilder: JsonObjectBuilder)
  private fun putSchemaObject(
    clazz: KClass<*>,
    /**
     * The instance of the object
     */
    instance: Any,
    /**
     * The forced property names that are allowed to be included in the schema.
     * These properties are added - even if they have no backing field and/or are transient
     */
    forcedPropertyNames: Set<String>,
    requiredPropertiesMode: RequiredPropertiesMode,
    /**
     * The callback to register further types
     */
    registerCompositeType: (type: SchemaDescriptor) -> Unit
  ) {
    require(clazz.isInstance(instance)) { "instance [$instance] must be of type [$clazz]" }

    jsonObjectBuilder.put("type", "object")
    metadataWriter.putKotlinSourceRef(clazz)
    metadataWriter.putDocumentation(documentationProvider.getDocumentationForType(clazz))
    metadataWriter.putGermanTerm(documentationProvider.getGermanTermForClass(clazz))

    val propertiesFilter: (KProperty1<out Any, *>) -> Boolean = { property ->
      forcedPropertyNames.contains(property.name) || //Include the forced properties
        property.hasBackingField && property.isTransient().not() //Do not include transient properties
    }

    jsonObjectBuilder.put("properties", buildJsonObject {
      val requiredPropertyNames = mutableListOf<String>()

      if (metadataWriter.putTypeDiscriminatorIfExists(clazz)) {
        //If the type discriminator was put, we need to add the "type" property as required
        requiredPropertyNames.add("type")
      }

      clazz.memberProperties
        .filter(propertiesFilter)
        .forEach { property ->
          put(property.name, buildJsonObject {
            metadataWriter.putDocumentation(documentationProvider.getDocumentationForProperty(clazz, property))
            metadataWriter.putGermanTerm(documentationProvider.getGermanTermForProperty(clazz, property.name))

            val returnType = property.returnType

            val constValue = property.getValueForced(instance) ?: return@forEach //Skip properties that have no value
            if (requiredPropertiesMode == RequiredPropertiesMode.AllPropertiesWithValue) {
              requiredPropertyNames.add(property.name)
            }

            when (val schemaDescriptor = SchemaDescriptor.of(returnType)) {
              is SchemaDescriptor.InlineSchemaValue -> {
                val nativeType = schemaDescriptor.asInline().toNativeType()
                put("type", nativeType.type)
                putNotNull("format", nativeType.format)
                metadataWriter.putKotlinSourceRef(returnType)
              }

              is SchemaDescriptor.ComponentSchema -> {
                put("type", "string") //always serialize as string
                metadataWriter.putKotlinSourceRef(returnType)
              }

              else -> {
                throw IllegalStateException("Unsupported schema type: $schemaDescriptor for property $property")
              }
            }

            putNotNull(
              "const", when (constValue) {
                is String -> {
                  JsonPrimitive(constValue)
                }

                is Number -> {
                  JsonPrimitive(constValue)
                }

                is Boolean -> {
                  JsonPrimitive(constValue)
                }

                is Collection<*> -> {
                  JsonArray(constValue.map {
                    it.toString()
                  }.map {
                    JsonPrimitive(it)
                  })
                }

                else -> {
                  JsonPrimitive(constValue.toString())
                }
              }
            )
          })
        }

      if (requiredPropertyNames.isNotEmpty()) {
        jsonObjectBuilder.put("required", buildJsonArray {
          requiredPropertyNames.fastForEach {
            add(it)
          }
        })
      }
    })

    clazz.memberProperties.filter { property ->
      propertiesFilter(property).not()
    }.map { it.name }.let { ignoredPropertyNames ->
      if (ignoredPropertyNames.isNotEmpty()) {
        jsonObjectBuilder.put(AdditionalOpenApiKeys.IgnoredProperties, buildJsonArray {
          ignoredPropertyNames.fastForEach { propertyName ->
            add(propertyName)
          }
        })
      }
    }
  }

  /**
   * Generates the ref type for objects (*NOT* classes)
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildSchemaTypeForObject(
    clazz: KClass<*>,
    requiredPropertiesMode: RequiredPropertiesMode,
    registerCompositeType: (type: SchemaDescriptor) -> Unit,
  ) {
    require(clazz.isObject) { "Only objects supported" }
    require(clazz.isInterface.not()) { "Interfaces not supported but was $clazz" }

    val objectInstance = clazz.objectInstance.requireNotNull { "Object instance for $clazz is null" }

    putSchemaObject(
      clazz = clazz, instance = objectInstance,
      forcedPropertyNames = emptySet(),
      requiredPropertiesMode = requiredPropertiesMode,
      registerCompositeType = registerCompositeType,
    )
  }

  /**
   * Builds the schema type using the serial descriptor.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  fun buildSchemaTypeUsingSerialDescriptor(type: KType, registerCompositeType: (type: SchemaDescriptor) -> Unit) {
    val clazz = type.asKClass()

    jsonObjectBuilder.put("type", "object")
    metadataWriter.putKotlinSourceRef(clazz)
    metadataWriter.putDocumentation(documentationProvider.getDocumentationForType(clazz))
    metadataWriter.putGermanTerm(documentationProvider.getGermanTermForClass(clazz))

    val serialDescriptor = type.serializer().descriptor


    // TODO re-enable discriminator support — `determineTypeDiscriminator(clazz)` was
    // disabled with `if (true) … null` and the alternative branch was unreachable. Keep the
    // current behavior (no discriminator) to avoid behavior changes in this cleanup.
    val typeDiscriminator: TypeDiscriminator? = null

    jsonObjectBuilder.put(
      "properties",
      propertyGenerator.generatePropertiesForSchema(
        context = OpenApiPropertyGenerator.PropertyCreationContext(clazz, type, serialDescriptor),
        typeDiscriminator = typeDiscriminator,
        registerCompositeType = registerCompositeType
      )
    )

    val requiredPropertyNames = buildList {
      if (typeDiscriminator != null) {
        add("type")
      }

      //Use requiredElementNamesForOpenApi to also mark computed properties as required
      addAll(serialDescriptor.requiredElementNamesForOpenApi(clazz))
    }

    jsonObjectBuilder.putNotEmpty("required", buildJsonArray {
      requiredPropertyNames
        .fastForEach { propertyName ->
          add(propertyName)
        }
    })
  }

  /**
   * Determines the type discriminator for the provided class.
   */
  private fun determineTypeDiscriminator(clazz: KClass<*>): TypeDiscriminator? {
    val sealedSerializableSuperTypes = clazz.supertypes
      .mapNotNull { it.findKClass() }
      .filter {
        it.isSealed && it.isInterface
      }.filter {
        it.isAnnotatedAsSerializable()
      }

    if (sealedSerializableSuperTypes.isEmpty()) {
      return null
    }

    val serialName = clazz.findSerialName()
    return TypeDiscriminator.ofNullable(serialName)
  }

  /**
   * Puts the enum object itself as an object type.
   *
   * Does *not* generate entries for the enum entries - instead adds references to the enum entries.
   * Automatically calls the provided [registerCompositeType] callback for each enum entry.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putEnumAsObjectType(clazz: KClass<Enum<*>>, registerCompositeType: (type: SchemaDescriptor) -> Unit) {
    require(clazz.isEnum()) { "Only enums are supported" }

    jsonObjectBuilder.put("type", "object")
    metadataWriter.putKotlinSourceRef(clazz)
    metadataWriter.putDocumentation(documentationProvider.getDocumentationForType(clazz))
    metadataWriter.putGermanTerm(documentationProvider.getGermanTermForClass(clazz))

    val enumEntries = clazz.asEnumClass().enumEntries
    val enumEntriesSchemas = enumEntries.map { enumEntry ->
      SchemaDescriptor.enumEntry(enumEntry).also { it ->
        registerCompositeType(it)
      }
    }

    jsonObjectBuilder.put("oneOf", buildJsonArray {
      enumEntriesSchemas.fastForEach { enumEntryType ->
        registerCompositeType(enumEntryType)

        addJsonObject {
          putRefTo(enumEntryType, ReferenceType.Discriminated)
        }
      }
    })

    jsonObjectBuilder.put("discriminator", buildJsonObject {
      put("propertyName", "name")
      put("mapping", buildJsonObject {
        enumEntries.fastForEach { entry ->
          val enumEntryType = SchemaDescriptor.enumEntry(entry)
          registerCompositeType(enumEntryType)

          put(entry.name, enumEntryType.reference(referenceType = ReferenceType.Plain).completeReference())
        }
      })
    })
  }

  /**
   * Collects the top level types that need to be generated from the request and response bodies
   */
  internal fun collectTypesToGenerate(routes: List<OpenApiRoute>): List<SchemaDescriptor> {
    return buildSet {
      routes.fastForEach { openApiRoute ->
        openApiRoute.request.body?.type?.let {
          add(it.asMessageIfObject())
        }

        addAll(openApiRoute.request.parameters.map { it.type })

        addAll(openApiRoute.responses.mapNotNull { it.body?.type?.asMessageIfObject() })
      }
    }.filter {
      it.isInline().not()
    }
  }

  /**
   * Puts a reference to the provided type
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putRef(type: SchemaDescriptor.ComponentSchema, schemaConstraints: SchemaPropertyConstraints, referenceType: ReferenceType) {
    if (schemaConstraints.showNullInType) {
      //Create "complicated" oneOf hierarchy to allow the property "nullable" at the same level
      jsonObjectBuilder.put("oneOf", buildJsonArray {
        addJsonObject {
          putRefTo(type, referenceType)
        }

        addJsonObject {
          put("type", "null") //must be a string!
        }
      })
    } else {
      putRefTo(type, referenceType)
    }
  }

  /**
   * Adds a schema type to the JSON context.
   *
   * This is only used for a route request body or a response body.
   * It should reference the schema type in the components section.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  fun putSchemaTypeOrReference(schemaType: SchemaDescriptor) {
    jsonObjectBuilder.put("schema", generateSchemaTypeOrReference(schemaType))
  }

  /**
   * Builds the schema type or a reference to the schema type.
   *
   * This is used
   */
  fun generateSchemaTypeOrReference(schemaType: SchemaDescriptor): JsonObject = buildJsonObject {
    when (schemaType) {
      is SchemaDescriptor.InlineEnumSchema -> {
        metadataWriter.putNativeType(schemaType.toNativeType(), SchemaPropertyConstraints.Required)
        putJsonArray("enum") {
          schemaType.allowedValues.forEach { add(it) }
        }
        metadataWriter.putKotlinSourceRef(schemaType)
      }

      is SchemaDescriptor.InlineSchemaValue -> {
        metadataWriter.putNativeType(schemaType.toNativeType(), SchemaPropertyConstraints.Required)
        metadataWriter.putKotlinSourceRef(schemaType)
      }

      is SchemaDescriptor.DiscriminatedClassSchemaWrapper -> {
        put($$"$ref", schemaType.baseSchema.reference(ReferenceType.Discriminated).completeReference())
      }

      is SchemaDescriptor.ComponentSchema -> {
        put($$"$ref", schemaType.reference(ReferenceType.Discriminated).completeReference())
        //Do not add the source type here, the source type is added in the components section
      }

      SchemaDescriptor.EmptySchema -> {
        //Do nothing
      }
    }
  }

  /**
   * Appends a reference to the provided type.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putRefTo(type: SchemaDescriptor.ComponentSchema, referenceType: ReferenceType) {
    putRef(type.reference(referenceType))
  }

  /**
   * Appends the provided reference.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  private fun putRef(reference: ComponentSchemaReference) {
    jsonObjectBuilder.put($$"$ref", reference.completeReference())
  }

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildSchemaTypeForClass(type: KType, registerCompositeType: (type: SchemaDescriptor) -> Unit) {
    val clazz = type.asKClass()
    require(clazz.isObject.not()) { "Objects not supported but was $clazz" }
    require(clazz.isInterface.not()) { "Interfaces not supported but was $clazz" }

    buildSchemaTypeUsingSerialDescriptor(type, registerCompositeType)
  }

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildSchemaTypeForInterface(type: KType, registerCompositeType: (type: SchemaDescriptor) -> Unit) {
    val clazz = type.asKClass()
    require(clazz.isInterface) { "Only interfaces supported but was $clazz" }

    buildSchemaTypeUsingSerialDescriptor(type, registerCompositeType)
  }

  context(jsonObjectBuilder: JsonObjectBuilder)
  private fun putClassSchemaObjectComponent(
    reference: ComponentSchemaReference,
    schemaObject: SchemaDescriptor.ClassSchemaObject,
    registerCompositeType: (type: SchemaDescriptor) -> Unit,
  ) {
    val type = schemaObject.type
    val clazz = type.asKClass()

    when {
      //A JsonElement is any JSON value there is - a value inside a free-form area, where the shape
      //belongs to whoever wrote it. The empty schema says exactly that, and the generated
      //TypeScript type stops at the boundary instead of inventing keys.
      clazz.isSubclassOf(JsonElement::class) -> {
        jsonObjectBuilder.put(reference.valueEscaped(), buildJsonObject {
          put("description", AnyJsonValueDescription)
          putSchemaLevelExamples(schemaObject.examples)
        })
      }

      clazz.isData && clazz.isObject.not() -> {
        val baseInterfaces = clazz.findOpenApiBaseSchemaInterfaces()
        if (baseInterfaces.isNotEmpty()) {
          jsonObjectBuilder.put(reference.valueEscaped(), buildJsonObject {
            buildDataClassWithAllOfComposition(type, baseInterfaces, registerCompositeType)
            putSchemaLevelExamples(schemaObject.examples)
          })
        } else {
          jsonObjectBuilder.put(reference.valueEscaped(), buildJsonObject {
            buildSchemaTypeForDataClass(type, registerCompositeType)
            putSchemaLevelExamples(schemaObject.examples)
          })
        }
      }

      clazz.isSealed -> {
        jsonObjectBuilder.put(reference.valueEscaped(), buildJsonObject {
          buildSchemaTypeForSealedHierarchy(schemaObject, type, registerCompositeType)
          putSchemaLevelExamples(schemaObject.examples)
        })
      }

      clazz.isSubclassOf(RestResponse.GoodResponse::class) -> {
        jsonObjectBuilder.put(reference.valueEscaped(), buildJsonObject {
          buildSchemaTypeUsingSerialDescriptor(type, registerCompositeType)
          putSchemaLevelExamples(schemaObject.examples)
        })
      }

      clazz.isSubclassOf(RestResponse.BadResponse::class) -> {
        jsonObjectBuilder.put(reference.valueEscaped(), buildJsonObject {
          buildSchemaTypeUsingSerialDescriptor(type, registerCompositeType)
          putSchemaLevelExamples(schemaObject.examples)
        })
      }

      clazz.isInterface -> {
        throw UnsupportedOperationException("Interfaces are not supported: $clazz. It is not possible to detect the subtypes.")
      }

      else -> {
        jsonObjectBuilder.put(reference.valueEscaped(), buildJsonObject {
          buildSchemaTypeForClass(type, registerCompositeType)
          putSchemaLevelExamples(schemaObject.examples)
        })
      }
    }
  }

  /**
   * Outputs the schema-level examples section if examples are present.
   *
   * Examples are output under the `x-examples` key in the schema definition - the standard `examples` key of a schema is an array,
   * the ids and summaries of the examples need an object:
   * ```json
   * {
   *   "MyType": {
   *     "type": "object",
   *     "properties": { ... },
   *     "x-examples": {
   *       "default": {
   *         "summary": "...",
   *         "value": { ... }
   *       }
   *     }
   *   }
   * }
   * ```
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  private fun putSchemaLevelExamples(examples: List<OpenApiExample>) {
    if (examples.isEmpty()) {
      return
    }

    jsonObjectBuilder.put("x-examples", buildJsonObject {
      examples.fastForEach { example ->
        put(example.exampleId.value, buildJsonObject {
          putNotNull("summary", example.summary)
          putNotNull("description", example.description)
          metadataWriter.putKotlinSourceRefIfNotNull(example.originalType)
          put("value", AnySerializableRawSerializer.encodeToJsonElement(example.example, includeOptionals = false))
        })
      }
    })
  }

  context(jsonObjectBuilder: JsonObjectBuilder)
  private fun putClassSchemaWithDiscriminatorComponent(
    reference: ComponentSchemaReference,
    componentSchema: SchemaDescriptor.ClassSchemaWithDiscriminatorObject,
    registerCompositeType: (SchemaDescriptor) -> Unit
  ) {
    val classSchemaObjectReference = componentSchema.baseSchema.reference(ReferenceType.Discriminated)
    val type = componentSchema.baseSchema.type
    val clazz = type.asKClass()


    jsonObjectBuilder.put(reference.valueEscaped(), buildJsonObject {
      put("type", "object")
      metadataWriter.putKotlinSourceRef(clazz)
      metadataWriter.putDocumentation(documentationProvider.getDocumentationForType(clazz))
      metadataWriter.putGermanTerm(documentationProvider.getGermanTermForClass(clazz))

      put("allOf", buildJsonArray {
        addJsonObject {
          putRef(classSchemaObjectReference)
        }
      })

      put("properties", buildJsonObject {
        val typeDiscriminator = determineTypeDiscriminator(clazz).requireNotNull { "No discriminator found for $componentSchema" }
        metadataWriter.putTypeDiscriminator(typeDiscriminator)
      })

      put("required", buildJsonArray {
        add("type")
      })
    })
  }


  /**
   * Puts the enum entry as an object type.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putEnumEntryAsObjectType(enumClass: KClass<out Enum<*>>, enumEntry: Enum<*>, registerCompositeType: (type: SchemaDescriptor) -> Unit) {
    require(enumClass.isEnum()) { "Only enum entries supported" }
    require(enumEntry::class.isSubclassOf(enumClass)) { "Enum entry $enumEntry is not a subclass of $enumClass" }

    putSchemaObject(
      clazz = enumClass, instance = enumEntry,
      forcedPropertyNames = setOf(Enum<*>::name.name),
      requiredPropertiesMode = RequiredPropertiesMode.AllPropertiesWithValue,
      registerCompositeType = registerCompositeType
    )
  }

  /**
   * Generates the ref type for data classes
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildSchemaTypeForDataClass(type: KType, registerCompositeType: (type: SchemaDescriptor) -> Unit) {
    val clazz = type.asKClass()

    require(clazz.isData) { "Not a data class: $clazz" }
    require(clazz.isInterface.not()) { "Interfaces not supported but was $clazz" }

    buildSchemaTypeUsingSerialDescriptor(type, registerCompositeType)
  }

  /**
   * Generates the schema for a non-sealed interface annotated with `@OpenApiBaseSchema`.
   *
   * Uses the property data every implementing data class offered in [buildDataClassWithAllOfComposition];
   * [buildComponentsSchemas] calls it after every implementer has been processed.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  private fun putInterfaceBaseSchemaComponent(
    interfaceSchema: SchemaDescriptor.InterfaceBaseSchema,
    registerCompositeType: (type: SchemaDescriptor) -> Unit,
  ) {
    val clazz = interfaceSchema.asClass()
    val reference = interfaceSchema.reference

    val cached = cachedBaseSchemaProperties[clazz]
      ?: error("No cached base schema properties for ${clazz.simpleName}. Ensure at least one implementing data class is processed before the interface schema.")

    jsonObjectBuilder.put(reference.valueEscaped(), buildJsonObject {
      put("type", "object")
      metadataWriter.putKotlinSourceRef(clazz)
      metadataWriter.putDocumentation(documentationProvider.getDocumentationForType(clazz))
      metadataWriter.putGermanTerm(documentationProvider.getGermanTermForClass(clazz))

      put("properties", JsonObject(cached.properties))
      if (cached.requiredPropertyNames.isNotEmpty()) {
        putJsonArray("required") {
          cached.requiredPropertyNames.fastForEach { add(it) }
        }
      }
    })
  }

  /**
   * Generates a data class schema as `allOf` composition with its `@OpenApiBaseSchema` interfaces.
   *
   * Each base interface is referenced via its own `$ref`. The data class's own properties (those
   * not visible on any base interface) are inlined as a final `allOf` member, together with every base
   * property it declares non-null where the interface declares it nullable — otherwise the composed
   * type would let a client read `null` from a property the class always answers.
   *
   * When the data class has no own properties beyond the base interfaces, the schema is generated
   * as a flat object (no `allOf`) to avoid OrVal generating an empty index-signature type.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  private fun buildDataClassWithAllOfComposition(
    type: KType,
    baseInterfaces: List<SchemaDescriptor.InterfaceBaseSchema>,
    registerCompositeType: (type: SchemaDescriptor) -> Unit,
  ) {
    require(baseInterfaces.isNotEmpty()) { "At least one base interface required" }

    val clazz = type.asKClass()

    baseInterfaces.fastForEach { registerCompositeType(it) }

    val fullProperties = generateFullPropertyMap(type, registerCompositeType)
    val fullRequired = generateFullRequiredSet(type)

    val allBasePropertyNames = baseInterfaces.flatMapTo(mutableSetOf()) { it.propertyNames }
    val tightenedPropertyNames = clazz.tightenedBasePropertyNames(baseInterfaces)
    val ownProperties = fullProperties.filterKeys { it !in allBasePropertyNames }

    // Cache properties per base interface so the interface schema generation can pull its own
    // slice. Each base may overlap with others (a property visible on multiple bases shows up in
    // each cache entry); the schema generator emits each base independently.
    baseInterfaces.fastForEach { baseInterface ->
      val cached = cachedBaseSchemaProperties.getOrPut(baseInterface.asClass()) { CachedBaseSchemaData() }
      fullProperties.forEach { (name, schema) ->
        if (name in baseInterface.propertyNames) {
          cached.offer(name, schema, required = name in fullRequired, tightened = name in tightenedPropertyNames)
        }
      }
    }

    // When the data class has no own properties, generate a flat schema instead of allOf.
    // OrVal generates a problematic index-signature type for empty inline schemas.
    if (ownProperties.isEmpty()) {
      buildSchemaTypeForDataClass(type, registerCompositeType)
      return
    }

    jsonObjectBuilder.put("allOf", buildJsonArray {
      baseInterfaces.fastForEach { baseInterface ->
        addJsonObject {
          put($$"$ref", baseInterface.reference.completeReference())
        }
      }
      addJsonObject {
        put("type", "object")
        metadataWriter.putKotlinSourceRef(clazz)
        metadataWriter.putDocumentation(documentationProvider.getDocumentationForType(clazz))
        metadataWriter.putGermanTerm(documentationProvider.getGermanTermForClass(clazz))

        val inlinedProperties = fullProperties.filterKeys { it !in allBasePropertyNames || it in tightenedPropertyNames }
        val ownRequired = fullRequired.filter { it in inlinedProperties }
        put("properties", JsonObject(inlinedProperties))
        if (ownRequired.isNotEmpty()) {
          putJsonArray("required") {
            ownRequired.fastForEach { add(it) }
          }
        }
      }
    })
  }

  /**
   * Generates the full property map for a type using its serial descriptor.
   */
  private fun generateFullPropertyMap(
    type: KType,
    registerCompositeType: (type: SchemaDescriptor) -> Unit,
  ): Map<String, JsonElement> {
    val serialDescriptor = type.serializer().descriptor
    val clazz = type.asKClass()
    return propertyGenerator.generatePropertiesForSchema(
      context = OpenApiPropertyGenerator.PropertyCreationContext(clazz, type, serialDescriptor),
      typeDiscriminator = null,
      registerCompositeType = registerCompositeType,
    ).let { (it as JsonObject).toMap() }
  }

  /**
   * Generates the full set of required property names for a type.
   */
  private fun generateFullRequiredSet(type: KType): List<String> {
    val serialDescriptor = type.serializer().descriptor
    val clazz = type.asKClass()
    return serialDescriptor.requiredElementNamesForOpenApi(clazz)
  }

  /**
   * Appends the schema type for a sealed hierarchy.
   *
   * This method currently only supports sealed interfaces.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildSchemaTypeForSealedHierarchy(referencedSchemaObject: SchemaDescriptor.ClassSchemaObject, type: KType, registerCompositeType: (type: SchemaDescriptor) -> Unit) {
    val clazz = type.asKClass()

    require(clazz.isSealed) { "Not a sealed class: $clazz" }

    require(clazz.isInterface) {
      "Currently only sealed interfaces are supported but was $clazz"
      //TODO improve
    }

    jsonObjectBuilder.put("type", "object")
    metadataWriter.putKotlinSourceRef(clazz)
    metadataWriter.putDocumentation(documentationProvider.getDocumentationForType(clazz))
    metadataWriter.putGermanTerm(documentationProvider.getGermanTermForClass(clazz))

    val interfaceTypeMapping: Map<KTypeParameter, KType> = clazz.typeParameters
      .zip(type.arguments)
      .mapNotNull { (typeParameter, argument) ->
        argument.type?.let { typeParameter to it }
      }.toMap()

    val sealedSubclasses = clazz.getAllSealedSubclasses()
    require(sealedSubclasses.isNotEmpty()) {
      "<${clazz.qualifiedName}> has no sealed subclass"
    }

    //Resolve all subclass types
    val subTypes = sealedSubclasses.map { subClass ->
      resolveSubclassType(subClass, clazz, interfaceTypeMapping)
    }

    //Register all subclasses that are relevant to be generated later one
    subTypes.fastForEach { subType ->
      registerCompositeType(SchemaDescriptor.ofMessage(subType))
    }

    jsonObjectBuilder.put("oneOf", buildJsonArray {
      sealedSubclasses
        .fastForEach { subClass ->
          val resolvedType = resolveSubclassType(subClass, clazz, interfaceTypeMapping)
          val schema = SchemaDescriptor.ofMessage(resolvedType)

          if (schema.isReferenced()) {
            addJsonObject {
              putRefTo(schema, ReferenceType.Discriminated)
            }
          } else {
            throw UnsupportedOperationException("Subclasses of sealed interfaces must be referenced: $subClass")
          }
        }
    })

    jsonObjectBuilder.put("discriminator", buildJsonObject {
      put("propertyName", "type")
      put("mapping", buildJsonObject {
        subTypes.fastForEach { subType ->
          val subClass = subType.asKClass()

          val referenceSchema = SchemaDescriptor.ofMessage(subType).asComponent()

          val discriminatorValue = subClass.getSerialName()

          validateRestResponseDiscriminator(subClass, discriminatorValue)

          put(discriminatorValue, referenceSchema.reference(ReferenceType.Discriminated).completeReference())
        }
      })
    })
  }

  /**
   * Validates the `@SerialName` discriminator of a [RestResponse] variant.
   *
   * The RestResponse contract is an identity rule between the variant's simple class name and its discriminator:
   * - variants implementing [RestResponse.GoodResponse] declare exactly `@SerialName("good")`
   * - variants implementing [RestResponse.BadResponse] declare exactly `"bad/" + simpleName`,
   *   e.g. `data object NotFound` requires `@SerialName("bad/NotFound")`. Plain `@SerialName("bad")` is forbidden.
   *
   * Frontends rely on this contract to classify responses; a wrong discriminator would only surface
   * at runtime in the browser. Failing here turns the misconfiguration into a build-time error:
   * every `*OpenApiTest` that generates the spec fails with a clear message.
   *
   * Classes that do not implement [RestResponse] are not affected.
   */
  private fun validateRestResponseDiscriminator(subClass: KClass<*>, serialName: String) {
    val isGood = subClass.isSubclassOf(RestResponse.GoodResponse::class)
    val isBad = subClass.isSubclassOf(RestResponse.BadResponse::class)

    check((isGood && isBad).not()) {
      "${subClass.qualifiedName} implements both RestResponse.GoodResponse and RestResponse.BadResponse - " +
        "a variant must be exactly one of the two, otherwise frontends cannot classify the response."
    }

    when {
      isGood -> {
        check(serialName == "good") {
          "Invalid @SerialName(\"$serialName\") on ${subClass.qualifiedName}: " +
            "variants implementing RestResponse.GoodResponse must declare exactly @SerialName(\"good\"). " +
            "Fix the annotation on the Kotlin response variant."
        }
      }

      isBad -> {
        val expected = "bad/${subClass.simpleName}"
        check(serialName == expected) {
          "Invalid @SerialName(\"$serialName\") on ${subClass.qualifiedName}: " +
            "variants implementing RestResponse.BadResponse must declare exactly \"bad/\" + the variant's simple class name - " +
            "here @SerialName(\"$expected\"). Plain \"bad\" is forbidden. " +
            "Either fix the annotation or rename the variant class so both sides match, e.g. data object NotFound with @SerialName(\"bad/NotFound\")."
        }
      }

      else -> {
        // A variant of a RestResponse hierarchy that implements neither marker is unclassifiable
        // for frontends (isGood/isBad both false) - reject it at generation time as well.
        check(subClass.isSubclassOf(RestResponse::class).not()) {
          "${subClass.qualifiedName} implements RestResponse but neither RestResponse.GoodResponse nor RestResponse.BadResponse - " +
            "every variant must implement exactly one of the two markers so frontends can classify the response."
        }
      }
    }
  }

  private fun resolveSubclassType(
    subClass: KClass<*>,
    sealedInterface: KClass<*>,
    interfaceMapping: Map<KTypeParameter, KType>,
  ): KType {
    // Find the exact supertype entry that links the subclass back to the sealed interface. This is
    // the only place where Kotlin still keeps the generic relationship.
    val interfaceSuperType = subClass.supertypes.firstOrNull { it.classifier == sealedInterface }
      ?: return subClass.starProjectedType

    // We build a fresh mapping for the subclass' type parameters. This mapping ultimately allows us
    // to call `subClass.createType(...)` with the concrete arguments we derived.
    val subclassMapping = mutableMapOf<KTypeParameter, KType>()

    /**
     * Recursively walks from the sealed interface declaration down to the subclass declaration.
     * Whenever we encounter a type parameter we assign the already resolved type argument so that the
     * subclass mapping ends up with concrete `KType`s for its own parameters.
     */
    fun assignTypes(superType: KType, expectedType: KType) {
      when (val classifier = superType.classifier) {
        is KTypeParameter -> {
          when (classifier) {
            // The type parameter belongs to the subclass itself. Assign the concrete type so it can
            // be plugged back into `createType()` later.
            in subClass.typeParameters -> subclassMapping.putIfAbsent(classifier, expectedType)
            // Otherwise it belongs to the interface. Recurse with the interface mapping to
            // eventually reach the subclass parameter.
            in interfaceMapping -> {
              interfaceMapping[classifier]?.let { assignTypes(it, expectedType) }
            }
          }
        }

        is KClass<*> -> {
          val expectedClassifier = expectedType.findKClass() ?: return
          // Abort if the structure deviates; mismatched classifiers mean we cannot propagate the type.
          if (classifier != expectedClassifier) {
            return
          }

          // Walk every nested type argument to push the expected type all the way down to the
          // subclass parameter.
          superType.arguments.zip(expectedType.arguments).forEach { (superArg, expectedArg) ->
            val superArgType = superArg.type ?: return@forEach
            val expectedArgType = expectedArg.type ?: return@forEach
            assignTypes(superArgType, expectedArgType)
          }
        }

        else -> Unit
      }
    }

    // Bootstrapping step: pair every sealed-interface parameter with the argument we resolved for the
    // owning type (e.g., AdjustableValue<String>), then propagate that to the subclass declaration.
    sealedInterface.typeParameters
      .zip(interfaceSuperType.arguments)
      .forEach { (interfaceTypeParameter, superTypeArgument) ->
        val expectedType = interfaceMapping[interfaceTypeParameter] ?: return@forEach
        val superTypeArgumentType = superTypeArgument.type ?: return@forEach
        assignTypes(superTypeArgumentType, expectedType)
      }

    // Finally build the argument list for the subclass KType. If we could not derive a concrete type
    // we fall back to the constraint (upper bound) or `Any` to keep the generator conservative.
    val subclassArguments = subClass.typeParameters.map { typeParameter ->
      val mappedType = subclassMapping[typeParameter]
      val argumentType = mappedType ?: typeParameter.upperBounds.firstOrNull()?.makeNotNullable() ?: Any::class.createType()
      KTypeProjection.invariant(argumentType)
    }

    return subClass.createType(subclassArguments, false)
  }

  companion object {
    /** What the schema of a [JsonElement] says, since it constrains nothing. */
    const val AnyJsonValueDescription: String = "Any JSON value"

    /** The base properties this class declares non-null while one of [baseInterfaces] declares them nullable. */
    private fun KClass<*>.tightenedBasePropertyNames(baseInterfaces: List<SchemaDescriptor.InterfaceBaseSchema>): Set<String> {
      val nonNullNames = memberProperties.filterNot { it.returnType.isMarkedNullable }.mapTo(mutableSetOf()) { it.name }
      return baseInterfaces.flatMapTo(mutableSetOf()) { baseInterface ->
        baseInterface.asClass().memberProperties
          .filter { it.returnType.isMarkedNullable && it.name in nonNullNames }
          .map { it.name }
      }
    }

    /**
     * Finds all `@OpenApiBaseSchema`-annotated interfaces in the full ancestry of the given class.
     *
     * Walks the entire supertype closure (`allSupertypes`), not just direct supertypes, so a
     * sealed interface sitting between the data class and the annotated base interface does not
     * hide the base. Returns every annotated ancestor — multiple base schemas compose via `allOf`.
     */
    fun KClass<*>.findOpenApiBaseSchemaInterfaces(): List<SchemaDescriptor.InterfaceBaseSchema> {
      return allSupertypes
        .mapNotNull { it.findKClass() }
        .filter { it.isInterface && it.hasOpenApiBaseSchemaAnnotation() }
        .map { SchemaDescriptor.InterfaceBaseSchema(it.starProjectedType) }
    }
  }
}
