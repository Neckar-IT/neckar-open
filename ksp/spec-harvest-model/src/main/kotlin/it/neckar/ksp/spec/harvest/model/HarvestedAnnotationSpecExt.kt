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

/**
 * Returns true if this spec *is* `kotlin.annotation.MustBeDocumented`. Whether an annotation *carries* it is
 * [isMarkedAsMustBeDocumented].
 *
 * The unqualified name is accepted as well because [HarvestedAnnotationSpec.name] falls back to it whenever the
 * harvester could not resolve the qualified name.
 */
fun HarvestedAnnotationSpec.isMustBeDocumentedMetaAnnotation(): Boolean {
  val simpleName = name.substringAfterLast('.')
  return name == "kotlin.annotation.MustBeDocumented" || simpleName == "MustBeDocumented"
}

/**
 * Returns true if this annotation is marked as MustBeDocumented (via meta-annotation).
 *
 * Looks one level deep only, which is where the harvester puts an annotation's own `@MustBeDocumented`.
 * False for every spec harvested without meta-annotations - [HarvestedAnnotationSpec.metaAnnotations] is empty then.
 */
fun HarvestedAnnotationSpec.isMarkedAsMustBeDocumented(): Boolean {
  return metaAnnotations.any { it.isMustBeDocumentedMetaAnnotation() }
}

/**
 * Returns true if this annotation is a `@Name` annotation
 */
fun HarvestedAnnotationSpec.isNameAnnotation(): Boolean {
  return name.substringAfterLast('.') == "Name"
}

/**
 * Returns the named argument [argumentName] of this annotation (quotes trimmed), or null if absent.
 */
fun HarvestedAnnotationSpec.extractArgument(argumentName: String): String? {
  return arguments[argumentName]?.trim('"')
}

/**
 * Returns the value argument of this annotation, or null if not present
 */
fun HarvestedAnnotationSpec.extractValueArgument(): String? {
  return extractArgument("value")
}

/**
 * Finds the value of a Name annotation, if present.
 *
 * If this annotation is annotated with:
 * `@Name("SomeName")`
 * then "SomeName" is returned.
 */
fun HarvestedAnnotationSpec.findNameAnnotationValue(): String? {
  return metaAnnotations.firstNotNullOfOrNull { meta ->
    if (meta.isNameAnnotation()) {
      meta.extractValueArgument()
    } else {
      null
    }
  }
}

/**
 * Returns true if this annotation is a `@Unit` annotation (the meta-annotation marking unit annotations)
 */
fun HarvestedAnnotationSpec.isUnitAnnotation(): Boolean {
  return name == "it.neckar.open.unit.Unit" || name.substringAfterLast('.') == "Unit"
}

/**
 * Returns true if this annotation is marked as a unit annotation (via @Unit meta-annotation).
 *
 * Looks one level deep only: a unit annotation that inherits `@Unit` through another meta-annotation is not detected.
 */
fun HarvestedAnnotationSpec.isMarkedAsUnit(): Boolean {
  return metaAnnotations.any { it.isUnitAnnotation() }
}

/**
 * Returns true if this annotation is a `@Symbol` annotation
 */
fun HarvestedAnnotationSpec.isSymbolAnnotation(): Boolean {
  return name.substringAfterLast('.') == "Symbol"
}

/**
 * Finds the unit symbol from a @Symbol meta-annotation.
 *
 * If this annotation is annotated with:
 * `@Symbol("Wp")`
 * then "Wp" is returned.
 */
fun HarvestedAnnotationSpec.findUnitSymbol(): String? {
  return metaAnnotations.firstNotNullOfOrNull { meta ->
    if (meta.isSymbolAnnotation()) {
      meta.extractValueArgument()
    } else {
      null
    }
  }
}

/**
 * Returns true if this annotation is a `@GermanTerm` annotation
 */
fun HarvestedAnnotationSpec.isGermanTermAnnotation(): Boolean {
  return name.substringAfterLast('.') == "GermanTerm"
}

/**
 * Returns true if this annotation's simple (unqualified) name equals [simpleName].
 */
fun HarvestedAnnotationSpec.hasSimpleName(simpleName: String): Boolean {
  return name.substringAfterLast('.') == simpleName
}

/**
 * Returns the `value` argument of the first annotation in this list whose simple name equals
 * [simpleName], or null if none is present. Used to read schema-constraint annotations
 * (`@MinLength`, `@Pattern`, `@Minimum`, ...) off a value class' wrapped property.
 */
fun List<HarvestedAnnotationSpec>.findValueArgument(simpleName: String): String? {
  return findArgument(simpleName, "value")
}

/**
 * Returns true if this list contains an annotation whose simple name equals [simpleName]
 * (e.g. `@ReadOnly`, `@WriteOnly`).
 */
fun List<HarvestedAnnotationSpec>.hasAnnotation(simpleName: String): Boolean {
  return any { it.hasSimpleName(simpleName) }
}

/**
 * Returns the value of the named argument [argumentName] (quotes trimmed), or null if absent.
 * Used for annotations whose relevant argument is not called `value` - e.g. `@Deprecated(message = ...)`.
 */
fun List<HarvestedAnnotationSpec>.findArgument(simpleName: String, argumentName: String): String? {
  return firstOrNull { it.hasSimpleName(simpleName) }?.extractArgument(argumentName)
}
