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
import it.neckar.open.annotations.OpenApiNullable
import it.neckar.open.kotlin.lang.asKClass
import it.neckar.open.kotlin.lang.findKClass
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.withNullability
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType

/**
 * Reflection helpers used across the OpenAPI schema generators to manipulate [KType]s.
 */

fun Collection<KCallable<*>>.filterIsProperty(): List<KProperty<*>> {
  return filterIsInstance<KProperty<*>>()
}

internal fun KType.makeNotNullable(): KType {
  return if (isMarkedNullable) {
    withNullability(false)
  } else {
    this
  }
}

/**
 * Applies the provided [mapping] to this type, replacing every type parameter with the concrete
 * argument captured earlier. This is effectively a tiny, reflection-friendly version of what the
 * Kotlin compiler does during type inference, which we need so all schema builders operate on the
 * same `KType` the user originally requested.
 */
internal fun KType.substituteTypeParameters(mapping: Map<KTypeParameter, KType>): KType {
  return when (val classifier = classifier) {
    is KTypeParameter -> {
      val mappedType = mapping[classifier] ?: return this
      if (isMarkedNullable) {
        mappedType.withNullability(true)
      } else {
        mappedType
      }
    }

    is KClass<*> -> {
      if (arguments.isEmpty()) {
        return this
      }

      var changed = false
      val newArguments = arguments.map { projection ->
        val argumentType = projection.type ?: return@map projection
        val substituted = argumentType.substituteTypeParameters(mapping)
        if (substituted === argumentType) {
          projection
        } else {
          changed = true
          KTypeProjection(projection.variance, substituted)
        }
      }

      if (!changed) {
        this
      } else {
        classifier.createType(newArguments, isMarkedNullable)
      }
    }

    else -> this
  }
}

/**
 * Returns true if this [KType]'s classifier is annotated with [OpenApiNullable].
 */
internal fun KType.hasOpenApiNullableAnnotation(): Boolean {
  return this.asKClass().findAnnotation<OpenApiNullable>() != null
}

/**
 * Resolves [subClass] against the sealed type [sealedType]: `FoundResponse.Good` and `FoundResponse<AuthorDetails>` give
 * `FoundResponse.Good<AuthorDetails>`; a subclass that implements [sealedType] only indirectly gives its star-projected type.
 */
internal fun resolveSubclassType(subClass: KClass<*>, sealedType: KType): KType {
  val sealedInterface = sealedType.asKClass()
  val interfaceMapping: Map<KTypeParameter, KType> = sealedInterface.typeParameters
    .zip(sealedType.arguments)
    .mapNotNull { (typeParameter, argument) -> argument.type?.let { typeParameter to it } }
    .toMap()

  // Only the direct supertype entry keeps the generic relationship between subclass and sealed interface.
  val interfaceSuperType = subClass.supertypes.firstOrNull { it.classifier == sealedInterface }
    ?: return subClass.starProjectedType

  val subclassMapping = mutableMapOf<KTypeParameter, KType>()

  // Walks the supertype declaration and the resolved type in parallel until it reaches a type parameter of the subclass.
  fun assignTypes(superType: KType, expectedType: KType) {
    when (val classifier = superType.classifier) {
      is KTypeParameter -> {
        if (classifier in subClass.typeParameters) {
          subclassMapping.putIfAbsent(classifier, expectedType)
        }
      }

      is KClass<*> -> {
        val expectedClassifier = expectedType.findKClass() ?: return
        // A deviating structure leaves the parameter unresolved.
        if (classifier != expectedClassifier) {
          return
        }

        superType.arguments.zip(expectedType.arguments).forEach { (superArg, expectedArg) ->
          val superArgType = superArg.type ?: return@forEach
          val expectedArgType = expectedArg.type ?: return@forEach
          assignTypes(superArgType, expectedArgType)
        }
      }
    }
  }

  sealedInterface.typeParameters
    .zip(interfaceSuperType.arguments)
    .forEach { (interfaceTypeParameter, superTypeArgument) ->
      val expectedType = interfaceMapping[interfaceTypeParameter] ?: return@forEach
      val superTypeArgumentType = superTypeArgument.type ?: return@forEach
      assignTypes(superTypeArgumentType, expectedType)
    }

  val subclassArguments = subClass.typeParameters.map { typeParameter ->
    val mappedType = subclassMapping[typeParameter]
    val argumentType = mappedType ?: typeParameter.upperBounds.first().makeNotNullable()
    KTypeProjection.invariant(argumentType)
  }

  return subClass.createType(subclassArguments, false)
}
