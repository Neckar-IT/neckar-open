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
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.JsPlatformInfo
import com.google.devtools.ksp.processing.JvmPlatformInfo
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSModifierListOwner
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import it.neckar.open.annotations.TsExport
import java.io.File
import kotlin.contracts.contract
import kotlin.reflect.KClass

/**
 * Returns true if this is running on the JS platform
 */
fun SymbolProcessorEnvironment.isRunningForJsPlatform(): Boolean {
  require(platforms.size == 1) { "Only one platform is supported but was [$platforms]" }

  return platforms.any { it is JsPlatformInfo }
}

/**
 * Returns true if this is running on the JVM platform
 */
fun SymbolProcessorEnvironment.isRunningForJvmPlatform(): Boolean {
  require(platforms.size == 1) { "Only one platform is supported but was [$platforms]" }

  return platforms.any { it is JvmPlatformInfo }
}

fun SymbolProcessorEnvironment.isRunningForTests(): Boolean {
  val type = codeGenerator::class.java

  val kotlinDir = type.getDeclaredField("kotlinDir").also {
    it.isAccessible = true
  }.get(codeGenerator) as File

  return when {
    kotlinDir.absolutePath.contains("jvmMain") -> false
    kotlinDir.absolutePath.contains("jvmTest") -> true
    kotlinDir.absolutePath.contains("jsMain") -> false
    kotlinDir.absolutePath.contains("jsTest") -> true
    else -> false
  }
}

/**
 * Returns the fully qualified name - if available.
 * Falls back to the simple name
 */
fun KSDeclaration.fqName(): String {
  this.qualifiedName?.let {
    return it.asString()
  }

  return this.simpleName.asString()
}

/**
 * Formats the location
 */
fun Location.format(): String {
  return when (this) {
    is FileLocation -> "${this.filePath}:${this.lineNumber}"
    NonExistLocation -> "-- non existent location --"
  }
}

/**
 * Returns true if this has an annotation of the provided type
 */
fun <T : Any> KSAnnotated.hasAnnotation(annotationKlass: KClass<T>): Boolean {
  val annotationClassQualifiedName = requireNotNull(annotationKlass.qualifiedName) { "Annotation qualified class name must not be null for $annotationKlass" }

  return this.annotations.any {
    it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationClassQualifiedName
  }
}

/**
 * Returns the containing class of this function (if there is one).
 * Will return null for top level functions
 */
fun KSFunctionDeclaration.findContainingClass(): KSClassDeclaration? {
  return parentDeclaration as? KSClassDeclaration
}

/**
 * The names of the kotlin primitive types (for the JVM)
 */
private val kotlinPrimitiveNames = setOf("kotlin.Int", "kotlin.Boolean", "kotlin.Char", "kotlin.Byte", "kotlin.Short", "kotlin.Long", "kotlin.Float", "kotlin.Double")

/**
 * Returns true if this declaration is a kotlin primitive type
 */
fun KSDeclaration.isPrimitive(): Boolean {
  val qualifiedName = this.qualifiedName ?: throw IllegalStateException("Could not find the qualified name for $this @ ${this.location.format()}")
  return kotlinPrimitiveNames.contains(qualifiedName.asString())
}

@OptIn(KspExperimental::class)
fun KSClassDeclaration.isKotlinClass(): Boolean {
  return origin == Origin.KOTLIN ||
    origin == Origin.KOTLIN_LIB ||
    isAnnotationPresent(Metadata::class)
}

private val kotlinPrimitiveArrayNames = setOf(
  "kotlin.IntArray",
  "kotlin.BooleanArray",
  "kotlin.CharArray",
  "kotlin.ByteArray",
  "kotlin.ShortArray",
  "kotlin.LongArray",
  "kotlin.FloatArray",
  "kotlin.DoubleArray"
)

/**
 * Returns true if this is of type [ClassKind.CLASS]
 */
val ClassKind.isClass: Boolean
  get() {
    return this == ClassKind.CLASS
  }

val ClassKind.isObject: Boolean
  get() {
    return this == ClassKind.OBJECT
  }

/**
 * Returns true if this declaration is a primitive array
 */
fun KSDeclaration.isPrimitiveArray(): Boolean {
  val qualifiedName = this.qualifiedName?.asString()
  return qualifiedName in kotlinPrimitiveArrayNames
}

private val kotlinPrimitiveCollectionNames = setOf(
  "it.neckar.open.collections.DoubleArrayList",
  "it.neckar.open.collections.IntArrayList",
  "it.neckar.open.collections.FloatArrayList",
)

/**
 * Returns true if this declaration is a primitive collection
 */
fun KSDeclaration.isPrimitiveCollection(): Boolean {
  val qualifiedName = this.qualifiedName?.asString()
  return qualifiedName in kotlinPrimitiveCollectionNames
}

/**
 * Returns true if this is a companion object
 */
fun KSDeclaration.isCompanionObject(): Boolean {
  contract {
    returns(true) implies (this@isCompanionObject is KSClassDeclaration)
  }

  return this is KSClassDeclaration && this.isCompanionObject
}

/**
 * Returns true, if this is an enum option
 */
fun KSDeclaration.isEnumOption(): Boolean {
  contract {
    returns(true) implies (this@isEnumOption is KSClassDeclaration)
  }

  return this is KSClassDeclaration && this.classKind == ClassKind.ENUM_ENTRY
}

/**
 * Returns true if the [KSDeclaration] is a value class (modifier [Modifier.VALUE] is set)
 */
fun KSDeclaration.isValueClass(): Boolean {
  contract {
    returns(true) implies (this@isValueClass is KSClassDeclaration)
  }

  if ((this is KSClassDeclaration).not()) {
    return false
  }

  return this.modifiers.contains(Modifier.VALUE)
}

/**
 * Returns true if the [KSDeclaration] is a data class (modifier [Modifier.DATA] is set)
 */
fun KSDeclaration.isDataClass(): Boolean {
  contract {
    returns(true) implies (this@isDataClass is KSClassDeclaration)
  }

  if ((this is KSClassDeclaration).not()) {
    return false
  }

  return this.modifiers.contains(Modifier.DATA)
}

/**
 * Returns true if this is a class or interface or object
 */
fun KSDeclaration.isClassLike(): Boolean {
  contract {
    returns(true) implies (this@isClassLike is KSClassDeclaration)
  }

  return (this is KSClassDeclaration)
}

fun <T : Annotation> Sequence<KSAnnotation>.containsAnnotation(annotationKlass: KClass<T>): Boolean {
  val annotationClassSimpleName = requireNotNull(annotationKlass.simpleName) { "Annotation simple class name must not be null for $annotationKlass" }
  return containsAnnotation(annotationClassSimpleName)
}

fun Sequence<KSAnnotation>.containsAnnotation(annotationClassSimpleName: String): Boolean {
  return any {
    it.shortName.asString() == annotationClassSimpleName
  }
}

/**
 * Returns true if the function itself or the return type is annotated with the provided annotation
 */
fun <T : Annotation> KSFunctionDeclaration.isReturnTypeAnnotated(annotationType: KClass<T>): Boolean {
  return annotations.containsAnnotation(annotationType) ||
    returnType?.annotations?.containsAnnotation(annotationType) == true
}

/**
 * Returns true if this parameter is annotated itself - or its type is annotated
 */
fun <T : Annotation> KSValueParameter.isAnnotatedOrTypeAnnotated(annotationKlass: KClass<T>): Boolean {
  return annotations.containsAnnotation(annotationKlass) || //the parameter itself is annotated
    type.annotations.containsAnnotation(annotationKlass) //the parameter type is annotated
}

/**
 * Returns true if this parameter has the type function
 */
fun <T : Annotation> KSValueParameter.isFunctionTypeWithAnnotated(annotationKlass: KClass<T>): Boolean {
  val resolvedType = this.type.resolve()

  if (resolvedType.isFunctionType) {
    return resolvedType.arguments.any {
      it.isAnnotationPresent(annotationKlass)
    }
  }

  return false
}

/**
 * Returns the parameter that matches the provided parameter.
 */
fun getSuperParameter(function: KSFunctionDeclaration, superFunction: KSFunctionDeclaration, parameter: KSValueParameter): KSTypeReference {
  // Match parameters by index and return the type
  val index = function.parameters.indexOf(parameter)
  require(index >= 0) {
    "Parameter [${parameter.name?.asString()}] not found in function [${function.simpleName.asString()}]"
  }
  require(index in superFunction.parameters.indices) {
    "No matching parameter at index $index in super function [${superFunction.simpleName.asString()}] with ${superFunction.parameters.size} parameters"
  }

  return superFunction.parameters[index].type
}

fun findSuperParameter(function: KSFunctionDeclaration, superFunction: KSFunctionDeclaration?, parameter: KSValueParameter): KSTypeReference? {
  if (superFunction == null) {
    return null
  }

  return getSuperParameter(function, superFunction, parameter)
}

/**
 * Returns true if this function contains the provided annotation anywhere in the declaration or parameters
 */
fun <T : Annotation> KSFunctionDeclaration.hasAnnotationInDeclarationOrParameters(annotationKlass: KClass<T>): Boolean {
  return isReturnTypeAnnotated(annotationKlass) || //The method itself or return type is annotated
    parameters.any { parameter ->
      parameter.isAnnotatedAnywhere(annotationKlass)
    }
}

/**
 * Returns true if the parameter has the provided annotation anywhere.
 * * is annotated itself
 * * has annotation at the type
 * * is of function type and the function has an annotation somewhere
 */
fun <T : Annotation> KSValueParameter.isAnnotatedAnywhere(annotationKlass: KClass<T>): Boolean {
  return this.isAnnotatedOrTypeAnnotated(annotationKlass) ||
    this.isFunctionTypeWithAnnotated(annotationKlass)
}

/**
 * Returns true if this declaration is deprecated (by checking the annotations)
 */
fun KSAnnotated.isDeprecated(): Boolean {
  val name: String = getAnnotationName<Deprecated>()
  return annotations.any { it.shortName.asString() == name }
}

/**
 * Returns true if this declaration is ignored for export (by checking the annotations)
 */
fun KSAnnotated.isIgnoredForExport(): Boolean {
  return annotations.any { it.shortName.asString() == TsExport.jsExportIgnoredAnnotationName }
}

fun KSAnnotated.isNotExported(): Boolean {
  return isExported().not()
}

/**
 * Returns true if this declaration is exported (by checking the annotations)
 */
fun KSAnnotated.isExported(): Boolean {
  val tsExportAnnotationName = getAnnotationName<TsExport>()

  return annotations.any {
    val shortName = it.shortName.asString()
    shortName == TsExport.jsExportAnnotationName ||
      shortName == tsExportAnnotationName
  }
}

private inline fun <reified T : Annotation> KSAnnotated.getAnnotationName(): String {
  return T::class.simpleName ?: throw IllegalStateException("Could not find the simple name for ${T::class} @ ${this.location.format()}")
}

/**
 * Returns true if this property is a field - in a class
 */
fun KSPropertyDeclaration.isClassProperty(): Boolean {
  return this.parentDeclaration is KSClassDeclaration
}

/**
 * Returns true if this property is a field
 */
fun KSPropertyDeclaration.isTopLevelProperty(): Boolean {
  return this.parentDeclaration == null
}

/**
 * Returns the value type!
 * Must only be called for value classes
 */
fun KSClassDeclaration.getValueType(): KSTypeReference {
  require(this.isValueClass()) {
    "Declaration ${this.simpleName.asString()} is not a value class @ ${this.location.format()}"
  }

  val primaryConstructor = primaryConstructor ?: throw IllegalArgumentException("No primary constructor for ${simpleName.asString()} @ ${location.format()}")
  val constructorParameter = primaryConstructor.parameters.firstOrNull() ?: throw IllegalArgumentException("No constructor parameter for ${simpleName.asString()} @ ${location.format()}")

  return constructorParameter.type
}

/**
 * Returns true if this declaration implements a sealed interface
 */
fun KSClassDeclaration.implementsSealed(): Boolean {
  return getAllSuperTypes().any { superType ->
    val declaration = superType.declaration
    if (declaration is KSClassDeclaration) {
      declaration.modifiers.contains(Modifier.SEALED)
    } else {
      false
    }
  }
}

/**
 * Returns true if this declaration has the provided modifier
 */
fun KSModifierListOwner.hasModifier(element: Modifier): Boolean {
  return modifiers.contains(element)
}

/**
 * Returns true if this declaration is external
 */
val KSModifierListOwner.isExternal: Boolean
  get() = hasModifier(Modifier.EXTERNAL)

val KSModifierListOwner.isPrivate: Boolean
  get() = hasModifier(Modifier.PRIVATE)

val KSModifierListOwner.isInternal: Boolean
  get() = hasModifier(Modifier.INTERNAL)

/**
 * Returns true if this declaration has been marked as public explicitly (by using the public modifier)
 */
val KSModifierListOwner.isExplicitlyPublic: Boolean
  get() = hasModifier(Modifier.PUBLIC)

/**
 * Returns true if this declaration is public (not internal, not private)
 */
val KSModifierListOwner.isPublic: Boolean
  get() = isPrivate.not() && isInternal.not()

val KSModifierListOwner.isOperator: Boolean
  get() = hasModifier(Modifier.OPERATOR)

val KSModifierListOwner.isAbstract: Boolean
  get() = hasModifier(Modifier.ABSTRACT)

val KSModifierListOwner.isSealed: Boolean
  get() = hasModifier(Modifier.SEALED)

/**
 * Returns true if the origin is [Origin.SYNTHETIC]
 */
val KSNode.isSynthetic: Boolean
  get() = origin == Origin.SYNTHETIC

/**
 * Returns the names of the type references
 */
fun Sequence<KSTypeReference>.names(): List<String> {
  return this.map { it.resolve().declaration.simpleName.asString() }.toList()
}

/**
 * Returns true if this is a nullable type
 */
val KSValueParameter.isNullable: Boolean
  get() = this.type.resolve().isMarkedNullable

fun <T : KSDeclaration> Sequence<T>.onlyPublic(): Sequence<T> {
  return filter { declaration -> declaration.isPublic() }
}

fun <T : KSNode> Sequence<T>.skipSynthetic(): Sequence<T> {
  return filter { declaration -> declaration.isSynthetic.not() }
}

fun <T : KSAnnotated> Sequence<T>.skipDeprecated(): Sequence<T> {
  return filter { declaration -> declaration.isDeprecated().not() }
}

fun <T : KSAnnotated> Sequence<T>.skipNotExported(): Sequence<T> {
  return filter { declaration -> declaration.isNotExported().not() }
}

/**
 * Skips the elements that are marked as "ignored for export"
 */
fun <T : KSAnnotated> Sequence<T>.skipIgnoredForExport(): Sequence<T> {
  return filter { declaration -> declaration.isIgnoredForExport().not() }
}

fun <T : KSType> Sequence<T>.skipTypesNotExported(): Sequence<T> {
  return filter { type -> type.declaration.isNotExported().not() }
}

fun <T : KSType> Sequence<T>.skipTypesIgnoredForExport(): Sequence<T> {
  return filter { type -> type.declaration.isIgnoredForExport().not() }
}

fun <T : KSFunctionDeclaration> Sequence<T>.onlyConstructors(): Sequence<T> {
  return filter { declaration -> declaration.isConstructor() }
}

fun <T : KSFunctionDeclaration> Sequence<T>.skipConstructors(): Sequence<KSFunctionDeclaration> {
  return filter { declaration -> declaration.isConstructor().not() }
}

/**
 * Returns the declared properties that are eligible for export (public, not deprecated, not synthetic, not ignored).
 */
fun KSClassDeclaration.exportableProperties(): Sequence<KSPropertyDeclaration> {
  return getDeclaredProperties()
    .onlyPublic()
    .skipDeprecated()
    .skipSynthetic()
    .skipIgnoredForExport()
}

/**
 * Returns the (still relevant) sealed subclasses of this class.
 * Skips deprecated, ignored for export and not exported classes
 */
fun KSClassDeclaration.relevantSealedSubclasses(): Sequence<KSClassDeclaration> {
  return getSealedSubclasses()
    .skipDeprecated()
    .skipIgnoredForExport()
    .skipNotExported()
}

/**
 * Returns the inner classes of this class.
 */
fun KSClassDeclaration.getInnerClassLikes(): Sequence<KSClassDeclaration> {
  return declarations
    .filterIsInstance<KSClassDeclaration>()
}

/**
 * Returns the inner classes and the inner classes of the inner classes recursively
 */
fun KSClassDeclaration.getInnerClassLikesRecursively(): Sequence<KSClassDeclaration> {
  return getInnerClassLikes() +
    getInnerClassLikes().flatMap { it.getInnerClassLikesRecursively() }
}


/**
 * Returns true if this is a [KSTypeParameter]
 */
fun KSType.isTypeParameter(): Boolean {
  return this.declaration is KSTypeParameter
}
