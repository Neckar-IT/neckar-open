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
package it.neckar.rest.openapi.schema
import it.neckar.contact.EmailAddress
import it.neckar.kotlin.reflect.isNativeType
import it.neckar.open.annotations.ExportAsObject
import it.neckar.open.kotlin.lang.asKClass
import it.neckar.open.kotlin.lang.enumEntries
import it.neckar.open.kotlin.lang.findAnnotationByName
import it.neckar.open.kotlin.lang.getSimpleTypeName
import it.neckar.open.kotlin.lang.getTypeName
import it.neckar.open.kotlin.lang.hasAnyStarProjection
import it.neckar.open.kotlin.lang.isCollection
import it.neckar.open.kotlin.lang.isEnum
import it.neckar.open.kotlin.lang.isInterface
import it.neckar.open.kotlin.lang.isNothing
import it.neckar.open.kotlin.lang.isObject
import it.neckar.open.kotlin.lang.isSealed
import it.neckar.open.kotlin.lang.isValueClass
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.kotlin.lang.simpleNameWithEnclosing
import it.neckar.open.kotlin.lang.valueClassWrappedType
import it.neckar.open.kotlin.reflect.classForName
import it.neckar.open.kotlin.reflect.fqNameToClassForName
import it.neckar.open.kotlin.serializers.SerializedForm
import it.neckar.open.kotlin.serializers.findSerialName
import it.neckar.open.kotlin.serializers.isAnnotatedAsSerializable
import it.neckar.open.kotlin.serializers.isPrimitive
import it.neckar.open.kotlin.serializers.serialName
import it.neckar.open.kotlin.serializers.serializedForm
import it.neckar.open.kotlin.serializers.toPrimitiveType
import it.neckar.reflect.ClassName
import it.neckar.rest.openapi.schema.SchemaDescriptor.ComponentSchema.ReferenceType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializerOrNull
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf
import it.neckar.rest.openapi.dsl.HasExamples
import it.neckar.rest.openapi.dsl.OpenApiExample


/**
 * A schema representation within the OpenAPI specification.
 * This is used to represent the schema of a type in the OpenAPI specification.
 */
sealed interface SchemaDescriptor {
  fun asInline(): InlineSchemaValue = this as InlineSchemaValue
  fun asComponent(): ComponentSchema = this as ComponentSchema
  fun asEnum(): EnumSchema = this as EnumSchema
  fun asDiscriminatedEnum(): ComponentEnumSchemaObject = this as ComponentEnumSchemaObject
  fun asEnumEntry(): EnumEntrySchema = this as EnumEntrySchema
  fun asNothing(): EmptySchema = this as EmptySchema
  fun asPrimitive(): NativePrimitiveSchema = this as NativePrimitiveSchema
  fun asValueClass(): ValueClassSchema = this as ValueClassSchema
  fun asClassAsInlinePrimitive(): SerializedAsPrimitiveSchema = this as SerializedAsPrimitiveSchema
  fun asClassSchemaObject(): ClassSchemaObject {
    return when (this) {
      is ClassSchemaObject -> this
      is DiscriminatedClassSchemaWrapper -> this.baseSchema
      else -> this as ClassSchemaObject
    }
  }
  fun asConstSchemaObject(): ConstSchemaObject = this as ConstSchemaObject
  fun asDiscriminatedClassSchema(): DiscriminatedClassSchemaWrapper = this as DiscriminatedClassSchemaWrapper

  /**
   * Returns the [MessageObjectSchema] for a [ConstantCatalogSchemaObject]; every other schema is returned unchanged.
   */
  fun asMessageIfObject(): SchemaDescriptor {
    if (this is ConstantCatalogSchemaObject) {
      return MessageObjectSchema(type)
    }
    return this
  }

  companion object {
    /**
     * Creates a new instance
     */
    operator fun invoke(type: KType): SchemaDescriptor {
      return of(type)
    }

    operator fun invoke(clazz: KClass<*>): SchemaDescriptor {
      return of(clazz)
    }

    fun of(clazz: KClass<*>): SchemaDescriptor {
      return of(clazz.starProjectedType)
    }

    inline fun <reified T> of(): SchemaDescriptor {
      return of(typeOf<T>())
    }

    fun of(className: ClassName): SchemaDescriptor {
      return of(className.value)
    }

    /**
     * Creates a [SchemaDescriptor] from a class name
     */
    fun of(className: String): SchemaDescriptor {
      require(className.endsWith("?").not()) { "Nullable types ($className) are not supported" }

      try {
        return of(classForName(className))
      } catch (e: ClassNotFoundException) {
        throw SchemaTypeNotFoundException(className, e)
      }
    }

    fun of(className: String, typeParameters: List<String>): SchemaDescriptor {
      require(className.endsWith("?").not()) { "Nullable types ($className) are not supported" }

      val fqNameToClassForName = fqNameToClassForName(className)
      try {
        val clazz = Class.forName(fqNameToClassForName).kotlin

        val parameterTypes = typeParameters.map {
          val typeParameterClazz = classForName(it)
          KTypeProjection.invariant(typeParameterClazz.starProjectedType)
        }

        return of(clazz.createType(parameterTypes))
      } catch (e: ClassNotFoundException) {
        throw SchemaTypeNotFoundException(className, e)
      }
    }

    /**
     * Creates a [SchemaDescriptor] from a [KType].
     *
     * Depending on the type, this will create one of the subclasses of the [SchemaDescriptor] interface.
     */
    fun of(kType: KType): SchemaDescriptor {
      return when {
        kType.isNothing() -> {
          Nothing
        }

        kType.isNativeType() -> {
          NativePrimitiveSchema(kType)
        }

        kType.isValueClass() -> {
          ValueClassSchema(kType)
        }

        kType.asKClass().serializedForm() == SerializedForm.Primitive -> {
          SerializedAsPrimitiveSchema(kType)
        }

        kType.isEnum() -> {
          when {
            kType.asKClass().findAnnotation<ExportAsObject>() != null -> {
              ComponentEnumSchemaObject(kType)
            }

            else -> EnumSchema(kType)
          }
        }

        kType.isObject() -> {
          ConstantCatalogSchemaObject(kType)
        }

        kType.isCollection() -> {
          ClassSchemaObject(kType)
        }

        kType.isInterface() -> {
          when {
            kType.isSealed() -> {
              //The interface itself is *not* a discriminated class - but its implementations are
              ClassSchemaObject(kType)
            }

            kType.asKClass().hasOpenApiBaseSchemaAnnotation() -> {
              InterfaceBaseSchema(kType)
            }

            else -> {
              throw IllegalArgumentException("Only sealed interfaces or @OpenApiBaseSchema interfaces are supported as schema objects - but [$kType] is not sealed and not annotated")
            }
          }
        }

        else -> {
          reference(kType)
        }
      }
    }

    //
    //
    // Creation methods for specific schema types
    //
    //

    /**
     * Creates any kind of referenced schema object.
     */
    fun reference(clazz: KClass<*>): SchemaDescriptor {
      return reference(clazz.starProjectedType)
    }

    fun reference(kType: KType): SchemaDescriptor {
      return when {
        kType.isNothing() -> {
          throw IllegalArgumentException("Cannot create a reference for [$kType]: it is Nothing")
        }

        kType.isNativeType() -> {
          throw IllegalArgumentException("Cannot create a reference for [$kType]: it is a native type")
        }

        kType.isValueClass() -> {
          throw IllegalArgumentException("Cannot create a reference for [$kType]: it is a value class")
        }

        kType.asKClass().serializedForm() == SerializedForm.Primitive -> {
          throw IllegalArgumentException("Cannot create a reference for [$kType]: it is serialized as a primitive")
        }

        kType.isEnum() -> {
          when {
            kType.asKClass().findAnnotationByName<ExportAsObject>() != null -> {
              ComponentEnumSchemaObject(kType)
            }

            else -> EnumSchema(kType)
          }
        }

        kType.isObject() -> {
          ConstantCatalogSchemaObject(kType)
        }

        kType.shouldCreateDiscriminatedClassSchema() -> {
          discriminatedClass(kType)
        }

        else -> {
          ClassSchemaObject(kType)
        }
      }
    }

    /**
     * Creates a [ClassSchemaObject] for a "normal" class that is represented by a reference.
     */
    fun referenceObject(clazz: KClass<*>): ClassSchemaObject {
      return referenceObject(clazz.starProjectedType)
    }

    /**
     * Creates a [ClassSchemaObject] for a "normal" type that is represented by a reference.
     */
    fun referenceObject(type: KType): ClassSchemaObject {
      return ClassSchemaObject(type)
    }

    /**
     * Creates a [ClassSchemaObject] for a "normal" type that is represented by a reference.
     */
    inline fun <reified T : Any> referenceObject(): ClassSchemaObject {
      return referenceObject(T::class)
    }

    /**
     * Creates a [ClassSchemaObject] with schema-level examples using a builder DSL.
     *
     * Usage:
     * ```kotlin
     * val schemaWithExamples = SchemaDescriptor.referenceObject<MyDataClass> {
     *   example("default", MyDataClassExample.Default) {
     *     summary = "Standard-Beispiel"
     *     description = "Ein typisches Beispiel für MyDataClass"
     *   }
     * }
     * ```
     */
    inline fun <reified T : Any> referenceObject(
      block: ClassSchemaObjectBuilder<T>.() -> Unit,
    ): ClassSchemaObject {
      return ClassSchemaObjectBuilder<T>(typeOf<T>()).apply(block).build()
    }

    fun discriminatedClass(
      type: KType,
    ): DiscriminatedClassSchemaWrapper {
      return DiscriminatedClassSchemaWrapper.of(type = type)
    }

    /**
     * Returns true if a discriminated class schema should be created for the provided type.
     */
    private fun KType.shouldCreateDiscriminatedClassSchema(): Boolean {
      val clazz = asKClass()

      if (clazz.isInterface) {
        return false
      }

      if (clazz.isAnnotatedAsSerializable().not()) {
        return false
      }

      val serialName = clazz.findSerialName()
      if (serialName.isNullOrBlank()) {
        return false
      }

      //Sealed classes always have a discriminator
      return clazz.hasSerializableSealedSuperType()
    }

    fun <T : Any> valueClass(klass: KClass<T>): ValueClassSchema {
      return ValueClassSchema(klass.starProjectedType)
    }

    fun <T : Any> const(klass: KClass<T>): ConstantCatalogSchemaObject {
      return ConstantCatalogSchemaObject(klass.starProjectedType)
    }

    /**
     * Creates the schema of a type that is sent as a message: an object becomes a [MessageObjectSchema]
     */
    fun ofMessage(type: KType): SchemaDescriptor {
      return of(type).asMessageIfObject()
    }

    fun <T : Any> message(klass: KClass<T>): MessageObjectSchema {
      return message(klass.starProjectedType)
    }

    fun message(type: KType): MessageObjectSchema {
      return MessageObjectSchema(type)
    }

    /**
     * Creates a new enum entry schema object.
     */
    fun enumEntry(enumEntry: Enum<*>): EnumEntrySchema {
      return EnumEntrySchema.of(enumEntry)
    }

    /**
     * Creates a *simple* enum schema object.
     */
    fun enum(klass: KClass<out Enum<*>>): EnumSchema {
      return EnumSchema.of(klass)
    }

    /**
     * Creates a discriminated enum schema object.
     */
    fun discriminatedEnum(klass: KClass<out Enum<*>>): ComponentEnumSchemaObject {
      return ComponentEnumSchemaObject(klass.starProjectedType)
    }

    inline fun <reified T: Enum<*>> discriminatedEnum(): ComponentEnumSchemaObject {
      return discriminatedEnum(T::class)
    }

    /**
     * Returns the schema entries for the entries for the provided enum.
     */
    @Deprecated("Usually not required, just use `discriminatedEnum` instead, the entries are added automatically")
    fun entries(clazz: KClass<out Enum<*>>): List<EnumEntrySchema> {
      return clazz.enumEntries.map { entry ->
        EnumEntrySchema.of(entry)
      }
    }

    //Primitive types

    val String: NativePrimitiveSchema = NativePrimitiveSchema(kotlin.String::class.starProjectedType)

    val DateTime: FormattedStringSchema = FormattedStringSchema("date-time")
    val Date: FormattedStringSchema = FormattedStringSchema("date")
    val Uuid: FormattedStringSchema = FormattedStringSchema("uuid")
    val Email: FormattedStringSchema = FormattedStringSchema("email", EmailAddress::class)
    val Uri: FormattedStringSchema = FormattedStringSchema("uri")

    val Int: NativePrimitiveSchema = NativePrimitiveSchema(kotlin.Int::class.starProjectedType)
    val Long: NativePrimitiveSchema = NativePrimitiveSchema(kotlin.Long::class.starProjectedType)
    val Float: NativePrimitiveSchema = NativePrimitiveSchema(kotlin.Float::class.starProjectedType)
    val Boolean: NativePrimitiveSchema = NativePrimitiveSchema(kotlin.Boolean::class.starProjectedType)
    val Double: NativePrimitiveSchema = NativePrimitiveSchema(kotlin.Double::class.starProjectedType)
    val ByteArray: NativePrimitiveSchema = NativePrimitiveSchema(kotlin.ByteArray::class.starProjectedType)
    val Nothing: EmptySchema = EmptySchema
  }

  /**
   * Represents a schema type used to represent the absence of a value in the OpenAPI specification.
   */
  data object EmptySchema : SchemaDescriptor

  /**
   * Represents a schema type that can be referenced in the OpenAPI specification.
   *
   * This will be represented with "type: object" and referenced via "$ref" in the OpenAPI specification.
   */
  sealed interface ComponentSchema : SchemaDescriptor {
    /**
     * Returns the reference of the component schema.
     */
    fun reference(
      /**
       * The reference type will be ignored for non-discriminated classes
       */
      referenceType: ReferenceType
    ): ComponentSchemaReference

    enum class ReferenceType {
      /**
       * Does *not* contain the type suffix for discriminated classes
       */
      Plain,

      /**
       * Contains the type suffix for discriminated classes
       */
      Discriminated,
    }
  }

  /**
   * Schemas that are used inline in the OpenAPI specification (no references are used).
   * They are represented by a value directly (e.g., primitive types, value classes, etc.).
   */
  sealed interface InlineSchemaValue : SchemaDescriptor {
    fun toNativeType(): NativeSchemaType
  }

  /**
   * Represents a value class schema in the OpenAPI specification.
   *
   * Only value classes that wrap a primitive type are supported.
   */
  data class ValueClassSchema(val type: KType) : InlineSchemaValue {
    /**
     * The "inner" value type this value class wraps.
     */
    val valueType: KType

    init {
      valueType = type.asKClass().valueClassWrappedType()
      require(valueType.isNativeType()) { "Currently only value classes wrapping a primitive type are supported" }
    }

    fun asClass(): KClass<*> {
      return type.asKClass()
    }

    override fun toNativeType(): NativeSchemaType {
      return valueType.asKClass().toOpenApiNativeType().requireNotNull { "Could not find native type for [$valueType]" }
    }
  }

  /**
   * Represents a class serialized as primitive ("Inline serialization") - but *not* an inline class (which is represented by [ValueClassSchema]).
   */
  @OptIn(InternalSerializationApi::class)
  data class SerializedAsPrimitiveSchema(val type: KType) : InlineSchemaValue {

    val primitiveType: KClass<*>

    init {
      val clazz = type.asKClass()
      require(clazz.isValue.not()) { "Value classes not supported [$type]" }
      val form: SerializedForm = clazz.serializedForm()
      require(form == SerializedForm.Primitive) { "Cannot create a primitive schema for [$type]: its serialized form is $form" }

      primitiveType = clazz.serializerOrNull()?.descriptor?.toPrimitiveType().requireNotNull { "Could not find primitive type for [$type]" }
    }

    fun asClass(): KClass<*> {
      return type.asKClass()
    }

    override fun toNativeType(): NativeSchemaType {
      val clazz = type.asKClass()
      val serializer = clazz.serializerOrNull().requireNotNull { "No serializer found for [$clazz]" }
      require(serializer.descriptor.isPrimitive()) { "Expected primitive serializer type for [$clazz] but was ${serializer.descriptor.kind}" }
      return serializer.descriptor.primitiveToOpenApiType()
    }
  }

  /**
   * The schema of a Kotlin object; [MessageObjectSchema] and [ConstantCatalogSchemaObject] differ in the `required` list.
   */
  sealed interface ConstSchemaObject : ComponentSchema {
    val type: KType

    val reference: ComponentSchemaReference
      get() = ComponentSchemaReference.ofUnescaped(type.getSimpleTypeName())

    /**
     * Returns the type as clazz
     */
    fun asClass(): KClass<*> {
      return type.asKClass()
    }

    override fun reference(referenceType: ReferenceType): ComponentSchemaReference {
      return reference
    }
  }

  /**
   * An object that a route sends or receives, or a variant of a sealed hierarchy.
   * Serialized, it carries at most the type discriminator, so only that is required.
   */
  data class MessageObjectSchema(override val type: KType) : ConstSchemaObject {
    init {
      require(type.isObject()) { "Only object types supported but was [$type]" }
    }
  }

  /**
   * An object that is not sent as a message but publishes constants (e.g. a companion): every property with a value is required.
   */
  data class ConstantCatalogSchemaObject(override val type: KType) : ConstSchemaObject {
    init {
      require(type.isObject()) { "Only object types supported but was [$type]" }
    }
  }

  /**
   * A "normal" schema representation that is used for most types.
   *
   * Is used for Kotlin classes - but not for objects, value classes, primitives, enums, etc.
   */
  data class ClassSchemaObject(
    val type: KType,
    /**
     * Schema-level examples that are attached directly to the schema definition.
     * These examples appear in `components/schemas/{TypeName}/x-examples`.
     */
    override val examples: List<OpenApiExample> = emptyList(),
  ) : ComponentSchema, HasExamples {
    //TODO check for type
    init {
      val clazz = type.asKClass()
      require(clazz.isObject.not()) { "Must not be called for an object type [$clazz]" }
      require(clazz.isValue.not()) { "Must not be called for a value class [$clazz]" }
      require(clazz.serializedForm() != SerializedForm.Primitive) { "Cannot create a reference schema for [$type]: it is serialized as a primitive" }

      //Do not allow star project types
      require(type.hasAnyStarProjection().not()) {
        "Star-projected types are not supported for [$type]"
      }
    }

    val reference: ComponentSchemaReference = ComponentSchemaReference.ofUnescaped(type.simpleNameWithEnclosing)

    override fun reference(referenceType: ReferenceType): ComponentSchemaReference {
      return reference
    }

    fun getTypeName(): String {
      return type.getTypeName()
    }

    fun getSimpleTypeName(): String {
      return type.getSimpleTypeName()
    }

    fun getSimpleArrayElementTypeName(): String {
      require(type.arguments.size == 1) { "Could not determine type of array-elements" }
      return type.arguments.first().let { arg ->
        arg.type?.getSimpleTypeName() ?: arg.toString()
      }
    }

    /**
     * Returns the type as clazz
     */
    fun asClass(): KClass<*> {
      return type.asKClass()
    }

    /**
     * Returns true if this is an enum class
     */
    fun isEnumType(): Boolean {
      return asClass().isEnum()
    }

    /**
     * Returns true if this is a sealed class/interface
     */
    fun isSealed(): Boolean {
      return asClass().isSealed
    }

    override fun toString(): String {
      return "ClassSchemaObject(referenceName=${reference(ReferenceType.Discriminated).unescapedValue})"
    }
  }


  /**
   * Represents a primitive type in the OpenAPI specification.
   * This type is referenced directly in the OpenAPI specification and is not used via a reference.
   */
  data class NativePrimitiveSchema(val type: KType) : InlineSchemaValue {
    init {
      require(type.isNativeType()) { "Only primitive types supported" }
    }

    override fun toNativeType(): NativeSchemaType {
      return type.asKClass().toOpenApiNativeType().requireNotNull { "Could not find native type for [$type]" }
    }
  }

  /**
   * Represents a string type with a specific format in the OpenAPI specification.
   */
  data class FormattedStringSchema(
    val format: String?,
    /** The Kotlin type the string represents, written as `x-source-kotlin-type`. */
    val sourceType: KClass<*> = kotlin.String::class,
  ) : InlineSchemaValue {
    override fun toNativeType(): NativeSchemaType {
      return NativeSchemaType.string(format)
    }
  }

  /**
   * Represents a string type with a list of allowed enum values in the OpenAPI specification.
   * Used for dynamic enum values that are not backed by a Kotlin enum class
   * (e.g., the `scope` parameter whose allowed values are derived from filter keys).
   *
   * For Kotlin enum types, use [EnumSchema] or [ComponentEnumSchemaObject] instead.
   */
  data class InlineEnumSchema(val allowedValues: List<String>) : InlineSchemaValue {
    init {
      require(allowedValues.isNotEmpty()) { "Enum values must not be empty" }
    }

    override fun toNativeType(): NativeSchemaType {
      return NativeSchemaType.string(null)
    }

    companion object {
      /**
       * Creates an [InlineEnumSchema] from a list of typed values,
       * extracting the string representation with [valueExtractor].
       *
       * This avoids unwrapping value classes at the call site.
       */
      operator fun <T> invoke(
        allowedValues: List<T>,
        valueExtractor: (T) -> String,
      ): InlineEnumSchema {
        return InlineEnumSchema(allowedValues.map(valueExtractor))
      }
    }
  }

  /**
   * Represents a class that has a type discriminator and uses another class schema object as base ("allOf").
   *
   * A class schema that includes a discriminator property.
   * It references a normal class schema object.
   */
  data class ClassSchemaWithDiscriminatorObject(
    /**
     * The class schema object that is referenced.
     * Its properties are referenced with "all" in the openapi
     */
    val baseSchema: ClassSchemaObject,

    /**
     * The suffix that is appended to the type name to form the reference name
     */
    val typeSuffix: String = DefaultReferenceSuffix,
  ) : ComponentSchema {

    override fun reference(referenceType: ReferenceType): ComponentSchemaReference {
      return when (referenceType) {
        ReferenceType.Plain -> baseSchema.reference(referenceType)
        ReferenceType.Discriminated -> baseSchema.reference(referenceType) + typeSuffix
      }
    }

    companion object {
      //TODO remove or move!!!
      const val DefaultDiscriminatorPropertyName: String = "type"

      /**
       * The default suffix that is appended to the base schema's reference name to form
       */
      const val DefaultReferenceSuffix: String = ".WithType"
    }
  }

  /**
   * Wrapper for a discriminated class schema.
   * Includes both:
   * * the base schema
   * * the schema with discriminator
   *
   * This schema will not be directly represented in the OpenAPI specification.
   * Instead, both contained schemas will be published.
   *
   * Represents a class schema that explicitly carries discriminator metadata. This is intended
   * for publishing an alternative schema variant when a class hierarchy exposes a discriminator
   * field alongside the plain representation.
   */
  data class DiscriminatedClassSchemaWrapper private constructor(
    /**
     * The base schema that will not contain the "type" information.
     */
    val baseSchema: ClassSchemaObject,

    /**
     * The schema that includes the type information (discriminator).
     */
    val schemaWithDiscriminator: ClassSchemaWithDiscriminatorObject,

    ) : ComponentSchema {
    init {
      //require(baseSchema.type == schemaWithDiscriminator.type) { "The base schema [$baseSchema] and the schema with type [$schemaWithDiscriminator] must represent the same type" }

      require(schemaWithDiscriminator.reference(ReferenceType.Discriminated).completeReference().startsWith(baseSchema.reference(ReferenceType.Plain).completeReference())) {
        "The reference name of the schema with type [${schemaWithDiscriminator.reference(ReferenceType.Discriminated)}] must start with the reference name of the base schema [${baseSchema.reference(ReferenceType.Plain)}]"
      }
      require(baseSchema.asClass().isSealed || baseSchema.asClass().hasSerializableSealedSuperType()) {
        "The base schema's class must be sealed or have a sealed super type - but was [${baseSchema.asClass().simpleName}]"
      }
    }

    override fun reference(referenceType: ReferenceType): ComponentSchemaReference {
      return when (referenceType) {
        ReferenceType.Plain -> baseSchema.reference(referenceType)
        ReferenceType.Discriminated -> schemaWithDiscriminator.reference(referenceType)
      }
    }

    /**
     * Returns the Kotlin class represented by this schema.
     */
    fun asClass(): KClass<*> {
      return baseSchema.asClass()
    }

    companion object {
      fun of(
        type: KType,
      ): DiscriminatedClassSchemaWrapper {
        val baseSchema = ClassSchemaObject(type)
        val schemaWithDiscriminator = ClassSchemaWithDiscriminatorObject(baseSchema) //TODO think about adding the baseSchema completely

        return DiscriminatedClassSchemaWrapper(baseSchema, schemaWithDiscriminator)
      }

      fun of(
        clazz: KClass<*>,
      ): DiscriminatedClassSchemaWrapper {
        return of(clazz.starProjectedType)
      }
    }
  }

  /**
   * Represents a "simple" enum - that is represented as a default "enum" in the OpenAPI specification.
   */
  data class EnumSchema(
    val enumType: KType,
  ) : ComponentSchema {
    init {
      require(enumType.asKClass().isEnum()) { "Only enum types supported but was [$enumType]" }
    }

    /**
     * Returns the wire value of the given enum entry: the constant's `@SerialName` when present,
     * [Enum.name] otherwise. kotlinx.serialization writes exactly this value, so the spec must
     * match it — a spec carrying the Kotlin constant name (`Success`) while the wire carries the
     * `@SerialName` (`success`) makes every generated frontend fail at runtime.
     */
    fun valueName(entry: Enum<*>): String {
      return entry.serialName()
    }

    /**
     * Returns the type as clazz
     */
    fun asClass(): KClass<*> {
      return enumType.asKClass()
    }

    /**
     * Returns the simple type name of the enum entry.
     * This is used to identify the schema type in the OpenAPI specification when using references
     */
    fun getSimpleTypeName(): String {
      return enumType.getSimpleTypeName()
    }

    override fun reference(referenceType: ReferenceType): ComponentSchemaReference {
      return ComponentSchemaReference.ofUnescaped(referenceNameUnescaped())
    }

    fun referenceNameUnescaped(): String {
      return getSimpleTypeName()
    }

    companion object {
      fun of(clazz: KClass<out Enum<*>>): EnumSchema {
        return EnumSchema(clazz.starProjectedType)
      }
    }
  }

  /**
   * Represents a discriminated enum schema in the OpenAPI specification.
   */
  data class ComponentEnumSchemaObject(val enumType: KType) : ComponentSchema {
    init {
      require(enumType.asKClass().isEnum()) { "Only enum classes supported but was [$enumType]" }
    }

    /**
     * Returns the type as clazz
     */
    fun asClass(): KClass<*> {
      return enumType.asKClass()
    }

    val reference: ComponentSchemaReference = ComponentSchemaReference.ofUnescaped(enumType.getSimpleTypeName())

    override fun reference(referenceType: ReferenceType): ComponentSchemaReference {
      return reference
    }

    companion object {
      fun of(enumType: KClass<out Enum<*>>): ComponentEnumSchemaObject {
        return ComponentEnumSchemaObject(enumType.starProjectedType)
      }
    }
  }

  /**
   * Represents a *single* enum entry as an object in the OpenAPI specification.
   */
  data class EnumEntrySchema internal constructor(val enumType: KType, val enumEntry: Enum<*>) : ComponentSchema {
    init {
      require(enumType.asKClass().isEnum()) { "Only enum types supported but was [$enumType]" }
    }

    /**
     * Returns the type as clazz
     */
    fun asClass(): KClass<out Enum<*>> {
      return enumType.asKClass() as KClass<out Enum<*>>
    }

    /**
     * Returns the simple type name of the enum entry.
     * This is used to identify the schema type in the OpenAPI specification when using references
     */
    fun getSimpleTypeName(): String {
      return "${enumType.getSimpleTypeName()}.${enumEntry.name}"
    }

    override fun reference(referenceType: ReferenceType): ComponentSchemaReference {
      return ComponentSchemaReference.ofUnescaped(getSimpleTypeName())
    }

    companion object {
      fun of(entry: Enum<*>): EnumEntrySchema {
        return EnumEntrySchema(entry::class.starProjectedType, entry)
      }
    }
  }

  /**
   * Represents a non-sealed interface annotated with `@OpenApiBaseSchema`.
   *
   * The interface's declared properties are published as a shared base schema in `components/schemas`.
   * Data classes implementing this interface are generated with `allOf` composition, referencing
   * the base schema for shared properties and inlining their own additional properties.
   */
  data class InterfaceBaseSchema(val type: KType) : ComponentSchema {
    init {
      require(type.asKClass().isInterface) { "Only interfaces supported but was [$type]" }
      require(type.asKClass().hasOpenApiBaseSchemaAnnotation()) { "Interface must be annotated with @OpenApiBaseSchema: [$type]" }
    }

    /**
     * All property names visible on this interface, including those inherited from supertypes
     * (e.g., cross-cutting mixins like `HasArchiveState`, `HasConfirmationStatus`).
     */
    val propertyNames: Set<String> = type.asKClass().memberProperties.map { it.name }.toSet()

    val reference: ComponentSchemaReference = ComponentSchemaReference.ofUnescaped(type.simpleNameWithEnclosing)

    override fun reference(referenceType: ReferenceType): ComponentSchemaReference {
      return reference
    }

    fun asClass(): KClass<*> {
      return type.asKClass()
    }
  }
}

/**
 * Returns true if the class has a sealed super type that is annotated as @Serializable
 */
fun KClass<*>.hasSerializableSealedSuperType(): Boolean {
  return supertypes
    .any { superType ->
      superType.isInterface() && superType.isSealed() && superType.asKClass().isAnnotatedAsSerializable()
    }
}

/**
 * Returns true if the class is annotated with `@OpenApiBaseSchema`.
 */
fun KClass<*>.hasOpenApiBaseSchemaAnnotation(): Boolean {
  return findAnnotationByName("it.neckar.open.annotations.OpenApiBaseSchema") != null
}

