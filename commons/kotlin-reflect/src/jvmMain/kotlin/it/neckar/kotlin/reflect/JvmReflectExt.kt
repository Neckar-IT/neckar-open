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
package it.neckar.kotlin.reflect

import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField


/**
 * Returns the property with the given name from the class, or throws an exception if it does not exist.
 */
fun <T : Any> KClass<out T>.getProperty(propertyName: String, instance: T): KProperty1<out T, *> {
  val property: KProperty1<out T, *> = this.declaredMemberProperties.firstOrNull { it.name == propertyName }
    ?: throw NoSuchFieldException("Property '$propertyName' not found in $this (declared properties: ${declaredPropertyNames()}${instanceHint(instance)})")
  return property
}

fun <T : Any> KClass<out T>.getMutableProperty(propertyName: String, instance: T): KMutableProperty1<out T, *> {
  val property: KProperty1<out T, *> = getProperty(propertyName, instance)

  require(property is KMutableProperty1<*, *>) { "Property '$propertyName' in $this is a val, not a var" }

  return property as KMutableProperty1<out T, *>
}

/**
 * The names of the properties this class declares itself - the set [getProperty] searches.
 */
private fun KClass<*>.declaredPropertyNames(): List<String> {
  return declaredMemberProperties.map { it.name }.sorted()
}

/**
 * Names the runtime type of [instance] when it differs from the class that was searched.
 *
 * The lookup runs on the receiver class, which may well be a super class of the instance - and only searches the
 * properties that class declares itself. Naming both classes keeps that visible in the failure.
 */
private fun KClass<*>.instanceHint(instance: Any): String {
  if (instance::class == this) {
    return ""
  }
  return ", instance is a ${instance::class}"
}

/**
 * Extension function to get the value of a declared field from an instance of a class using reflection.
 */
fun <T : Any> Class<T>.getDeclaredFieldValue(instance: T, fieldName: String): Any? {
  val field = getDeclaredFieldOrThrow(fieldName)
  field.isAccessible = true
  return field.get(instance)
}

fun <T : Any> Class<T>.setDeclaredFieldValue(instance: T, fieldName: String, newValue: Any?) {
  val field = getDeclaredFieldOrThrow(fieldName)
  field.isAccessible = true
  field.set(instance, newValue)
}

/**
 * Looks up a declared field and fails with the class and its declared field names.
 *
 * [Class.getDeclaredField] itself throws a [NoSuchFieldException] whose message is nothing but the requested field
 * name - it does not even say which class was searched.
 */
private fun <T : Any> Class<T>.getDeclaredFieldOrThrow(fieldName: String): Field {
  try {
    return getDeclaredField(fieldName)
  } catch (e: NoSuchFieldException) {
    val declaredFieldNames = declaredFields.map { it.name }.sorted()
    throw NoSuchFieldException("Field '$fieldName' not found in $this (declared fields: $declaredFieldNames)").apply { initCause(e) }
  }
}

/**
 * Returns the value of a declared property from an instance of a class using reflection.
 */
fun <T : Any> KClass<out T>.getDeclaredPropertyValue(instance: T, propertyName: String): Any? {
  val property: KProperty1<out T, *> = getProperty(propertyName, instance)

  property.isAccessible = true
  return (property as KProperty1<Any, *>).get(receiver = instance)
}

fun <T : Any> KClass<out T>.setDeclaredPropertyValue(instance: T, propertyName: String, newValue: Any?) {
  val property: KProperty1<out T, *> = getProperty(propertyName, instance)
  property.isAccessible = true

  val backingField = property.javaField
    ?: throw NoSuchFieldException("Property '$propertyName' in $this has no backing field - only properties with a backing field can be set by reflection")

  backingField.setAccessible(true)
  backingField.set(instance, newValue)
}

/**
 * Returns true if the type is a simple type (e.g. String, Int, ...)
 */
fun KType.isNativeType(): Boolean {
  return when (classifier) {
    kotlin.String::class -> true
    kotlin.Int::class -> true
    kotlin.Long::class -> true
    kotlin.Double::class -> true
    kotlin.Float::class -> true
    kotlin.Boolean::class -> true
    kotlin.ByteArray::class -> true
    kotlin.Nothing::class -> true
    else -> false
  }
}
