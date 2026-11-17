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

package it.neckar.kotlin.reflect

import it.neckar.open.unit.si.V
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

/**
 * Returns all superclasses and interfaces of this class.
 */
val <T> Class<T>.superClassesAndInterfaces: List<Class<*>>
  get() {
    return buildList {
      var currentClass: Class<*>? = this@superClassesAndInterfaces
      while (currentClass != null) {
        if (currentClass != this@superClassesAndInterfaces) {
          add(currentClass)
        }
        addAll(currentClass.interfaces)
        currentClass = currentClass.superclass
      }
    }
  }

/**
 * Finds annotations on this property or on the property of the superclasses.
 */
fun <V, T : Annotation> KProperty<V>.findAnnotationsSuper(annotationClass: KClass<T>): List<T> {
  val directlyAnnotated = findAnnotations(annotationClass)
  if (directlyAnnotated.isNotEmpty()) {
    return directlyAnnotated
  }

  //First try to find the annotation on the field
  val field = javaField
  if (field != null) {
    val declaringClass = field.declaringClass
    val superClassesAndInterfaces = declaringClass.superClassesAndInterfaces

    val found = superClassesAndInterfaces
      .mapNotNull { javaClass ->
        javaClass.kotlin.declaredMemberProperties.firstOrNull { it.name == field.name }
      }
      .firstNotNullOfOrNull { it.annotations.find { annotation -> annotation.annotationClass == annotationClass } }

    if (found != null) {
      return listOf(found) as List<T>
    }
  }

  //Find the annotation on the getter
  val getter = this.javaGetter
  if (getter != null) {
    val declaringClass = getter.declaringClass
    val superClassesAndInterfaces = declaringClass.superClassesAndInterfaces

    val found = superClassesAndInterfaces
      .mapNotNull { javaClass ->
        // Compare against the property name, not getter.name ("getFoo"), which never matches a
        // declaredMemberProperties entry.
        javaClass.kotlin.declaredMemberProperties.firstOrNull { it.name == this.name }
      }
      .firstNotNullOfOrNull { it.annotations.find { annotation -> annotation.annotationClass == annotationClass } }

    if (found != null) {
      return listOf(found) as List<T>
    }
  }

  if (field == null && getter == null) {
    throw IllegalStateException("Cannot find field or getter for property $this")
  }

  return emptyList()
}

/**
 * Returns true if two methods have the same signature
 */
fun Method.hasSameSignatureAs(other: Method): Boolean {
  if (this.name != other.name) {
    return false
  }

  if (this.parameterCount != other.parameterCount) {
    return false
  }

  for (i in 0 until this.parameterCount) {
    if (this.parameterTypes[i] != other.parameterTypes[i]) {
      return false
    }
  }

  return true
}
