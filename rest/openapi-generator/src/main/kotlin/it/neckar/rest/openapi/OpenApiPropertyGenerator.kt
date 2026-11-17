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

import it.neckar.open.annotations.ForcePolymorphic
import it.neckar.open.collections.fastForEach
import it.neckar.open.kotlin.lang.asKClass
import it.neckar.open.kotlin.lang.getAllSealedSubclasses
import it.neckar.open.kotlin.lang.isValueClass
import it.neckar.open.kotlin.lang.substringBetween
import it.neckar.open.kotlin.serializers.SerializedForm
import it.neckar.open.kotlin.serializers.isPrimitive
import it.neckar.open.kotlin.serializers.serializedForm
import it.neckar.rest.openapi.schema.SchemaDescriptor.ComponentSchema.ReferenceType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.SchemaPropertyConstraints
import it.neckar.rest.openapi.schema.TypeDiscriminator
import it.neckar.rest.openapi.schema.hasOpenApiNullableAnnotation
import it.neckar.rest.openapi.documentation.kotlinPropertyInfo
import it.neckar.rest.openapi.schema.makeNotNullable
import it.neckar.rest.openapi.schema.primitiveToOpenApiType
import it.neckar.rest.openapi.schema.substituteTypeParameters

/**
 * Helper class for generating OpenAPI property definitions.
 *
 * Handles property generation for various types:
 * - Primitive types
 * - Enums
 * - Classes
 * - Lists/Arrays
 * - Maps
 * - Sealed classes
 * - Value classes
 */
internal class OpenApiPropertyGenerator(
  /**
   * The documentation provider for resolving property descriptions
   */
  private val documentationProvider: DocumentationProvider,

  /**
   * The metadata writer for adding Kotlin source refs, documentation, etc.
   */
  private val metadataWriter: OpenApiMetadataWriter,
) {
  /**
   * The schema generator. Property generation and schema generation are mutually recursive
   * (a property may reference a schema, a schema is built from its properties), so the reference
   * is wired in after construction to break the initialization cycle.
   */
  internal lateinit var schemaGenerator: OpenApiSchemaGenerator

  /**
   * The serial names of the kotlinx-serialization `Set` serializers. A property of these types is
   * emitted with `uniqueItems: true`.
   */
  private val setSerialNames: Set<String> = setOf("kotlin.collections.LinkedHashSet", "kotlin.collections.HashSet")

  /**
   * Builds all properties for a schema (class/object).
   *
   * Creates a JSON object with all properties of the given type, including type discriminators
   * for sealed hierarchies.
   */
  internal fun generatePropertiesForSchema(
    /**
     * The context that contains information about the property
     */
    context: PropertyCreationContext,

    /**
     * Type discriminator (usually for subclasses of sealed interfaces).
     * Usually derived from the @SerialName annotation
     */
    typeDiscriminator: TypeDiscriminator?,

    /**
     * Is called when a new type has been detected
     */
    registerCompositeType: (type: SchemaDescriptor) -> Unit,
  ): JsonObject {
    return buildJsonObject {
      if (typeDiscriminator != null) {
        //Add the (hard coded) type, derived from the @SerialName annotation
        metadataWriter.putTypeDiscriminator(typeDiscriminator)
      }

      context.ownerSerialDescriptor.elementDescriptors.forEachIndexed { index, propertyDescriptor ->
        val name = context.ownerSerialDescriptor.getElementName(index)

        try {
          val propertyType = context.resolvePropertyType(name)

          //If the ForcePolymorphic annotation is used, use the concrete type instead of the detected type
          val forcedType = context.ownerSerialDescriptor.getElementAnnotations(index)
            .filterIsInstance<ForcePolymorphic>()
            .firstOrNull()
            ?.concreteType

          //Get the property info from the serial descriptor, using KClass to detect computed properties
          val kotlinPropertyInfo = context.ownerSerialDescriptor.kotlinPropertyInfo(index, context.ownerClazz)

          // Types annotated with @OpenApiNullable always show null as a valid value in the schema
          // (e.g., Patchable uses null for Delete semantics)
          val forceNullableInSchema = propertyType.hasOpenApiNullableAnnotation()

          val schemaConstraints = kotlinPropertyInfo.toSchemaConstraints(forceNullableInSchema)

          put(
            key = name,
            element = generatePropertyStatementForSchema(
              context = context.withPropertyName(name),
              propertyType = propertyType,
              propertyDescriptor = propertyDescriptor,
              forcedType = forcedType,
              schemaConstraints = schemaConstraints,
              registerCompositeType = registerCompositeType,
            ),
          )
        } catch (e: Exception) {
          throw IllegalStateException("Could not generate property statement for property [$name] at index $index of ${context.ownerClazz} (${context.ownerSerialDescriptor})", e)
        }
      }
    }
  }

  /**
   * Generates a property statement for the provided element descriptor
   */
  internal fun generatePropertyStatementForSchema(
    /**
     * The context that contains information about the property creation process
     */
    context: NamedPropertyCreationContext,

    /**
     * The reflective property type - always available via reflection
     */
    propertyType: KType,

    /**
     * The serial descriptor for this property
     */
    propertyDescriptor: SerialDescriptor,

    /**
     * The forced type for this property. If set, this type is used instead of the detected type.
     */
    forcedType: KClass<*>?,

    /**
     * The constraints for the OpenAPI schema (required, showNullInType)
     */
    schemaConstraints: SchemaPropertyConstraints,

    /**
     * Callback to schedule a composite type - if detected
     */
    registerCompositeType: (type: SchemaDescriptor) -> Unit,
  ): JsonElement {

    /**
     * Handle inlined classes first, call with the inlined element descriptor
     */
    if (propertyDescriptor.isInline) {
      //Inline class detected, add the type directly

      require(propertyDescriptor.elementsCount == 1) {
        "Invalid inline class type: $propertyDescriptor"
      }

      val inlinedElementDescriptor = propertyDescriptor.getElementDescriptor(0)

      //Inline inlined classes (usually value classes)
      return generatePropertyStatementForSchema(
        context = context,
        propertyType = propertyType,
        propertyDescriptor = inlinedElementDescriptor,
        forcedType = forcedType,
        schemaConstraints = schemaConstraints,
        registerCompositeType = registerCompositeType,
      )
    }

    /**
     * Not inlined, handle the other cases here
     */

    return buildJsonObject {
      metadataWriter.putDocumentation(documentationProvider.getDocumentationForProperty(context.ownerClazz, context.propertyName))

      //Add unit annotation if present (e.g., @Months -> x-unit: "mo")
      metadataWriter.putUnit(documentationProvider.findUnitForProperty(context.ownerClazz, context.propertyName))

      //Add value class metadata if the property type is a value class (x-source-kotlin-type, x-unit-symbol, x-unit-name, x-german-term)
      if (propertyType.isValueClass()) {
        metadataWriter.putKotlinSourceRef(propertyType.asKClass())
        metadataWriter.putValueClassMetadata(propertyType.asKClass())
      } else {
        //Add German term for non-value-class properties (value classes already include it via putValueClassMetadata)
        metadataWriter.putGermanTerm(documentationProvider.getGermanTermForProperty(context.ownerClazz, context.propertyName))
      }

      //@ReadOnly / @WriteOnly - server-set vs client-sent properties
      if (documentationProvider.isReadOnlyProperty(context.ownerClazz, context.propertyName)) {
        put("readOnly", true)
      }
      if (documentationProvider.isWriteOnlyProperty(context.ownerClazz, context.propertyName)) {
        put("writeOnly", true)
      }
      //A @Deprecated property is marked deprecated in the schema, with its message as x-deprecated-reason
      if (documentationProvider.isDeprecatedProperty(context.ownerClazz, context.propertyName)) {
        put("deprecated", true)
        documentationProvider.getDeprecatedReasonForProperty(context.ownerClazz, context.propertyName)?.let {
          put(AdditionalOpenApiKeys.DeprecatedReason, it)
        }
      }

      when {
        propertyDescriptor.kind.isPrimitive -> {
          buildPrimitivePropertyElement(propertyDescriptor, schemaConstraints)
        }

        //PolymorphicKind.OPEN -> TODO()
        propertyDescriptor.kind == PolymorphicKind.SEALED -> {
          buildSealedClassPropertyElement(propertyDescriptor, forcedType, propertyType, schemaConstraints, registerCompositeType)
        }

        propertyDescriptor.kind == SerialKind.ENUM -> {
          buildEnumPropertyElement(propertyDescriptor, schemaConstraints, propertyType, registerCompositeType)
        }

        propertyDescriptor.kind == StructureKind.CLASS && propertyType.makeNotNullable().asKClass().serializedForm() == SerializedForm.Primitive -> {
          val schema = SchemaDescriptor.of(propertyType.makeNotNullable()) as SchemaDescriptor.InlineSchemaValue
          metadataWriter.putNativeType(schema.toNativeType(), schemaConstraints)
          metadataWriter.putKotlinSourceRef(schema)
        }

        propertyDescriptor.kind == StructureKind.CLASS -> {
          buildClassPropertyElement(propertyDescriptor, propertyType, schemaConstraints, registerCompositeType)
        }

        propertyDescriptor.kind == StructureKind.LIST -> {
          buildListPropertyElement(propertyDescriptor, forcedType, propertyType, registerCompositeType)
        }

        propertyDescriptor.kind == StructureKind.MAP -> {
          buildMapPropertyElement(propertyDescriptor, forcedType, propertyType, schemaConstraints, registerCompositeType)
        }

        propertyDescriptor.kind == StructureKind.OBJECT -> {
          TODO("OBJECT is not supported yet: $propertyDescriptor")
        }

        propertyDescriptor.kind == SerialKind.CONTEXTUAL -> {
          //usually from a sealed class!?
          buildContextualPropertyElement(context, propertyDescriptor, registerCompositeType)
        }

        else -> {
          throw UnsupportedOperationException("Unknown kind ${propertyDescriptor.kind} for ${propertyDescriptor.serialName}")
        }
      }
    }
  }

  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildContextualPropertyElement(context: NamedPropertyCreationContext, elementDescriptor: SerialDescriptor, registerCompositeType: (type: SchemaDescriptor) -> Unit) {
    require(elementDescriptor.kind == SerialKind.CONTEXTUAL) {
      "Invalid kind: ${elementDescriptor.kind}"
    }

    val serialName = elementDescriptor.serialName
    require(serialName.startsWith("kotlinx.serialization.Sealed<")) {
      "Invalid serial name: $serialName"
    }

    val sealedClassName = serialName.substringBetween("kotlinx.serialization.Sealed<", ">")

    try {
      val sealedClass = Class.forName(sealedClassName).kotlin
      require(sealedClass.isSealed) { "Not a sealed class: $sealedClass" }

      jsonObjectBuilder.put("type", "object")
      jsonObjectBuilder.put("oneOf", buildJsonArray {
        sealedClass.getAllSealedSubclasses()
          .fastForEach { subClass ->
            add(
              generatePropertyStatementForSchema(
                context = context,
                propertyType = subClass.starProjectedType,
                propertyDescriptor = subClass.serializer().descriptor,
                forcedType = null,
                schemaConstraints = SchemaPropertyConstraints.Required,
                registerCompositeType = registerCompositeType,
              )
            )
          }
      })
    } catch (e: ClassNotFoundException) {
      throw IllegalStateException("Could not find sealed class: $sealedClassName", e)
    }
  }

  /**
   * Builds the schema for a single collection element - the `items` of a list/set or the
   * `additionalProperties` value of a map. Dispatches on the element's serial kind; the reflective
   * [elementType] is required for the non-primitive cases (it is `null` for custom serializers).
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  private fun buildCollectionElementSchema(
    elementDescriptor: SerialDescriptor,
    elementType: KType?,
    forcedType: KClass<*>?,
    schemaConstraints: SchemaPropertyConstraints,
    registerCompositeType: (type: SchemaDescriptor) -> Unit,
  ) {
    when {
      elementDescriptor.isInline -> {
        // Value class element (e.g., Set<GitlabLabel> where GitlabLabel wraps String).
        // Unwrap the value class and generate the schema for the inner type.
        require(elementDescriptor.elementsCount == 1) {
          "Invalid inline class type: $elementDescriptor"
        }
        val inlinedElementDescriptor = elementDescriptor.getElementDescriptor(0)
        require(inlinedElementDescriptor.kind.isPrimitive) {
          "Only value classes wrapping a primitive type are supported as collection elements: $elementDescriptor"
        }

        //The element names its value class the way a property does, so a reader finds the Kotlin type at either place
        if (elementType != null && elementType.isValueClass()) {
          metadataWriter.putKotlinSourceRef(elementType.asKClass())
          metadataWriter.putValueClassMetadata(elementType.asKClass())
        }

        buildPrimitivePropertyElement(inlinedElementDescriptor, schemaConstraints)
      }

      elementDescriptor.kind == StructureKind.LIST -> {
        val nestedType = elementType ?: error("Nested list type [$elementDescriptor] must have a type argument")
        buildListPropertyElement(elementDescriptor, forcedType, nestedType, registerCompositeType)
      }

      elementDescriptor.kind == StructureKind.MAP -> {
        val nestedType = elementType ?: error("Nested map type [$elementDescriptor] must have a type argument")
        buildMapPropertyElement(elementDescriptor, forcedType, nestedType, schemaConstraints, registerCompositeType)
      }

      elementDescriptor.kind.isPrimitive -> {
        // Primitives don't need the reflective type
        buildPrimitivePropertyElement(elementDescriptor, schemaConstraints)
      }

      elementDescriptor.kind == PolymorphicKind.SEALED -> {
        val nestedType = elementType ?: error("Sealed class collection type [$elementDescriptor] must have a type argument")
        val detectedType = SchemaDescriptor.of(nestedType)
        buildSealedClassPropertyElement(elementDescriptor, forcedType, nestedType, schemaConstraints, registerCompositeType)
        registerCompositeType(detectedType)
      }

      elementDescriptor.kind == StructureKind.OBJECT ||
        elementDescriptor.kind == StructureKind.CLASS ||
        elementDescriptor.kind == SerialKind.ENUM -> {
        val nestedType = elementType ?: error("Class/Enum collection type [$elementDescriptor] must have a type argument")
        val detectedType = SchemaDescriptor.of(nestedType) as SchemaDescriptor.ComponentSchema

        registerCompositeType(detectedType)
        schemaGenerator.putRef(detectedType, schemaConstraints, ReferenceType.Plain)
      }

      else -> {
        throw UnsupportedOperationException("Unknown kind ${elementDescriptor.kind}")
      }
    }
  }

  /**
   * Generates the property statement for a map. Emits `type: object` (`["object", "null"]` when
   * nullable) plus a typed `additionalProperties` describing the map's value type.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildMapPropertyElement(
    mapDescriptor: SerialDescriptor,
    forcedType: KClass<*>?,
    propertyType: KType,
    schemaConstraints: SchemaPropertyConstraints,
    registerCompositeType: (type: SchemaDescriptor) -> Unit,
  ) {
    require(mapDescriptor.kind == StructureKind.MAP) {
      "Invalid kind: ${mapDescriptor.kind}"
    }

    if (schemaConstraints.showNullInType) {
      jsonObjectBuilder.put("type", JsonArray(listOf(JsonPrimitive("object"), JsonPrimitive("null"))))
    } else {
      jsonObjectBuilder.put("type", "object")
    }

    // A Map<K, V> serializes with two element descriptors: the key (index 0) and the value (index 1).
    // additionalProperties describes the value type V.
    val valueDescriptor = mapDescriptor.getElementDescriptor(1)
    val valueType: KType? = propertyType.arguments.getOrNull(1)?.type?.makeNotNullable()

    if (valueType == null || valueType.classifier == Any::class) {
      // Map<String, Any?> or a custom serializer without a reflective value type: any value is allowed.
      jsonObjectBuilder.put("additionalProperties", true)
      return
    }

    @Suppress("DEPRECATION")
    val valueConstraints = mapDescriptor.kotlinPropertyInfo(1).toSchemaConstraints()

    jsonObjectBuilder.put("additionalProperties", buildJsonObject {
      buildCollectionElementSchema(valueDescriptor, valueType, forcedType, valueConstraints, registerCompositeType)
    })
  }

  /**
   * Generates the property statement for a list
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildListPropertyElement(
    elementDescriptor: SerialDescriptor,
    forcedType: KClass<*>?,
    propertyType: KType,
    registerCompositeType: (type: SchemaDescriptor) -> Unit
  ) {
    require(elementDescriptor.kind == StructureKind.LIST) {
      "Invalid kind: ${elementDescriptor.kind}"
    }

    jsonObjectBuilder.put("type", "array")
    jsonObjectBuilder.put("items", buildJsonObject {
      require(elementDescriptor.elementsCount == 1) {
        "Invalid list type: $elementDescriptor"
      }

      val itemDescriptor = elementDescriptor.getElementDescriptor(0)
      // For generic list types (List<T>, Set<T>), extract the element type from reflection.
      // For classes with custom serializers that serialize as lists (e.g., Tags), this will be null.
      val itemType: KType? = propertyType.arguments.firstOrNull()?.type?.makeNotNullable()
      // Deprecated method without KClass: no KClass is available for the element type here, and computed
      // property detection does not apply (list elements are values, not class properties).
      @Suppress("DEPRECATION")
      val itemConstraints = elementDescriptor.kotlinPropertyInfo(0).toSchemaConstraints()

      buildCollectionElementSchema(itemDescriptor, itemType, forcedType, itemConstraints, registerCompositeType)
    })

    // A Set serializes as a LIST but with a set-specific serializer; that serializer identity - not a
    // substring of the serial name - is what marks the items as unique. Matching the exact serial names
    // avoids false positives from user types that merely contain "Set" in their name.
    if (elementDescriptor.serialName in setSerialNames) {
      jsonObjectBuilder.put("uniqueItems", true)
    }
  }

  /**
   * Creates the property element for an enum.
   *
   * Adds the reference to the enum schema.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildEnumPropertyElement(
    elementDescriptor: SerialDescriptor,
    /**
     * The schema constraints (required, showNullInType) for this property
     */
    schemaConstraints: SchemaPropertyConstraints,
    /**
     * The reflective property type - always available via reflection
     */
    propertyType: KType,
    registerCompositeType: (type: SchemaDescriptor) -> Unit
  ) {
    require(elementDescriptor.kind == SerialKind.ENUM) {
      "Invalid kind: ${elementDescriptor.kind}"
    }

    val schema = SchemaDescriptor.of(propertyType) as SchemaDescriptor.ComponentSchema
    registerCompositeType(schema)
    schemaGenerator.putRef(schema, schemaConstraints, ReferenceType.Plain)
  }

  /**
   * Builds the property element for a [StructureKind.CLASS] [elementDescriptor].
   *
   * Adds a reference to the class schema.
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildClassPropertyElement(
    elementDescriptor: SerialDescriptor,
    /**
     * The reflective property type - always available via reflection
     */
    propertyType: KType,
    /**
     * The schema constraints (required, showNullInType) for this property
     */
    schemaConstraints: SchemaPropertyConstraints,
    registerCompositeType: (type: SchemaDescriptor) -> Unit
  ) {
    require(elementDescriptor.isInline.not()) {
      "Inline classes not supported: $elementDescriptor"
    }

    val schema = SchemaDescriptor.of(propertyType) as SchemaDescriptor.ComponentSchema
    registerCompositeType(schema)
    schemaGenerator.putRef(schema, schemaConstraints, ReferenceType.Plain)
  }


  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun buildPrimitivePropertyElement(elementDescriptor: SerialDescriptor, schemaConstraints: SchemaPropertyConstraints) {
    val openApiType = elementDescriptor.primitiveToOpenApiType()
    metadataWriter.putNativeType(openApiType, schemaConstraints)
  }

  /**
   * Puts an enum type to this JSON object builder
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  internal fun putEnumType(enumEntries: Iterable<String>, descriptions: Map<String, String> = emptyMap(), germanTerms: Map<String, String> = emptyMap()) {
    jsonObjectBuilder.put("type", "string")
    jsonObjectBuilder.put("enum", buildJsonArray {
      enumEntries.forEach {
        add(it)
      }
    })
    if (descriptions.isNotEmpty()) {
      jsonObjectBuilder.put(AdditionalOpenApiKeys.EnumDescriptions, buildJsonObject {
        descriptions.forEach { (value, description) -> put(value, description) }
      })
    }
    if (germanTerms.isNotEmpty()) {
      jsonObjectBuilder.put(AdditionalOpenApiKeys.EnumGermanTerms, buildJsonObject {
        germanTerms.forEach { (value, germanTerm) -> put(value, germanTerm) }
      })
    }
  }

  /**
   * Builds the property element for a [PolymorphicKind.SEALED] [elementDescriptor]
   */
  context(jsonObjectBuilder: JsonObjectBuilder)
  fun buildSealedClassPropertyElement(
    elementDescriptor: SerialDescriptor,
    /**
     * The forced type for this property. If set, this type is used instead of the detected type.
     */
    forcedType: KClass<*>?,
    /**
     * The reflective property type - always available via reflection
     */
    propertyType: KType,
    /**
     * The schema constraints (required, showNullInType) for this property
     */
    schemaConstraints: SchemaPropertyConstraints,
    registerCompositeType: (type: SchemaDescriptor) -> Unit
  ) {
    require(elementDescriptor.kind == PolymorphicKind.SEALED) {
      "Invalid kind: ${elementDescriptor.kind}"
    }

    // Use forced type if specified (from @ForcePolymorphic annotation), otherwise use the reflective property type
    val schemaType: KType = forcedType?.starProjectedType ?: propertyType
    val schema: SchemaDescriptor.ComponentSchema = SchemaDescriptor.of(schemaType) as SchemaDescriptor.ComponentSchema
    registerCompositeType(schema)
    schemaGenerator.putRef(schema, schemaConstraints, ReferenceType.Discriminated)
  }

  /**
   * Context object when creating a property.
   *
   * The generator needs access to the *concrete* `KType` of the owner to expand generic
   * type arguments. Without this the Kotlin reflection APIs would deliver star-projected types,
   * which in turn would trip the "star-projected types are not supported" guards. Capturing the
   * `ownerType` up front lets us build a deterministic mapping from type parameters to their
   * runtime arguments and reuse it for every property.
   */
  class PropertyCreationContext(
    /**
     * The owner class of the property
     */
    val ownerClazz: KClass<*>,
    /**
     * The reflective owner type including its concrete type arguments
     */
    val ownerType: KType,
    /**
     * The serialization descriptor of the owner class
     */
    val ownerSerialDescriptor: SerialDescriptor,
  ) {

    private val typeParameterMapping: Map<KTypeParameter, KType> = ownerClazz.typeParameters
      .zip(ownerType.arguments)
      .mapNotNull { (typeParameter, argument) ->
        argument.type?.let { typeParameter to it }
      }.toMap()


    /**
     * Resolves the concrete property type for the provided property name.
     *
     * This method substitutes the captured generic arguments, ensuring that downstream schema
     * builders (value classes, sealed hierarchies, etc.) see the same `KType` a user would expect
     * from `typeOf<Foo<Bar>>()`.
     *
     * @throws IllegalStateException if the property cannot be found (should never happen for valid serial descriptors)
     */
    fun resolvePropertyType(propertyName: String): KType {
      val property = ownerClazz.memberProperties.firstOrNull { it.name == propertyName }
        ?: error("Property [$propertyName] not found in class [$ownerClazz]. This indicates a mismatch between the serial descriptor and the Kotlin class.")
      return property.returnType
        .makeNotNullable()
        .substituteTypeParameters(typeParameterMapping)
    }

    fun withPropertyName(propertyName: String): NamedPropertyCreationContext {
      return NamedPropertyCreationContext(
        ownerClazz = ownerClazz,
        ownerSerialDescriptor = ownerSerialDescriptor,
        ownerType = ownerType,
        propertyName = propertyName,
      )
    }
  }

  /**
   * Context object when creating a property that also contains the property name
   */
  class NamedPropertyCreationContext(
    /**
     * The owner class of the property
     */
    val ownerClazz: KClass<*>,
    /**
     * The serialization descriptor of the owner class
     */
    val ownerSerialDescriptor: SerialDescriptor,
    /**
     * The reflective owner type including its concrete type arguments
     */
    val ownerType: KType,

    val propertyName: String,
  )
}
