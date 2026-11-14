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
package it.neckar.rest.test.route

import it.neckar.open.kotlin.lang.isInternalOrPublic
import it.neckar.open.kotlin.lang.isObject
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.isAccessible

/**
 * Calculate the "strings" that can be added to the @Effective annotations for an endpoints object
 */
object EffectiveEndpointsCalculator {
  /**
   * Calculates the effective values for all vals of all nested objects of the given [T] class
   */
  inline fun <reified T : Any> calculateEffectiveValues(): EffectiveValues {
    return calculateEffectiveValues(T::class)
  }

  inline fun calculateEffectiveValues(clazz: KClass<*>): EffectiveValues {
    require(clazz.isObject) { "Class ${clazz.qualifiedName} is not an object" }

    return EffectiveValues(buildMap {
      visitAll(clazz) { property, propertyValue, context ->
        val key = EffectiveValues.Key.fromScopes(
          scopes = context.scopes,
          propertyName = property.name,
        )

        put(key, propertyValue.toString())
      }
    })
  }

  /**
   * Visits all vals for all nested objects of the given [T] and calls the given [action] with the property, its value and the context
   */
  inline fun <reified T : Any> visitAll(crossinline action: (property: KProperty<*>, propertyValue: Any?, context: EndpointEntryVisitingContext) -> Unit) {
    return visitAll(T::class, action)
  }

  /**
   * Visits all vals for all nested objects of the given [clazz] and calls the given [action] with the property, its value and the context
   */
  inline fun visitAll(clazz: KClass<*>, crossinline action: (property: KProperty<*>, propertyValue: Any?, context: EndpointEntryVisitingContext) -> Unit) {
    val initialContext = EndpointEntryVisitingContext.fromClass(clazz)

    visitAllVals(clazz) { property, propertyValue ->
      action(property, propertyValue, initialContext)
    }

    visitAllNestedObjects(clazz, initialContext) { nestedObjectClazz, context ->
      visitAllVals(nestedObjectClazz) { property, propertyValue ->
        action(property, propertyValue, context)
      }
    }
  }

  /**
   * Calls the given [function] for all nested objects of the given [clazz]
   */
  fun <T : Any> visitAllNestedObjects(
    clazz: KClass<T>,
    context: EndpointEntryVisitingContext,
    function: (objectType: KClass<*>, context: EndpointEntryVisitingContext) -> Unit,
  ) {
    clazz.java.declaredClasses
      .filter { it.kotlin.isObject }
      .forEach {
        val clazzToVisit = it.kotlin
        val updatedContext = context + clazzToVisit

        function(clazzToVisit, updatedContext)
        visitAllNestedObjects(clazz = clazzToVisit, context = updatedContext, function = function)
      }
  }

  /**
   * Visits all vals for the given object [clazz] and calls the given [action] with the property and its value
   */
  fun visitAllVals(clazz: KClass<*>, action: (property: KProperty<*>, propertyValue: Any?) -> Unit) {
    require(clazz.isObject) { "Class ${clazz.qualifiedName} is not an object" }

    clazz.declaredMembers
      .filterIsInstance<KProperty<*>>()
      .filter { it.isInternalOrPublic() } // only public arguments
      .filter { it.parameters.size == 1 } // only val/var without parameters
      .forEach { member ->
        member.isAccessible = true
        try {
          val result = member.call(clazz.objectInstance)
          action(member, result)
        } catch (e: IllegalArgumentException) {
          throw IllegalArgumentException("Error calling [${member.name}] on ${clazz.qualifiedName}", e)
        }
      }
  }
}
