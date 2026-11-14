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
package it.neckar.rest.openapi

import assertk.*
import assertk.assertions.*
import it.neckar.ksp.spec.harvest.model.HarvestedAnnotationSpec
import it.neckar.ksp.spec.harvest.model.HarvestedClassSpec
import it.neckar.ksp.spec.harvest.model.HarvestedPropertySpec
import it.neckar.ksp.spec.harvest.model.SpecHarvestResult
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider

class DocumentationProviderTest {
  @Test
  fun `extracts value class metadata with unit and german term`() {
    val classQualifiedName = ValueClassWithUnit::class.qualifiedName!!
    val classSpec = HarvestedClassSpec(
      packageName = classQualifiedName.substringBeforeLast('.', missingDelimiterValue = ""),
      qualifiedName = classQualifiedName,
      simpleName = "ValueClassWithUnit",
      type = "CLASS",
      modifiers = listOf("VALUE"),
      superTypes = emptyList(),
      documentation = null,
      annotations = listOf(
        // @GermanTerm("PV-Modulleistung")
        HarvestedAnnotationSpec(
          name = "it.neckar.open.annotations.GermanTerm",
          arguments = mapOf("value" to "\"PV-Modulleistung\""),
          metaAnnotations = emptyList(),
        ),
      ),
      typeParameters = emptyList(),
      properties = listOf(
        HarvestedPropertySpec(
          name = "wattPeak",
          type = "kotlin.Double",
          isNullable = false,
          isMutable = false,
          hasDefault = false,
          documentation = null,
          annotations = emptyList(),
          typeAnnotations = listOf(
            // @Wp annotation with @Unit, @Symbol("Wp"), @Name("watt-peak") meta-annotations
            HarvestedAnnotationSpec(
              name = "it.neckar.open.unit.other.Wp",
              arguments = emptyMap(),
              metaAnnotations = listOf(
                HarvestedAnnotationSpec(
                  name = "it.neckar.open.unit.Unit",
                  arguments = emptyMap(),
                  metaAnnotations = emptyList(),
                ),
                HarvestedAnnotationSpec(
                  name = "it.neckar.open.unit.Symbol",
                  arguments = mapOf("value" to "\"Wp\""),
                  metaAnnotations = emptyList(),
                ),
                HarvestedAnnotationSpec(
                  name = "it.neckar.open.unit.Name",
                  arguments = mapOf("value" to "\"watt-peak\""),
                  metaAnnotations = emptyList(),
                ),
              ),
            ),
          ),
        )
      )
    )

    val documentationProvider = DocumentationProvider.usingHarvestedSpecs(
      listOf(SpecHarvestResult.create(listOf(classSpec)))
    )

    val metadata = documentationProvider.getValueClassMetadata(ValueClassWithUnit::class)
    assertThat(metadata).isNotNull()
    assertThat(metadata!!.unitSymbol).isEqualTo("Wp")
    assertThat(metadata.unitName).isEqualTo("watt-peak")
    assertThat(metadata.germanTerm).isEqualTo("PV-Modulleistung")
  }

  @Test
  fun `returns null for value class without metadata`() {
    val classQualifiedName = ValueClassWithoutUnit::class.qualifiedName!!
    val classSpec = HarvestedClassSpec(
      packageName = classQualifiedName.substringBeforeLast('.', missingDelimiterValue = ""),
      qualifiedName = classQualifiedName,
      simpleName = "ValueClassWithoutUnit",
      type = "CLASS",
      modifiers = listOf("VALUE"),
      superTypes = emptyList(),
      documentation = null,
      annotations = emptyList(),
      typeParameters = emptyList(),
      properties = listOf(
        HarvestedPropertySpec(
          name = "value",
          type = "kotlin.String",
          isNullable = false,
          isMutable = false,
          hasDefault = false,
          documentation = null,
          annotations = emptyList(),
          typeAnnotations = emptyList(),
        )
      )
    )

    val documentationProvider = DocumentationProvider.usingHarvestedSpecs(
      listOf(SpecHarvestResult.create(listOf(classSpec)))
    )

    val metadata = documentationProvider.getValueClassMetadata(ValueClassWithoutUnit::class)
    assertThat(metadata).isNull()
  }

  @Test
  fun `appends must be documented annotations inline`() {
    val classQualifiedName = DocumentedType::class.qualifiedName!!
    val classSpec = HarvestedClassSpec(
      packageName = classQualifiedName.substringBeforeLast('.', missingDelimiterValue = ""),
      qualifiedName = classQualifiedName,
      simpleName = DocumentedType::class.simpleName ?: "DocumentedType",
      type = "CLASS",
      modifiers = emptyList(),
      superTypes = emptyList(),
      documentation = "Type doc",
      annotations = listOf(
        documentedAnnotation("com.example.DocumentedTypeAnnotation", friendly = "DocType"),
        undocumentedAnnotation("com.example.HiddenTypeAnnotation"),
      ),
      typeParameters = emptyList(),
      properties = listOf(
        HarvestedPropertySpec(
          name = "target",
          type = "kotlin.String",
          isNullable = false,
          isMutable = false,
          hasDefault = false,
          documentation = "Property doc",
          annotations = listOf(
            documentedAnnotation("com.example.DocumentedPropertyAnnotation", friendly = "PropFriendly"),
            undocumentedAnnotation("com.example.HiddenPropertyAnnotation"),
          ),
          typeAnnotations = listOf(
            documentedAnnotation("com.example.DocumentedTypeUsageAnnotation", friendly = "TypeFriendly"),
            undocumentedAnnotation("com.example.HiddenTypeUsageAnnotation"),
          ),
        )
      )
    )

    val documentationProvider = DocumentationProvider.usingHarvestedSpecs(
      listOf(SpecHarvestResult.create(listOf(classSpec)))
    )

    val typeDoc = documentationProvider.getDocumentationForType(DocumentedType::class)
    assertThat(typeDoc).isEqualTo("Type doc [DocType]")

    val propertyDoc = documentationProvider.getDocumentationForProperty(DocumentedType::class, "target")
    assertThat(propertyDoc).isEqualTo("Property doc [PropFriendly, TypeFriendly]")
  }
}

private fun documentedAnnotation(name: String, friendly: String? = null): HarvestedAnnotationSpec {
  val metaAnnotations = buildList {
    add(mustBeDocumentedMetaAnnotation())
    if (friendly != null) {
      add(nameAnnotation(friendly))
    }
  }
  return HarvestedAnnotationSpec(
    name = name,
    arguments = emptyMap(),
    metaAnnotations = metaAnnotations,
  )
}

private fun undocumentedAnnotation(name: String): HarvestedAnnotationSpec {
  return HarvestedAnnotationSpec(
    name = name,
    arguments = emptyMap(),
    metaAnnotations = emptyList(),
  )
}

private fun mustBeDocumentedMetaAnnotation(): HarvestedAnnotationSpec {
  return HarvestedAnnotationSpec(
    name = "kotlin.annotation.MustBeDocumented",
    arguments = emptyMap(),
    metaAnnotations = emptyList(),
  )
}

private fun nameAnnotation(value: String): HarvestedAnnotationSpec {
  return HarvestedAnnotationSpec(
    name = "it.neckar.open.unit.Name",
    arguments = mapOf("value" to value),
    metaAnnotations = emptyList(),
  )
}

private class DocumentedType(val target: String)

@JvmInline
private value class ValueClassWithUnit(val wattPeak: Double)

@JvmInline
private value class ValueClassWithoutUnit(val value: String)
