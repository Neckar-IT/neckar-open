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
package it.neckar.ksp.spec.harvest

import assertk.*
import assertk.assertions.*
import it.neckar.gradle.ksp.options.KspPluginOptions
import it.neckar.ksp.spec.harvest.model.HarvestedClassSpec
import it.neckar.ksp.spec.harvest.model.SpecHarvestResult
import it.neckar.ksp.spec.harvest.model.findArgument
import it.neckar.ksp.spec.harvest.model.findValueArgument
import it.neckar.ksp.spec.harvest.sample.CustomerPayload
import it.neckar.ksp.spec.harvest.sample.DeprecatedTokenHolder
import it.neckar.ksp.spec.harvest.sample.PartNumber
import it.neckar.ksp.spec.harvest.sample.SampleStatus
import it.neckar.ksp.spec.harvest.sample.Wrapper
import it.neckar.ksp.test.KspTestUtils
import it.neckar.open.resources.getResourceAsStreamSafe
import it.neckar.open.test.utils.SlowTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

@SlowTest
class SpecHarvestProcessorTest {
  val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
  }

  @Test
  fun `collects metadata for serializable classes`(@TempDir tempDir: Path) {
    val projectDir = tempDir.toFile()

    val sampleSource = File("src/test/kotlin/it/neckar/ksp/spec/harvest/sample/SerializableSamples.kt")
    assertThat(sampleSource).exists()

    val processorProvider = SpecHarvestProcessorProvider()

    KspTestUtils.compileSourceFiles(
      sourceFiles = listOf(sampleSource),
      symbolProcessorProvider = processorProvider,
      options = mapOf(
        KspPluginOptions.Common.ProjectDir to projectDir.absolutePath,
      ),
    )

    val classSpecs = processorProvider.context.result
    val result = SpecHarvestResult.create(classSpecs)
    assertThat(classSpecs).isNotNull()
    val customerPayloadClassSpec = result.getClassSpec(CustomerPayload::class.qualifiedName!!)

    assertThat(customerPayloadClassSpec.packageName).isEqualTo("it.neckar.ksp.spec.harvest.sample")
    assertThat(customerPayloadClassSpec.simpleName).isEqualTo("CustomerPayload")

    assertThat(customerPayloadClassSpec.type).isEqualTo("CLASS")
    assertThat(customerPayloadClassSpec.modifiers).contains("DATA")
    assertThat(customerPayloadClassSpec.documentation).isEqualTo("Represents a customer payload returned by the API.")
    assertThat(customerPayloadClassSpec.annotations.map { it.name }).containsAll(
      "kotlinx.serialization.Serializable",
      "kotlin.Deprecated",
    )

    val idProperty = customerPayloadClassSpec.properties.first { it.name == "id" }
    assertThat(idProperty.type).isEqualTo("kotlin.String")
    assertThat(idProperty.isNullable).isEqualTo(false)
    assertThat(idProperty.annotations.map { it.name }).contains("kotlinx.serialization.SerialName")
    assertThat(idProperty.documentation).isEqualTo("Unique customer identifier.")
    assertThat(idProperty.typeAnnotations).isEmpty()

    val ageProperty = customerPayloadClassSpec.properties.first { it.name == "age" }
    assertThat(ageProperty.type).isEqualTo("kotlin.Int?")
    assertThat(ageProperty.hasDefault).isTrue()
    assertThat(ageProperty.documentation).isEqualTo("Optional customer age when known.")
    assertThat(ageProperty.typeAnnotations.map { it.name }).contains("it.neckar.ksp.spec.harvest.sample.W")
    val wattAnnotation = ageProperty.typeAnnotations.first { it.name == "it.neckar.ksp.spec.harvest.sample.W" }

    assertThat(wattAnnotation.metaAnnotations.map { it.name }).containsAtLeast(
      "it.neckar.ksp.spec.harvest.sample.Name",
      "it.neckar.ksp.spec.harvest.sample.Symbol",
    )

    wattAnnotation.metaAnnotations.first { it.name == "it.neckar.ksp.spec.harvest.sample.Name" }.let { nameAnnotation ->
      assertThat(nameAnnotation.arguments["value"]).isEqualTo("watt")
    }

    assertThat(wattAnnotation.metaAnnotations.map { it.name }).containsAtLeast(
      "it.neckar.ksp.spec.harvest.sample.Name",
      "it.neckar.ksp.spec.harvest.sample.Symbol",
    )
    val nameAnnotation = wattAnnotation.metaAnnotations.first { it.name == "it.neckar.ksp.spec.harvest.sample.Name" }
    assertThat(nameAnnotation.arguments["value"]).isEqualTo("watt")
    val symbolAnnotation = wattAnnotation.metaAnnotations.first { it.name == "it.neckar.ksp.spec.harvest.sample.Symbol" }
    assertThat(symbolAnnotation.arguments["value"]).isEqualTo("W")
    assertThat(nameAnnotation.metaAnnotations.map { it.name }).contains("kotlin.annotation.Target")
    val targetMetaAnnotation = nameAnnotation.metaAnnotations.first { it.name == "kotlin.annotation.Target" }
    assertThat(targetMetaAnnotation.metaAnnotations).isEmpty()

    val tagsProperty = customerPayloadClassSpec.properties.first { it.name == "tags" }
    assertThat(tagsProperty.type).isEqualTo("kotlin.collections.List<kotlin.String>")
    assertThat(tagsProperty.hasDefault).isTrue()
    assertThat(tagsProperty.typeAnnotations).isEmpty()

    val wrapper = result.getClassSpec(Wrapper::class.qualifiedName!!)
    assertThat(wrapper.properties).hasSize(1)
    val payloadProperty = wrapper.properties.first()
    assertThat(payloadProperty.type).isEqualTo("it.neckar.ksp.spec.harvest.sample.CustomerPayload")
    assertThat(payloadProperty.typeAnnotations).isEmpty()


    val encodedSpecsJson = json.encodeToString<MutableList<HarvestedClassSpec>>(classSpecs)

    this::class.getResourceAsStreamSafe("/expected.json").use { expectedStream ->
      val expectedJson = expectedStream.reader().readText()
      assertThat(encodedSpecsJson.toPrettyJson()).isEqualTo(expectedJson.toPrettyJson())
    }
  }

  @Test
  fun `harvests JSON-Schema constraint annotation arguments off a value class`(@TempDir tempDir: Path) {
    val processorProvider = SpecHarvestProcessorProvider()

    KspTestUtils.compileSourceFiles(
      sourceFiles = listOf(File("src/test/kotlin/it/neckar/ksp/spec/harvest/sample/ConstraintSamples.kt")),
      symbolProcessorProvider = processorProvider,
      options = mapOf(
        KspPluginOptions.Common.ProjectDir to tempDir.toFile().absolutePath,
      ),
    )

    val result = SpecHarvestResult.create(processorProvider.context.result)
    val partNumber = result.getClassSpec(PartNumber::class.qualifiedName!!)

    // The wrapped `value` property carries the constraint annotations.
    val valueProperty = partNumber.properties.first()
    val annotations = valueProperty.annotations + valueProperty.typeAnnotations

    // Integer argument is rendered as its plain string, string argument keeps its raw value.
    assertThat(annotations.findValueArgument("MinLength")).isEqualTo("4")
    assertThat(annotations.findValueArgument("Pattern")).isEqualTo("[A-Z0-9]+")

    // Enum constants are harvested in declaration order, each carrying its KDoc and its annotations.
    val status = result.getClassSpec(SampleStatus::class.qualifiedName!!)

    // The compiler-synthesized `entries` property is not declared in the source and must not show up.
    assertThat(status.properties.map { it.name }).isEmpty()

    assertThat(status.enumEntries.map { it.name to it.documentation }).isEqualTo(
      listOf(
        "Active" to "The item is active.",
        "Archived" to "The item is archived.",
      )
    )
    assertThat(status.enumEntries.first().annotations.findValueArgument("GermanTerm")).isEqualTo("Aktiv")
    assertThat(status.enumEntries.last().annotations).isEmpty()

    // A property-level `@Deprecated` message (passed positionally) is harvested under its named `message` argument.
    val tokenHolder = result.getClassSpec(DeprecatedTokenHolder::class.qualifiedName!!)
    val legacyTokenProperty = tokenHolder.properties.first { it.name == "legacyToken" }
    assertThat(legacyTokenProperty.annotations.findArgument("Deprecated", "message")).isEqualTo("Use the OAuth2 flow instead")
  }
}

/**
 * Can not be moved to common modules - since we are not allowed to reference default modules from KSP projects
 */
private fun String.toPrettyJson(): String {
  val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
  }

  val jsonElement = json.parseToJsonElement(this)
  return json.encodeToString(jsonElement)
}
