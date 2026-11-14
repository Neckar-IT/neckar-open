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
package it.neckar.ksp.ts

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import it.neckar.ksp.exportableProperties
import it.neckar.ksp.isDataClass
import it.neckar.ksp.isDeprecated
import it.neckar.ksp.isPublic
import it.neckar.ksp.isSealed
import it.neckar.ksp.isSynthetic
import it.neckar.ksp.isValueClass
import it.neckar.ksp.onlyConstructors
import it.neckar.ksp.onlyPublic
import it.neckar.ksp.skipConstructors
import it.neckar.ksp.skipDeprecated
import it.neckar.ksp.skipIgnoredForExport
import it.neckar.ksp.skipSynthetic
import it.neckar.ksp.skipTypesIgnoredForExport
import it.neckar.ksp.skipTypesNotExported
import it.neckar.ksp.ts.model.CollectingContext
import it.neckar.ksp.ts.model.TsAbstractClass
import it.neckar.ksp.ts.model.TsClass
import it.neckar.ksp.ts.model.TsCompanionObject
import it.neckar.ksp.ts.model.TsConstObject
import it.neckar.ksp.ts.model.TsConstructor
import it.neckar.ksp.ts.model.TsEnumClass
import it.neckar.ksp.ts.model.TsEnumClass.TsEnumValue
import it.neckar.ksp.ts.model.TsFunctionParameter
import it.neckar.ksp.ts.model.TsFunctionSignature
import it.neckar.ksp.ts.model.TsInterface
import it.neckar.ksp.ts.model.TsNamespace
import it.neckar.ksp.ts.model.TsProperty
import it.neckar.ksp.ts.model.TsTopLevelFunction
import it.neckar.ksp.ts.model.TsType
import it.neckar.ksp.ts.model.TsTypeParameter
import it.neckar.ksp.ts.model.TsValueClass
import it.neckar.ksp.ts.model.toTypescriptType

/**
 * Collects all types that are annotated with the annotation to export.
 */
class TypeScriptDefinitionCollectorVisitor(
  val logger: KSPLogger,
  val options: Map<String, String>,
) : KSTopDownVisitor<CollectingContext, Unit>() {

  override fun defaultHandler(node: KSNode, context: CollectingContext) {
    //ignore
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, context: CollectingContext) {
    logger.logging("[TypeScriptDefinitionCollectorVisitor] Visiting class ${classDeclaration.simpleName.asString()}")

    val namespace = TsNamespace.of(classDeclaration.parentDeclaration?.simpleName?.asString())

    val parentParent = classDeclaration.parentDeclaration?.parentDeclaration
    if (parentParent is KSDeclaration) {
      throw IllegalStateException("Nested classes in nested classes are not yet supported. This: ${classDeclaration.qualifiedName?.asString()} Parent: ${classDeclaration.parentDeclaration?.qualifiedName?.asString()}, parentParent: ${parentParent.qualifiedName?.asString()}")
    }

    handleClassDeclaration(
      classDeclaration = classDeclaration,
      context = context,
      //No namespace - we are at top level
      currentNamespace = namespace
    )

    if (false) {
      //Do not call this, to avoid recursion
      super.visitClassDeclaration(classDeclaration, context)
    }
  }

  /**
   * Handles all kinds of class declarations.
   */
  private fun handleClassDeclaration(classDeclaration: KSClassDeclaration, context: CollectingContext, currentNamespace: TsNamespace?) {
    if (classDeclaration.isDeprecated()) {
      //Skip deprecated functions
      return
    }

    if (classDeclaration.isInternal()) {
      //Skip internal classes
      return
    }

    if (classDeclaration.isPrivate()) {
      //Skip private classes
      return
    }

    when (classDeclaration.classKind) {
      ClassKind.CLASS -> {
        //always generate interfaces - even for classes
        when {
          classDeclaration.isValueClass() -> {
            collectValueClass(classDeclaration, context, currentNamespace)
          }

          classDeclaration.isAbstract() || classDeclaration.isSealed -> {
            collectAbstractClass(classDeclaration, context, currentNamespace)
          }

          else -> {
            collectClass(classDeclaration, context, currentNamespace)
          }
        }
      }

      ClassKind.INTERFACE -> {
        collectInterface(classDeclaration, context, currentNamespace)
      }

      ClassKind.ENUM_CLASS -> {
        collectEnumClass(classDeclaration, context, currentNamespace)
      }

      ClassKind.ENUM_ENTRY -> {
        //Skip enum entries
        return
      }

      ClassKind.OBJECT -> {
        collectObject(classDeclaration, context, currentNamespace)
      }

      ClassKind.ANNOTATION_CLASS -> {
        return // do nothing
      }
    }

    //Recursion not necessary, since all annotated methods are called
    if (false) {
      classDeclaration.collectNestedDeclarations().forEach { childDeclaration ->
        handleClassDeclaration(
          classDeclaration = childDeclaration,
          context = context,
          currentNamespace = TsNamespace(currentNamespace, classDeclaration.simpleName.asString())
        )
      }
    }
  }

  override fun visitFunctionDeclaration(function: KSFunctionDeclaration, context: CollectingContext) {
    //Only handle top level functions
    if ((function.parent is KSFile).not()) {
      return
    }

    if (function.isDeprecated()) {
      //Skip deprecated functions
      return
    }

    if (function.isSynthetic) {
      //Skip synthetic functions
      return
    }

    if (function.isPublic.not()) {
      //Skip private functions
      return
    }

    if (function.isConstructor()) {
      throw IllegalStateException("Constructor should not be visited")
    }

    //We are sure that this is a top level function
    val toTypescriptFunction = function.toTypescriptFunction()
    context.addTopLevelFunction(TsTopLevelFunction(toTypescriptFunction))
  }


  private fun collectEnumClass(declaration: KSClassDeclaration, context: CollectingContext, currentNamespace: TsNamespace?) {
    val enumName = declaration.collectTypeScriptName()
    val docString = declaration.docString
    val kotlinSource = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()

    val constructors = declaration.collectConstructors()
    val superTypes = declaration.collectSuperTypes()
    val typeParameters = declaration.collectTypeParameters()

    val enumValues = declaration.collectEnumValues()
    val properties = declaration.collectProperties()
    val functions = declaration.collectFunctions()

    val companionObject = declaration.collectCompanionObject()

    val classToCreate = TsEnumClass(
      name = enumName,
      namespace = currentNamespace,
      doc = docString,
      kotlinSource = kotlinSource,
      constructors = constructors,
      superTypes = superTypes,
      typeParameters = typeParameters,
      values = enumValues,
      properties = properties,
      functions = functions,
      companionObject = companionObject,
    )

    context.addEnum(classToCreate)
  }

  private fun collectValueClass(declaration: KSClassDeclaration, context: CollectingContext, currentNamespace: TsNamespace?) {
    val classNameString = declaration.collectTypeScriptName()
    val docString = declaration.docString
    val kotlinSource = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()

    val properties = declaration.collectBackingFieldProperties()
    check(properties.size == 1) { "Value classes must have exactly one backing-field property but found ${properties.joinToString(", ", "[ ", " ]") { it.name }} in $classNameString" }

    val classToCreate = TsValueClass(
      name = classNameString,
      namespace = currentNamespace,
      doc = docString,
      kotlinSource = kotlinSource,
      property = properties.first(),
    )
    context.addValueClass(classToCreate)
  }

  private fun collectInterface(declaration: KSClassDeclaration, context: CollectingContext, currentNamespace: TsNamespace?) {
    val classNameString = declaration.collectTypeScriptName()
    val docString = declaration.docString
    val kotlinSource = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()

    val superTypes = declaration.collectSuperTypes()
    val typeParameters = declaration.collectTypeParameters()

    val properties = declaration.collectProperties()
    val functions: List<TsFunctionSignature> = declaration.collectFunctions()

    val companionObject = declaration.collectCompanionObject()

    val classToCreate = TsInterface(
      name = classNameString,
      namespace = currentNamespace,
      doc = docString,
      kotlinSource = kotlinSource,
      superTypes = superTypes,
      typeParameters = typeParameters,
      properties = properties,
      functions = functions,
      companionObject = companionObject,
    )
    context.addInterface(classToCreate)
  }

  private fun collectAbstractClass(declaration: KSClassDeclaration, context: CollectingContext, currentNamespace: TsNamespace?) {
    val classNameString = declaration.collectTypeScriptName()
    val docString = declaration.docString
    val kotlinSource = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()

    val superTypes = declaration.collectSuperTypes()
    val typeParameters = declaration.collectTypeParameters()
    val constructors = declaration.collectConstructors()

    val properties = declaration.collectProperties()
    val functions: List<TsFunctionSignature> = declaration.collectFunctions()

    val companionObject = declaration.collectCompanionObject()

    val classToCreate = TsAbstractClass(
      name = classNameString,
      namespace = currentNamespace,
      doc = docString,
      kotlinSource = kotlinSource,
      superTypes = superTypes,
      constructors = constructors,
      properties = properties,
      typeParameters = typeParameters,
      functions = functions,
      companionObject = companionObject,
    )

    context.addAbstractClass(classToCreate)
  }


  private fun collectClass(declaration: KSClassDeclaration, context: CollectingContext, currentNamespace: TsNamespace?) {
    val classNameString = declaration.collectTypeScriptName()
    val docString = declaration.docString
    val kotlinSource = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()

    val superTypes = declaration.collectSuperTypes()
    val typeParameters = declaration.collectTypeParameters()
    val constructors = declaration.collectConstructors()

    val properties = declaration.collectProperties()

    val functions: List<TsFunctionSignature> = when {
      declaration.isDataClass() -> {
        buildList {
          addAll(declaration.collectFunctions())
          add(
            TsFunctionSignature(
              name = "copy",
              parameters = properties.map { TsFunctionParameter(it.name, it.type, isOptional = false) },
              returnType = declaration.asStarProjectedType().toTypescriptType(),
              typeParameters = emptyList(),
              doc = null,
            )
          )

          if (this.none { it.name == "toString" }) {
            add(
              TsFunctionSignature(
                name = "toString",
                parameters = emptyList(),
                returnType = TsType.String,
                typeParameters = emptyList(),
                doc = null,
              )
            )
          }
        }
      }

      else -> {
        declaration.collectFunctions()
      }
    }

    val companionObject = declaration.collectCompanionObject()

    val classToCreate = TsClass(
      name = classNameString,
      namespace = currentNamespace,
      doc = docString,
      kotlinSource = kotlinSource,
      superTypes = superTypes,
      constructors = constructors,
      properties = properties,
      typeParameters = typeParameters,
      functions = functions,
      companionObject = companionObject,
    )

    context.addClass(classToCreate)
  }

  private fun collectObject(declaration: KSClassDeclaration, context: CollectingContext, currentNamespace: TsNamespace?) {
    val objectNameString = declaration.collectTypeScriptName()
    val docString = declaration.docString
    val kotlinSource = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()

    val superTypes = declaration.collectSuperTypes()
    val typeParameters = declaration.collectTypeParameters()

    val properties = declaration.collectProperties()
    val functions: List<TsFunctionSignature> = declaration.collectFunctions()

    val constObjectToCreate = TsConstObject(
      name = objectNameString,
      namespace = currentNamespace,
      doc = docString,
      kotlinSource = kotlinSource,
      superTypes = superTypes,
      typeParameters = typeParameters,
      properties = properties,
      functions = functions,
    )

    context.addConstObject(constObjectToCreate)
  }
}


/**
 * Returns the TypeScript name of the declaration.
 * Uses the `JsName` annotation if present, otherwise uses the simple name.
 */
fun KSDeclaration.collectTypeScriptName(): String {
  val nameFromAnnotation = this.findNameFromAnnotation()
  if (nameFromAnnotation != null) {
    return nameFromAnnotation
  }

  return simpleName.asString()
}

/**
 * Returns the name from the annotation (JsName or TsName) if present
 */
fun KSAnnotated.findNameFromAnnotation(): String? {
  return annotations.find {
    val annotationName = it.shortName.asString()
    annotationName == "JsName" || annotationName == "TsName"
  }?.arguments?.first {
    it.name?.asString() == "name"
  }?.value?.toString()
}

fun KSClassDeclaration.collectSuperTypes(): List<TsType> {
  return superTypes
    .skipDeprecated()
    .skipSynthetic()
    .map { it.resolve() }
    .skipTypesIgnoredForExport()
    .skipTypesNotExported()
    .map { it.toTypescriptType() }
    .toList()
}

fun KSDeclaration.collectTypeParameters(): List<TsTypeParameter> {
  return typeParameters.map { it.toTypescriptTypeParameter() }
}

fun KSClassDeclaration.collectConstructors(): List<TsConstructor> {
  return getDeclaredFunctions()
    .onlyPublic()
    .skipDeprecated()
    .skipSynthetic()
    .onlyConstructors()
    .skipIgnoredForExport()
    .map(KSFunctionDeclaration::toTypescriptConstructor).toList()
}

/**
 * Returns the collected functions of the class declaration.
 */
fun KSClassDeclaration.collectFunctions(): List<TsFunctionSignature> {
  return getDeclaredFunctions()
    .onlyPublic()
    .skipDeprecated()
    .skipSynthetic()
    .skipConstructors()
    .skipIgnoredForExport()
    .map(KSFunctionDeclaration::toTypescriptFunction).toList()
}

fun KSClassDeclaration.collectProperties(): List<TsProperty> {
  return exportableProperties()
    .map(KSPropertyDeclaration::toTypescriptProperty).toList()
}

/**
 * Returns only the properties that have a backing field.
 *
 * Used for the value-class uniqueness check: a value class must have exactly one
 * backing-field property (the constructor property). Derived properties
 * (`val found: Boolean get() = ...`) have no backing field and are not emitted for a value class,
 * so they must not count towards the uniqueness check.
 */
fun KSClassDeclaration.collectBackingFieldProperties(): List<TsProperty> {
  return exportableProperties()
    .filter { it.hasBackingField }
    .map(KSPropertyDeclaration::toTypescriptProperty).toList()
}

fun KSClassDeclaration.collectCompanionObject(): TsCompanionObject? {
  return declarations
    .filterIsInstance<KSClassDeclaration>()
    .filter { it.isCompanionObject }
    .filter { it.classKind != ClassKind.ENUM_CLASS }
    .onlyPublic()
    .skipDeprecated()
    .skipIgnoredForExport().toList()
    .also { check(it.size <= 1) { "Only one companion object is allowed" } }
    .firstOrNull()
    ?.toTypescriptCompanionObject()
}

/**
 * Returns *all* nested classes, interfaces, enums and objects
 */
fun KSClassDeclaration.collectNestedDeclarations(): List<KSClassDeclaration> {
  return declarations
    .filterIsInstance<KSClassDeclaration>()
    .filter { it.isCompanionObject.not() }
    //.filter { it.classKind == ClassKind.CLASS }
    .onlyPublic()
    .skipDeprecated()
    .skipIgnoredForExport()
    .toList()
}

/**
 * Returns the nested classes
 */
@Deprecated("use nested all")
fun KSClassDeclaration.collectNestedClasses(): List<KSClassDeclaration> {
  return declarations
    .filterIsInstance<KSClassDeclaration>()
    .filter { it.isCompanionObject.not() }
    .filter { it.classKind == ClassKind.CLASS }
    .onlyPublic()
    .skipDeprecated()
    .skipIgnoredForExport()
    .toList()
}

@Deprecated("use nested all")
fun KSClassDeclaration.collectNestedEnumClasses(): List<KSClassDeclaration> {
  return declarations
    .filterIsInstance<KSClassDeclaration>()
    .filter { it.isCompanionObject.not() }
    .filter { it.classKind == ClassKind.ENUM_CLASS }
    .onlyPublic()
    .skipDeprecated()
    .skipIgnoredForExport()
    .toList()
}

fun KSClassDeclaration.collectEnumValues(): List<TsEnumValue> {
  return declarations
    .filterIsInstance<KSClassDeclaration>()
    .filter { it.classKind == ClassKind.ENUM_ENTRY }
    .onlyPublic()
    .skipDeprecated()
    .skipIgnoredForExport()
    .map(KSClassDeclaration::toTypescriptEnumValue).toList()
}

fun KSTypeParameter.toTypescriptTypeParameter(): TsTypeParameter {
  return TsTypeParameter(
    name = name.asString(),
    bounds = bounds
      .skipDeprecated()
      .skipSynthetic()
      .map { it.resolve() }
      .skipTypesNotExported()
      .map { it.toTypescriptType() }.toList(),
  )
}

fun KSPropertyDeclaration.toTypescriptProperty(): TsProperty {
  val name = collectTypeScriptName()
  return TsProperty(
    name = name,
    type = this.type.toTypescriptType(),
    doc = this.docString,
  )
}

fun KSClassDeclaration.toTypescriptEnumValue(): TsEnumValue {
  val name = collectTypeScriptName()
  return TsEnumValue(value = name, doc = this.docString)
}

fun KSClassDeclaration.toTypescriptCompanionObject(): TsCompanionObject {
  val properties = collectProperties()
  val functions = collectFunctions()

  return TsCompanionObject(
    doc = docString,
    kotlinSource = qualifiedName?.asString() ?: simpleName.asString(),
    properties = properties,
    functions = functions,
  )
}

fun KSFunctionDeclaration.toTypescriptFunction(): TsFunctionSignature {
  val name = collectTypeScriptName()
  val parameters: List<TsFunctionParameter> = parameters.toTypescriptParameters()
  val tsReturnType = returnType.toTypescriptType()
  val typeParameters = typeParameters.map { it.toTypescriptTypeParameter() }

  return TsFunctionSignature(
    name = name,
    parameters = parameters,
    returnType = tsReturnType,
    typeParameters = typeParameters,
    doc = docString,
  )
}

fun KSFunctionDeclaration.toTypescriptConstructor(): TsConstructor {
  val parameters: List<TsFunctionParameter> = parameters.toTypescriptParameters()
  return TsConstructor(parameters = parameters, doc = docString)
}

/**
 * Converts the parameters to TypeScript parameters.
 */
fun List<KSValueParameter>.toTypescriptParameters(): List<TsFunctionParameter> {
  return mapIndexed { index: Int, parameter: KSValueParameter ->
    val name = parameter.findNameFromAnnotation() ?: parameter.name?.asString() ?: "param$index"
    val tsType = parameter.type.toTypescriptType()
    val doc: String? = null //currently doc is not supported by KSP

    TsFunctionParameter(
      name = name,
      type = tsType,
      isOptional = parameter.hasDefault,
      doc = doc,
    )
  }
}
