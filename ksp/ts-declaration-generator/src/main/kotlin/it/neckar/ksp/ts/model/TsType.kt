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
package it.neckar.ksp.ts.model

/**
 * Represents a TypeScript type to be used in the generation of the TypeScript declaration file
 */
//TODO think about creating sub classes for primitives, sealed interfaces, lambdas, ...
data class TsType(
  /**
   * The name of the type
   */
  override val name: String,

  override val namespace: TsNamespace? = null,

  val isNullable: Boolean = false,

  val isArray: Boolean = false,

  val typeArguments: List<TsType> = emptyList(),

  /**
   * Is required to generate inheritance statements when extending this type.
   * In most cases, the information is not required.
   *
   * In many cases, this is null - we do not have any relevant information (e.g., generics, etc.)
   */
  val classKind: ClassKind? = null,

  /**
   * If this type is a sealed class, this contains the subtypes.
   * In most cases, this is empty.
   */
  val sealedSubTypes: List<TsType> = emptyList(),

  /**
   * The name of the module that this type is imported from.
   */
  val importedFrom: TsModuleReference,

  ) : TsCodeFragmentProvider, HasNamespace {

  init {
    require(name.contains("[").not()) { "Name $name must not contain '['" }
    require(name.contains("]").not()) { "Name $name must not contain ']'" }
    require(name.contains("<").not()) { "Name $name must not contain '<'" }
    require(name.contains(">").not()) { "Name $name must not contain '>'" }
    require(name.contains("::").not()) { "Name $name must not contain '::'" }
    require(name.contains(".").not()) { "Name $name must not contain '.'" }

    if (name == "Promise") {
      require(typeArguments.size == 1) { "Promise must have exactly one type argument but was [$typeArguments]" }
    }
  }

  /**
   * Returns true if this type is a primitive type in TypeScript.
   */
  val isPrimitive: Boolean
    get() {
      return classKind == ClassKind.Primitive
    }

  val isInterface: Boolean
    get() {
      return classKind == ClassKind.Interface
    }

  val isClass: Boolean
    get() {
      return classKind == ClassKind.Class
    }

  val isSealedType: Boolean
    get() {
      return sealedSubTypes.isNotEmpty()
    }

  /**
   * Converts this TypeScript type into its string representation.
   *
   * @return A string representing the TypeScript type.
   */
  override fun toTypescriptString(indentation: Indentation): String {
    return if (sealedSubTypes.isNotEmpty()) {
      buildString {
        append(indentation)
        if (isArray) append("Array<")
        append(sealedSubTypes.joinToString(" | ") { it.name })
        if (isArray) append(">")
        if (isNullable) append(" | null | undefined")
      }
    } else {
      toTypescriptTypeString(indentation)
    }
  }

  /**
   * Generates the TypeScript string representation of this type.
   */
  fun toTypescriptTypeString(indentation: Indentation = Indentation.None): String {
    return buildString {
      append(indentation)
      if (namespace != null) append("${namespace.name}.")
      append(name)
      if (typeArguments.isNotEmpty()) {
        append("<")
        append(typeArguments.joinToString(", ") { it.toTypescriptString(Indentation.None) })
        append(">")
      }
      if (isArray) append("[]")
      if (isNullable) append(" | null | undefined")
    }
  }

  fun withNamespace(namespace: TsNamespace?): TsType {
    return copy(namespace = namespace)
  }

  fun withNullable(newNullable: Boolean): TsType {
    return copy(isNullable = newNullable)
  }

  fun orNullable(newNullable: Boolean): TsType {
    return copy(isNullable = isNullable || newNullable)
  }

  /**
   * Returns this type as a nullable type.
   */
  fun asNullable(): TsType {
    if (this.isNullable) {
      return this
    }

    return copy(isNullable = true)
  }

  fun asArray(newIsArray: Boolean = true): TsType {
    return copy(isArray = newIsArray)
  }

  fun getAllTypesToImport(): Set<TsType> {
    return buildSet {
      if (isPrimitive.not()) {
        add(this@TsType)
      }
      typeArguments.forEach { addAll(it.getAllTypesToImport()) }
      sealedSubTypes.forEach { addAll(it.getAllTypesToImport()) }
    }
  }

  override fun toString(): String {
    return buildString {
      append("TsType('")
      if (namespace != null) append("${namespace.name}.")
      append(name)
      if (isArray) append("[]")
      if (typeArguments.isNotEmpty()) {
        append("<")
        append(typeArguments.joinToString(", ") { it.toString() })
        append(">")
      }
      if (isNullable) {
        append(" | null | undefined")
      }
      append("'")


      if (classKind != null) append(" ($classKind)")

      if (sealedSubTypes.isNotEmpty()) {
        append(", sealedSubTypes=[")
        append(sealedSubTypes.joinToString { it.toString() })
        append("]")
      }

      append(", importedFrom=$importedFrom")

      append(")")
    }
  }

  override fun collectTypes(): Set<TsType> {
    return setOf(this)
  }

  /**
   * Returns the basic type of this type.
   * Removes the array and nullable information.
   */
  fun basicType(): TsType {
    return if (isArray || isNullable) {
      copy(isArray = false, isNullable = false)
    } else {
      this
    }
  }

  /**
   * Is the type a class, interface or abstract class?
   */
  enum class ClassKind {
    /**
     * Represents a primitive TypeScript type (e.g., string, number, boolean).
     */
    Primitive,

    /**
     * Represents a built-in TypeScript type (e.g., Array, Object).
     */
    BuildIn,

    /**
     * This is a class (might be abstract)
     */
    Class,

    /**
     * This is an interface
     */
    Interface,

    TypeParameter,
  }

  companion object {
    /**
     * Creates a "default" class type.
     */
    fun defaultClass(
      className: String,
      namespace: TsNamespace? = null,
      isNullable: Boolean = false,
      isArray: Boolean = false,
      typeArguments: List<TsType> = emptyList(),
      importedFrom: TsModuleReference,
    ): TsType {
      return TsType(
        name = className,
        namespace = namespace,
        isNullable = isNullable,
        isArray = isArray,
        classKind = ClassKind.Class,
        typeArguments = typeArguments,
        importedFrom = importedFrom,
      )
    }

    /**
     * Creates a "default" interface type.
     */
    fun defaultInterface(
      interfaceName: String,
      namespace: TsNamespace? = null,
      isNullable: Boolean = false,
      isArray: Boolean = false,
      typeArguments: List<TsType> = emptyList(),
      importedFrom: TsModuleReference,
    ): TsType {
      return TsType(
        name = interfaceName,
        namespace = namespace,
        isNullable = isNullable,
        isArray = isArray,
        classKind = ClassKind.Interface,
        typeArguments = typeArguments,
        importedFrom = importedFrom,
      )
    }

    /**
     * Creates a type parameter type.
     */
    fun typeParameter(
      parameterName: String,
      isNullable: Boolean = false,
      isArray: Boolean = false,
    ): TsType {
      return TsType(
        name = parameterName,
        isNullable = isNullable,
        isArray = isArray,
        classKind = ClassKind.TypeParameter,
        importedFrom = TsModuleReference.GenericType,
      )
    }

    val String: TsType = TsType("string", classKind = ClassKind.Primitive, importedFrom = TsModuleReference.NativeType)

    fun String(isNullable: Boolean = false, isArray: Boolean = false): TsType {
      return String.copy(isNullable = isNullable, isArray = isArray)
    }

    val Number: TsType = TsType("number", classKind = ClassKind.Primitive, importedFrom = TsModuleReference.NativeType)

    fun Number(isNullable: Boolean = false, isArray: Boolean = false): TsType {
      return Number.copy(isNullable = isNullable, isArray = isArray)
    }

    val Boolean: TsType = TsType("boolean", classKind = ClassKind.Primitive, importedFrom = TsModuleReference.NativeType)

    fun Boolean(isNullable: Boolean = false, isArray: Boolean = false): TsType {
      return Boolean.copy(isNullable = isNullable, isArray = isArray)
    }

    val Any: TsType = TsType("any", classKind = ClassKind.Primitive, importedFrom = TsModuleReference.NativeType)
    val AnyNullable: TsType = TsType("any", isNullable = true, classKind = ClassKind.Primitive, importedFrom = TsModuleReference.NativeType)

    fun Any(isNullable: Boolean = false, isArray: Boolean = false): TsType {
      return Any.copy(isNullable = isNullable, isArray = isArray)
    }

    val Void: TsType = TsType("void", classKind = ClassKind.Primitive, importedFrom = TsModuleReference.NativeType)
    val Never: TsType = TsType("never", classKind = ClassKind.Primitive, importedFrom = TsModuleReference.NativeType)

    val Array: TsType = TsType("Array", classKind = ClassKind.BuildIn, importedFrom = TsModuleReference.NativeType)
    val Object: TsType = TsType("Object", classKind = ClassKind.BuildIn, importedFrom = TsModuleReference.NativeType)
    val Function: TsType = TsType("Function", classKind = ClassKind.BuildIn, importedFrom = TsModuleReference.NativeType)


    @Deprecated("remove me!")
    val typeScriptPrimitives: Set<String> = setOf("any", "boolean", "string", "number", "Array", "null")
  }
}
