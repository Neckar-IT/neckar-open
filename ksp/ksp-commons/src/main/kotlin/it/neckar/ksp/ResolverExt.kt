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
@file:OptIn(KspExperimental::class)

package it.neckar.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import kotlin.reflect.KClass

fun <T : Annotation> Resolver.getClassesWithAnnotation(annotationKlass: KClass<T>): Sequence<KSClassDeclaration> {
  return getAllFiles().flatMap { ksFile ->
    ksFile.declarations.filterIsInstance<KSClassDeclaration>().filter { it.hasAnnotation(annotationKlass) }
  }
}


/**
 * Returns all function declarations that have the specified annotation or have a parameter with the specified annotation.
 */
fun <T : Annotation> Resolver.getFunctionsWithAnnotationOrAnnotatedParameters(annotationKlass: KClass<T>): Sequence<KSFunctionDeclaration> {
  //Collect all functions in all files. Top level functions, functions from classes and its companion objects
  val allFunctions = getAllFiles().flatMap { ksFile ->
    ksFile.declarations.flatMap { declaration ->
      when (declaration) {
        is KSFunctionDeclaration -> { //Function declared in a file
          sequenceOf(declaration)
        }

        is KSClassDeclaration -> {
          val companionObjectFunctions = declaration.declarations.flatMap {
            if (it.isCompanionObject()) {
              it.getDeclaredFunctions()
            } else {
              emptySequence()
            }
          }

          //Function declared in a class
          val declaredFunctions = declaration.getDeclaredFunctions()

          //Combine the results
          declaredFunctions + companionObjectFunctions
        }

        else -> emptySequence()
      }
    }
  }

  return allFunctions.filter { functionDeclaration ->
    functionDeclaration.hasAnnotationInDeclarationOrParameters(annotationKlass)
  }
}

/**
 * Returns the super function - if there is one
 */
fun Resolver.findSuperFunction(function: KSFunctionDeclaration): KSFunctionDeclaration? {
  val containingClass = function.findContainingClass() ?: return null

  for (superType in containingClass.superTypes) {
    // Resolve the type declaration of the super type
    val superTypeDeclaration = superType.resolve().declaration

    // If it's a class or interface, check its functions
    if (superTypeDeclaration is KSClassDeclaration) {
      for (superFunction in superTypeDeclaration.getDeclaredFunctions()) {

        if (overrides(function, superFunction)) {
          return superFunction
        }
      }
    }
  }

  return null
}

/**
 * Returns the type of the parameter in the super function
 */
fun Resolver.findSuperTypeParameterType(parameter: KSValueParameter): KSTypeReference? {
  val function: KSFunctionDeclaration = parameter.parent as? KSFunctionDeclaration ?: return null

  val superFunction = findSuperFunction(function) ?: return null
  return getSuperParameter(function, superFunction, parameter)
}
