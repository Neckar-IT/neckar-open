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
package it.neckar.rest.openapi.documentation
import it.neckar.ksp.spec.harvest.model.HarvestedAnnotationSpec
import it.neckar.ksp.spec.harvest.model.HarvestedClassSpec
import it.neckar.ksp.spec.harvest.model.SpecHarvestResult
import it.neckar.ksp.spec.harvest.model.extractValueArgument
import it.neckar.ksp.spec.harvest.model.findNameAnnotationValue
import it.neckar.ksp.spec.harvest.model.findUnitSymbol
import it.neckar.ksp.spec.harvest.model.isGermanTermAnnotation
import it.neckar.ksp.spec.harvest.model.isMarkedAsMustBeDocumented
import it.neckar.ksp.spec.harvest.model.isMarkedAsUnit
import it.neckar.ksp.spec.harvest.model.findValueArgument
import it.neckar.ksp.spec.harvest.model.hasAnnotation
import it.neckar.ksp.spec.harvest.model.findArgument
import it.neckar.open.annotations.ForcePolymorphic
import it.neckar.open.collections.fastForEach
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.kotlin.lang.joinStrings
import it.neckar.rest.openapi.spec.harvest.HarvestedSpecSupport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Contains metadata about a value class, extracted from harvested annotations.
 *
 * @param unitSymbol The unit symbol (e.g., "Wp", "kWh", "m²") - from @Symbol meta-annotation
 * @param unitName The unit name (e.g., "watt-peak", "kilowatt-hour") - from @Name meta-annotation
 * @param germanTerm The German technical term (e.g., "PV-Modulleistung") - from @GermanTerm annotation
 * @param minLength / maxLength / pattern JSON-Schema string constraints - from @MinLength / @MaxLength / @Pattern
 * @param minimum / maximum / multipleOf JSON-Schema numeric constraints - from @Minimum / @Maximum / @MultipleOf
 */
data class ValueClassMetadata(
  val unitSymbol: String?,
  val unitName: String?,
  val germanTerm: String?,
  val minLength: Int? = null,
  val maxLength: Int? = null,
  val pattern: String? = null,
  val minimum: Double? = null,
  val maximum: Double? = null,
  val multipleOf: Double? = null,
) {
  val hasAnyData: Boolean
    get() = unitSymbol != null || unitName != null || germanTerm != null ||
      minLength != null || maxLength != null || pattern != null ||
      minimum != null || maximum != null || multipleOf != null
}

/**
 * Callback that can be used to provide (additional) documentation for OpenAPI generation
 */
interface DocumentationProvider {
  /**
   * Returns the documentation for the given type, or null if no documentation is available
   */
  fun getDocumentationForType(clazz: KClass<*>): String?

  /**
   * Returns the documentation for the given property name, or null if no documentation is available
   */
  fun getDocumentationForProperty(clazz: KClass<*>, propertyName: String): String?

  /**
   * Returns the documentation for the given property, or null if no documentation is available
   */
  fun getDocumentationForProperty(clazz: KClass<*>, property: KProperty1<*, *>): String? {
    return None.getDocumentationForProperty(clazz, property.name)
  }

  /**
   * Returns the unit symbol for the given property, or null if the property has no unit annotation.
   * For example, returns "mo" for a property annotated with @Months.
   */
  fun findUnitForProperty(clazz: KClass<*>, propertyName: String): String? {
    return null
  }

  /**
   * Returns metadata for a value class, including unit symbol/name and German term.
   * Returns null if no metadata is available.
   */
  fun getValueClassMetadata(clazz: KClass<*>): ValueClassMetadata? {
    return null
  }

  /**
   * Returns the German term for the given class, or null if no @GermanTerm annotation is present.
   * This is used for all classes (not just value classes) to provide i18n information.
   */
  fun getGermanTermForClass(clazz: KClass<*>): String? {
    return null
  }

  /**
   * Returns the German term for the given property, or null if no @GermanTerm annotation is present.
   */
  fun getGermanTermForProperty(clazz: KClass<*>, propertyName: String): String? {
    return null
  }

  /**
   * Returns true if the given property is annotated with @ReadOnly (server-set, never sent by the client).
   */
  fun isReadOnlyProperty(clazz: KClass<*>, propertyName: String): Boolean {
    return false
  }

  /**
   * Returns true if the given property is annotated with @WriteOnly (client-sent, never returned by the server).
   */
  fun isWriteOnlyProperty(clazz: KClass<*>, propertyName: String): Boolean {
    return false
  }

  /**
   * Returns true if the given property is annotated with Kotlin's @Deprecated.
   */
  fun isDeprecatedProperty(clazz: KClass<*>, propertyName: String): Boolean {
    return false
  }

  /**
   * Returns the deprecation message of the given property's @Deprecated annotation, or null.
   */
  fun getDeprecatedReasonForProperty(clazz: KClass<*>, propertyName: String): String? {
    return null
  }

  /**
   * Returns the KDoc of the given enum constant, or null if none is available.
   */
  fun getDocumentationForEnumEntry(enumClass: KClass<*>, entryName: String): String? {
    return null
  }

  /**
   * Returns the German term of the given enum constant, or null if no @GermanTerm annotation is present.
   */
  fun getGermanTermForEnumEntry(enumClass: KClass<*>, entryName: String): String? {
    return null
  }

  object None : DocumentationProvider {
    override fun getDocumentationForType(clazz: KClass<*>): String? {
      return null
    }

    override fun getDocumentationForProperty(clazz: KClass<*>, propertyName: String): String? {
      return null
    }

    override fun getValueClassMetadata(clazz: KClass<*>): ValueClassMetadata? {
      return null
    }
  }

  companion object {
    /**
     * Creates a new instance that uses harvested specs
     */
    fun usingHarvestedSpecs(specHarvestResults: List<SpecHarvestResult> = HarvestedSpecSupport.loadProductionSpecs()): DocumentationProvider {
      val merged: Map<String, HarvestedClassSpec> = buildMap {
        specHarvestResults.fastForEach {
          putAll(it.classSpecs)
        }
      }

      return object : DocumentationProvider {
        override fun getDocumentationForType(clazz: KClass<*>): String? {
          val classSpec = merged[clazz.qualifiedName] ?: return null
          return joinStrings(classSpec.documentation, createAnnotationsDoc(classSpec.annotations))
        }

        override fun getDocumentationForProperty(clazz: KClass<*>, propertyName: String): String? {
          val classSpec = merged[clazz.qualifiedName] ?: return null
          val propertySpec = classSpec.findPropertySpec(propertyName) ?: return null

          return joinStrings(propertySpec.documentation, createAnnotationsDoc(propertySpec.annotations + propertySpec.typeAnnotations))
        }

        override fun findUnitForProperty(clazz: KClass<*>, propertyName: String): String? {
          val classSpec = merged[clazz.qualifiedName] ?: return null
          val propertySpec = classSpec.findPropertySpec(propertyName) ?: return null

          // Find the first unit annotation (from property annotations or type annotations)
          val unitAnnotation = (propertySpec.annotations + propertySpec.typeAnnotations)
            .firstOrNull { it.isMarkedAsUnit() }
            ?: return null

          // Return the symbol if available, otherwise the annotation's simple name
          return unitAnnotation.findUnitSymbol()
            ?: unitAnnotation.findNameAnnotationValue()
            ?: unitAnnotation.name.substringAfterLast('.')
        }

        override fun getValueClassMetadata(clazz: KClass<*>): ValueClassMetadata? {
          val classSpec = merged[clazz.qualifiedName] ?: return null

          // For value classes, we look at the first property (the wrapped value)
          val valueProperty = classSpec.properties.firstOrNull() ?: return null

          // Extract unit info from type annotations on the value property
          val unitAnnotation = valueProperty.typeAnnotations.firstOrNull { it.isMarkedAsUnit() }
          val unitSymbol = unitAnnotation?.findUnitSymbol()
          val unitName = unitAnnotation?.findNameAnnotationValue()

          // Extract German term from class-level annotation
          val germanTermAnnotation = classSpec.annotations.firstOrNull { it.isGermanTermAnnotation() }
          val germanTerm = germanTermAnnotation?.extractValueArgument()

          // Extract JSON-Schema constraints from the value property's annotations
          val constraintAnnotations = valueProperty.annotations + valueProperty.typeAnnotations

          val metadata = ValueClassMetadata(
            unitSymbol = unitSymbol,
            unitName = unitName,
            germanTerm = germanTerm,
            minLength = constraintAnnotations.findValueArgument("MinLength")?.toIntOrNull(),
            maxLength = constraintAnnotations.findValueArgument("MaxLength")?.toIntOrNull(),
            pattern = constraintAnnotations.findValueArgument("Pattern"),
            minimum = constraintAnnotations.findValueArgument("Minimum")?.toDoubleOrNull(),
            maximum = constraintAnnotations.findValueArgument("Maximum")?.toDoubleOrNull(),
            multipleOf = constraintAnnotations.findValueArgument("MultipleOf")?.toDoubleOrNull(),
          )

          return if (metadata.hasAnyData) metadata else null
        }

        override fun getGermanTermForClass(clazz: KClass<*>): String? {
          val classSpec = merged[clazz.qualifiedName] ?: return null
          val germanTermAnnotation = classSpec.annotations.firstOrNull { it.isGermanTermAnnotation() }
          return germanTermAnnotation?.extractValueArgument()
        }

        override fun getGermanTermForProperty(clazz: KClass<*>, propertyName: String): String? {
          val classSpec = merged[clazz.qualifiedName] ?: return null
          val propertySpec = classSpec.findPropertySpec(propertyName) ?: return null

          // Check property annotations and type annotations for @GermanTerm
          val germanTermAnnotation = (propertySpec.annotations + propertySpec.typeAnnotations)
            .firstOrNull { it.isGermanTermAnnotation() }
          return germanTermAnnotation?.extractValueArgument()
        }

        override fun isReadOnlyProperty(clazz: KClass<*>, propertyName: String): Boolean {
          val propertySpec = merged[clazz.qualifiedName]?.findPropertySpec(propertyName) ?: return false
          return (propertySpec.annotations + propertySpec.typeAnnotations).hasAnnotation("ReadOnly")
        }

        override fun isWriteOnlyProperty(clazz: KClass<*>, propertyName: String): Boolean {
          val propertySpec = merged[clazz.qualifiedName]?.findPropertySpec(propertyName) ?: return false
          return (propertySpec.annotations + propertySpec.typeAnnotations).hasAnnotation("WriteOnly")
        }

        override fun isDeprecatedProperty(clazz: KClass<*>, propertyName: String): Boolean {
          val propertySpec = merged[clazz.qualifiedName]?.findPropertySpec(propertyName) ?: return false
          return propertySpec.annotations.hasAnnotation("Deprecated")
        }

        override fun getDeprecatedReasonForProperty(clazz: KClass<*>, propertyName: String): String? {
          val propertySpec = merged[clazz.qualifiedName]?.findPropertySpec(propertyName) ?: return null
          return propertySpec.annotations.findArgument("Deprecated", "message")
        }

        override fun getDocumentationForEnumEntry(enumClass: KClass<*>, entryName: String): String? {
          return merged[enumClass.qualifiedName]?.findEnumEntrySpec(entryName)?.documentation
        }

        override fun getGermanTermForEnumEntry(enumClass: KClass<*>, entryName: String): String? {
          val enumEntrySpec = merged[enumClass.qualifiedName]?.findEnumEntrySpec(entryName) ?: return null
          return enumEntrySpec.annotations.firstOrNull { it.isGermanTermAnnotation() }?.extractValueArgument()
        }

        /**
         * Skip these annotations - they are not relevant for OpenAPI
         */
        private val annotationsToSkip: Set<String> = setOf(
          Serializable::class.qualifiedName.requireNotNull { "Serializable has no qualified name" },
          SerialName::class.qualifiedName.requireNotNull { "SerialName has no qualified name" },
          ForcePolymorphic::class.qualifiedName.requireNotNull { "ForcePolymorphic has no qualified name" },
        )

        /**
         * Creates documentation based on the annotations
         */
        private fun createAnnotationsDoc(annotations: List<HarvestedAnnotationSpec>): String? {
          val labels = annotations
            .asSequence()
            .filter { annotationsToSkip.contains(it.name).not() }
            .filter { it.isMarkedAsMustBeDocumented() } //only those marked as @MustBeDocumented
            .map { it.documentationLabel() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

          if (labels.isEmpty()) {
            return null
          }

          return labels.joinToString(prefix = "[", postfix = "]", separator = ", ")
        }

        /**
         * Extracts the documentation label for the annotation.
         * Uses the @Name or value argument if present, otherwise uses the simple name.
         */
        private fun HarvestedAnnotationSpec.documentationLabel(): String {
          return findFriendlyLabel() ?: name.substringAfterLast('.')
        }

        /**
         * Finds a friendly label for the annotation
         *
         * This could be:
         * * A direct @Name annotation's value
         * * The value argument of this annotation
         */
        private fun HarvestedAnnotationSpec.findFriendlyLabel(): String? {
          val directFriendly = findNameAnnotationValue()

          if (directFriendly != null) {
            return directFriendly
          }

          val annotationName = name.substringAfterLast('.')
          val annotationValue = arguments["value"] ?: return annotationName

          return "$annotationName(${annotationValue.trim('"')})"
        }
      }
    }
  }
}
