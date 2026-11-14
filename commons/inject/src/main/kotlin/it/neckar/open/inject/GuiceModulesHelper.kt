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
package it.neckar.open.inject

import com.google.inject.ConfigurationException
import com.google.inject.CreationException
import com.google.inject.Guice
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.ProvisionException
import com.google.inject.spi.Message
import java.util.ArrayList
import java.util.Collections
import javax.annotation.Nonnull

/**
 *
 * GuiceModulesHelper class.
 *
 */
object GuiceModulesHelper {
  /**
   *
   * minimize
   *
   * @param modules   a List object.
   * @param testTypes a Class object.
   * @return a GuiceModulesHelper.Result object.
   */
  @JvmStatic
  fun minimize(modules: List<Module>, vararg testTypes: Class<*>): Result {
    return minimize(modules, *convertToKeys(*testTypes))
  }

  /**
   * Converts the cs array to a keys array
   */
  @JvmStatic
  fun convertToKeys(vararg types: Class<*>): Array<Key<*>> {
    return types.map {
      Key.get(it)
    }.toTypedArray()
  }

  /**
   *
   * minimize
   *
   * @param modules a List object.
   * @param keys    a Key object.
   * @return a GuiceModulesHelper.Result object.
   */
  @JvmStatic
  fun minimize(modules: List<Module>, vararg keys: Key<*>): Result {
    //Verify to ensure it works with all modules
    verifyInjection(modules, *keys)
    return minimize(Result(modules), *keys)
  }

  /**
   *
   * minimize
   *
   * @param result   a GuiceModulesHelper.Result object.
   * @param testType a Class object.
   * @return a GuiceModulesHelper.Result object.
   */
  @JvmStatic
  fun minimize(result: Result, testType: Class<*>): Result {
    return minimize(result, Key.get(testType))
  }

  /**
   *
   * minimize
   *
   * @param result a GuiceModulesHelper.Result object.
   * @param keys   a Key object.
   * @return a GuiceModulesHelper.Result object.
   */
  @JvmStatic
  fun minimize(result: Result, vararg keys: Key<*>): Result {
    require(keys.isNotEmpty()) { "Need at least one key" }

    //Iterate over all types (copy because the result is updated)
    val modules: List<Module> = ArrayList(result.getTypes())
    for (current in modules) {
      val copy: MutableCollection<Module> = ArrayList(modules)
      copy.remove(current)

      if (!canProvide(copy, keys)) {
        continue //the current module is required: without it the keys cannot be provided
      }

      //Update the result
      result.remove(current)
      if (copy.isEmpty()) {
        result.removeAll()
        return result //fast exit
      }

      //Try to minimize further
      return minimize(result, *keys)
    }
    return result //no minimization
  }

  /**
   * Returns true if the modules provide instances for all keys.
   *
   * Returns false if they do not provide them - the only expected outcome while minimizing: it means the module
   * that has just been left out is required.
   *
   * A module that throws while it is being configured, or a provider that fails on its own, is a defect and not a
   * reason to keep a module. Those failures are rethrown instead of being reported as "cannot be minimized".
   */
  private fun canProvide(modules: Collection<Module>, keys: Array<out Key<*>>): Boolean {
    try {
      verifyInjection(modules, *keys)
    } catch (_: ConfigurationException) {
      //There is no binding for one of the keys
      return false
    } catch (e: CreationException) {
      //Guice wraps whatever a module throws in configure() into a CreationException, so the type alone does not tell
      //a missing binding from a broken module - the cause does.
      if (e.errorMessages.hasCause()) {
        throw e
      }
      return false
    } catch (e: ProvisionException) {
      if (e.errorMessages.hasCause()) {
        throw e
      }
      return false
    }

    return true
  }

  /**
   * Whether any of these Guice error messages was caused by an exception - as opposed to Guice itself reporting a
   * missing or ambiguous binding
   */
  private fun Collection<Message>.hasCause(): Boolean {
    return any { it.cause != null }
  }

  private fun verifyInjection(modules: Iterable<Module>, vararg keys: Key<*>) {
    require(keys.isNotEmpty()) { "Need at least one key" }
    val injector = Guice.createInjector(modules)

    keys.forEach {
      injector.getInstance(it)
    }
  }

  /**
   *
   * assertMinimizeNotPossible
   *
   * @param modules   a List object.
   * @param testTypes the Class types.
   * @throws AssertionError if any.
   */
  @JvmStatic
  @Throws(AssertionError::class)
  fun assertMinimizeNotPossible(modules: List<Module>, vararg testTypes: Class<*>) {
    assertMinimizeNotPossible(modules, *convertToKeys(*testTypes))
  }

  /**
   *
   * assertMinimizeNotPossible
   *
   * @param modules a List object.
   * @param keys    a Key object.
   * @throws AssertionError if any.
   */
  @JvmStatic
  @Throws(AssertionError::class)
  fun assertMinimizeNotPossible(modules: List<Module>, vararg keys: Key<*>) {
    val minimal = minimize(modules, *keys)
    if (minimal.getRemoved().isNotEmpty()) {
      throw AssertionError(
        """
  Can be minimized:
  Remove:
  ${minimal.removedClassNamesAsString}${minimal.asInstantiations()}
  """.trimIndent()
      )
    }
  }

  class Result(types: List<Module>) {
    private val types: MutableList<out Module>

    private val removed: MutableList<Module> = ArrayList()
    fun size(): Int {
      return types.size
    }

    fun removeAll() {
      removed.addAll(types)
      types.clear()
    }

    fun remove(@Nonnull toRemove: Module) {
      types.remove(toRemove)
      removed.add(toRemove)
    }

    fun getTypes(): List<Module> {
      return Collections.unmodifiableList(types)
    }

    fun getRemoved(): List<Module> {
      return Collections.unmodifiableList(removed)
    }

    val removedClassNamesAsString: String
      get() {
        return buildString {
          removed.forEach {
            append("- ")
            append(it.javaClass.name)
            append("\n")
          }
        }
      }

    fun asInstantiations(): String {
      val builder = StringBuilder()
      val iterator: Iterator<Module> = types.iterator()
      while (iterator.hasNext()) {
        val module = iterator.next()
        builder.append("new ").append(module.javaClass.name).append("()")
        if (iterator.hasNext()) {
          builder.append(", ")
        }
      }
      return builder.toString()
    }

    init {
      this.types = ArrayList(types)
    }
  }
}
