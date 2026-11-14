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
 * Is used to collect the types that shall be created
 */
class CollectingContext {

  private val interfaces = mutableSetOf<TsInterface>()
  fun interfaces(): List<TsInterface> = interfaces.toList()
  fun addInterface(interfaceToCreate: TsInterface) {
    interfaces.add(interfaceToCreate)
  }

  private val abstractClasses = mutableSetOf<TsAbstractClass>()
  fun abstractClasses(): List<TsAbstractClass> = abstractClasses.toList()
  fun addAbstractClass(tsClass: TsAbstractClass) {
    abstractClasses.add(tsClass)
  }

  private val classes = mutableSetOf<TsClass>()
  fun classes(): List<TsClass> = classes.toList()
  fun addClass(tsClass: TsClass) {
    classes.add(tsClass)
  }

  private val topLevelFunctions = mutableSetOf<TsTopLevelFunction>()
  fun topLevelFunctions(): List<TsTopLevelFunction> = topLevelFunctions.toList()
  fun addTopLevelFunction(function: TsTopLevelFunction) {
    topLevelFunctions.add(function)
  }

  private val constObjects = mutableSetOf<TsConstObject>()
  fun constObjects(): List<TsConstObject> = constObjects.toList()
  fun addConstObject(namespace: TsConstObject) {
    constObjects.add(namespace)
  }

  private val enums = mutableSetOf<TsEnumClass>()
  fun enums(): List<TsEnumClass> = enums.toList()
  fun addEnum(enum: TsEnumClass) {
    enums.add(enum)
  }

  private val valueClasses = mutableSetOf<TsValueClass>()

  /**
   * Merge value classes with the same name into one
   */
  fun valueClasses(): List<TsValueClass> = valueClasses.toList()

  fun addValueClass(valueClass: TsValueClass) {
    valueClasses.add(valueClass)
  }

  /**
   * Returns all types that must be imported
   */
  fun collectTypesToImport(): Collection<TsType> {
    return collectTypes()
      .filter { it.importedFrom.requiresImport() }
      .toSet()
  }

  /**
   * Returns all types
   */
  fun collectTypes(): List<TsType> {
    return (interfaces().flatMap { it.collectTypes() } +
      abstractClasses().flatMap { it.collectTypes() } +
      classes().flatMap { it.collectTypes() } +
      valueClasses().flatMap { it.collectTypes() } +
      topLevelFunctions().flatMap { it.collectTypes() } +
      constObjects().flatMap { it.collectTypes() } +
      enums().flatMap { it.collectTypes() }
      )
  }

  /**
   * Returns only the top level elements
   */
  fun collectTopLevelElements(): List<TsCodeFragmentProvider> {
    return (
      topLevelFunctions() +
        interfaces().filterOnlyTopLevel() +
        abstractClasses().filterOnlyTopLevel() +
        classes().filterOnlyTopLevel() +
        valueClasses().filterOnlyTopLevel() +
        constObjects().filterOnlyTopLevel() +
        enums().filterOnlyTopLevel()
      )
  }

  /**
   * Returns all elements containing a namespace
   */
  fun collectElementsWithNamespace(): List<TsCodeFragmentProviderWithNamespace> {
    return (
      interfaces().filterOnlyNamespace() +
        abstractClasses().filterOnlyNamespace() +
        classes().filterOnlyNamespace() +
        valueClasses().filterOnlyNamespace() +
        constObjects().filterOnlyNamespace() +
        enums().filterOnlyNamespace()
      )
  }
}

fun <T : HasNamespace> Collection<T>.filterOnlyTopLevel(): List<T> {
  return filter { it.namespace == null }
}

fun <T : HasNamespace> Collection<T>.filterOnlyNamespace(): List<T> {
  return filter { it.namespace != null }
}
