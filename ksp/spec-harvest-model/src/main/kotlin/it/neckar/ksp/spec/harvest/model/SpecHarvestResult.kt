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
package it.neckar.ksp.spec.harvest.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream
import kotlin.reflect.KClass

/**
 * Aggregated result.
 *
 * ATTENTION: This file is created by the aggregating Gradle task. *Not* by the KSP processor itself
 */
@Serializable
data class SpecHarvestResult(
  /**
   * The class specs, keyed by their fully qualified name.
   *
   * Only `@Serializable` Kotlin classes are harvested, so this is never a complete picture of a module's types.
   */
  val classSpecs: Map<String, HarvestedClassSpec>,
) {
  /**
   * Returns the class spec for the given qualified name, or throws if no spec was harvested for it - which is the
   * regular outcome for a class that is not `@Serializable` or whose module does not apply the spec-harvest processor.
   */
  fun getClassSpec(qualifiedName: String): HarvestedClassSpec {
    return classSpecs[qualifiedName] ?: error("Class spec for '$qualifiedName' not found (${classSpecs.size} class specs available).")
  }

  /**
   * Returns the class spec for the given KClass
   */
  inline fun <reified T : Any> getClassSpec(kClass: KClass<T> = T::class): HarvestedClassSpec {
    return getClassSpec(kClass.qualifiedName ?: error("The class ${kClass.simpleName} must have a qualified name"))
  }

  companion object {
    /**
     * Loads the result from the given input stream.
     *
     * Decodes with the strict default [Json]: a document that carries keys unknown to this model fails to load
     * rather than being read partially. Producer and consumer of the file therefore have to share this model version.
     */
    fun load(inputStream: InputStream): SpecHarvestResult {
      return Json.decodeFromStream<SpecHarvestResult>(inputStream)
    }

    /**
     * Convenience to build a result from in-memory specs, keyed by their qualified names.
     */
    fun create(classSpecs: List<HarvestedClassSpec>): SpecHarvestResult {
      return SpecHarvestResult(
        classSpecs.associateBy { it.qualifiedName }
      )
    }
  }
}
