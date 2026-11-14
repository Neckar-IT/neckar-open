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
package it.neckar.rest.openapi.dsl
import it.neckar.open.http.UrlParameterName
import it.neckar.open.kotlin.lang.asKClass
import it.neckar.open.kotlin.lang.isEnum
import it.neckar.reflect.ClassName
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.uuid.Uuid
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 * Represents the documentation for a request
 */
data class OpenApiRequest(
  /**
   * A list of parameters that are applicable for this operation.
   * This list contains all parameters from all locations (see [OpenApiRequestParameter.Location])
   */
  val parameters: List<OpenApiRequestParameter>,

  /**
   * The body of the request
   */
  val body: OpenApiSimpleBody? = null,
) {

  companion object {
    /**
     * Empty request without any parameters or body
     */
    val empty: OpenApiRequest = OpenApiRequest(emptyList())
  }
}


@OpenApiDsl
class OpenApiRequestBuilder(
  pathParameterNames: List<UrlParameterName>,
) {
  /**
   * The parameters of the request.
   */
  val parameters: MutableList<OpenApiRequestParameterBuilder<*>> = mutableListOf<OpenApiRequestParameterBuilder<*>>().also { requestParameterBuilders ->
    pathParameterNames.forEach { name ->
      val parameterBuilder = OpenApiRequestParameterBuilder<String>(name, SchemaDescriptor.String)
      parameterBuilder.location = OpenApiRequestParameter.Location.Path
      requestParameterBuilders.add(parameterBuilder)
    }
  }

  var body: OpenApiSimpleBodyBuilder<*>? = null

  inline fun <reified ExampleType : Any> parameter(name: UrlParameterName, kotlinSourceType: KClass<*>? = null, noinline block: OpenApiRequestParameterBuilder<ExampleType>.() -> Unit = {}) {
    parameter(name, SchemaDescriptor.of<ExampleType>(), kotlinSourceType, block)
  }

  fun <ExampleType : Any> parameter(
    name: UrlParameterName,
    type: SchemaDescriptor,
    kotlinSourceType: KClass<*>? = null,
    block: OpenApiRequestParameterBuilder<ExampleType>.() -> Unit = {},
  ) {
    //Remove old parameters with the same name
    val oldParameter: OpenApiRequestParameterBuilder<*>? = parameters.find { it.name == name }
    parameters.removeIf { it.name == name }

    val parameterBuilder = OpenApiRequestParameterBuilder<ExampleType>(name, type, kotlinSourceType)
    if (oldParameter != null) {
      parameterBuilder.initializeValues(oldParameter)
    }

    parameters.add(parameterBuilder.apply(block))
  }

  fun parameterInt(name: UrlParameterName, kotlinSourceType: KClass<*>? = null, block: OpenApiRequestParameterBuilder<Int>.() -> Unit = {}) {
    parameter(name, SchemaDescriptor.Int, kotlinSourceType, block)
  }

  fun parameterLong(name: UrlParameterName, kotlinSourceType: KClass<*>? = null, block: OpenApiRequestParameterBuilder<Long>.() -> Unit = {}) {
    parameter(name, SchemaDescriptor.Long, kotlinSourceType, block)
  }

  fun parameterUuid(name: UrlParameterName, kotlinSourceType: KClass<*>? = null, block: OpenApiRequestParameterBuilder<Uuid>.() -> Unit = {}) {
    parameter(name, SchemaDescriptor.Uuid, kotlinSourceType, block)
  }

  fun parameterEmail(name: UrlParameterName, kotlinSourceType: KClass<*>? = null, block: OpenApiRequestParameterBuilder<String>.() -> Unit = {}) {
    parameter(name, SchemaDescriptor.Email, kotlinSourceType, block)
  }

  fun parameterDateTime(name: UrlParameterName, kotlinSourceType: KClass<*>? = null, block: OpenApiRequestParameterBuilder<Uuid>.() -> Unit = {}) {
    parameter(name, SchemaDescriptor.DateTime, kotlinSourceType, block)
  }

  fun parameterDate(name: UrlParameterName, kotlinSourceType: KClass<*>? = null, block: OpenApiRequestParameterBuilder<Uuid>.() -> Unit = {}) {
    parameter(name, SchemaDescriptor.Date, kotlinSourceType, block)
  }

  fun parameterDouble(name: UrlParameterName, kotlinSourceType: KClass<*>? = null, block: OpenApiRequestParameterBuilder<Double>.() -> Unit = {}) {
    parameter(name, SchemaDescriptor.Double, kotlinSourceType, block)
  }

  fun parameterString(name: UrlParameterName, kotlinSourceType: KClass<*>? = null, block: OpenApiRequestParameterBuilder<String>.() -> Unit = {}) {
    parameter(name, SchemaDescriptor.String, kotlinSourceType, block)
  }

  /**
   * Creates a string parameter with a list of allowed inline enum values.
   * For Kotlin enum types, use [parameterEnum] instead.
   */
  fun parameterStringWithEnum(name: UrlParameterName, allowedValues: List<String>, kotlinSourceType: KClass<*>? = null, block: OpenApiRequestParameterBuilder<String>.() -> Unit = {}) {
    parameter(name, SchemaDescriptor.InlineEnumSchema(allowedValues), kotlinSourceType, block)
  }

  /**
   * Creates a parameter with enum values extracted from a Kotlin enum class.
   * This creates a schema reference (not inline enum values).
   *
   * Example:
   * ```
   * parameterEnum<RentalStatus>(UrlParameterName("status")) {
   *   description = "Filter by status"
   *   required = false
   * }
   * ```
   */
  inline fun <reified T : Enum<T>> parameterEnum(
    name: UrlParameterName,
    noinline block: OpenApiRequestParameterBuilder<T>.() -> Unit = {},
  ) {
    parameter<T>(name, SchemaDescriptor.EnumSchema(T::class.starProjectedType), T::class, block)
  }

  /**
   * Creates an enum parameter for the given enum type.
   */
  fun <T : Enum<T>> parameterEnum(name: UrlParameterName, enumType: KClass<T>) {
    parameterEnum<T>(name, enumType.starProjectedType, kotlinSourceType = enumType)
  }

  fun <T : Enum<T>> parameterEnum(name: UrlParameterName, enumType: KType, kotlinSourceType: KClass<*>? = null) {
    require(enumType.asKClass().isEnum()) { "Only enums are supported - but was [$enumType]" }
    parameter<T>(name, SchemaDescriptor.EnumSchema(enumType), kotlinSourceType) {}
  }

  fun <T : Enum<T>> parameterEnum(name: UrlParameterName, enumTypeName: ClassName, kotlinSourceType: KClass<*>? = null) {
    parameter<T>(name, SchemaDescriptor.of(enumTypeName), kotlinSourceType)
  }

  fun parameterBoolean(name: UrlParameterName, kotlinSourceType: KClass<*>? = null, block: OpenApiRequestParameterBuilder<Boolean>.() -> Unit = {}) {
    parameter(name, SchemaDescriptor.Boolean, kotlinSourceType, block)
  }

  fun <ExampleType : Any> body(type: SchemaDescriptor, block: OpenApiSimpleBodyBuilder<ExampleType>.() -> Unit) {
    body = OpenApiSimpleBodyBuilder<ExampleType>(type).apply(block)
  }

  fun <ExampleType : Any> body(type: KClass<ExampleType>, block: OpenApiSimpleBodyBuilder<ExampleType>.() -> Unit) {
    body = OpenApiSimpleBodyBuilder<ExampleType>(SchemaDescriptor.of(type)).apply(block)
  }

  inline fun <reified BodyType : Any> body(noinline block: OpenApiSimpleBodyBuilder<BodyType>.() -> Unit): Unit {
    body(SchemaDescriptor.of<BodyType>(), block)
  }

  fun build(): OpenApiRequest = OpenApiRequest(parameters.build(), body?.build())
}
