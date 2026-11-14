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
package it.neckar.open.test.utils

import assertk.*
import assertk.assertions.support.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.JacksonYAMLParseException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode
import java.io.StringWriter
import java.net.URL
import java.nio.charset.Charset
import javax.annotation.Nonnull

typealias YamlNode = JsonNode

/**
 * YAML Utils for comparing YAML strings or files.
 */
object YamlUtils {
  @JvmStatic
  @JvmOverloads
  fun assertYamlEquals(expected: URL, actual: String, charset: Charset = Charsets.UTF_8) {
    assertYamlEquals(AssertUtils.toString(expected, charset), actual)
  }


  @JvmStatic
  @JvmOverloads
  fun assertYamlEquals(
    expected: String?, actual: String?,
    actualTreeModifier: YamlNode.() -> Unit = {},
  ) {
    // Null/blank guard
    if (expected.isNullOrBlank() || actual.isNullOrBlank()) {
      val exp = formatYaml(expected ?: "").trim()
      val act = formatYaml(actual ?: "").trim()
      assertThat(exp).fail(exp, act)
    }

    try {
      // YAML parser
      val yamlMapper = ObjectMapper(
        YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
      )
      // JSON serializer for canonical comparison
      val jsonMapper = ObjectMapper()

      val expectedTree = yamlMapper.readTree(expected)
      val actualTree = yamlMapper.readTree(actual).also(actualTreeModifier)

      if (expectedTree != actualTree) {
        val expectedJson = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expectedTree)
        val actualJson   = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualTree)

        val result = JSONCompare.compareJSON(expectedJson, actualJson, JSONCompareMode.STRICT)

        if (result.failed()) {
          System.err.println("           Pointer               | Expected                                -- Actual")
          System.err.println("---------------------------------------------------------------------------------------------------")

          // field failures with values
          result.fieldFailures.forEach {
            System.err.println(
              "Failure: ${it.field.padEnd(23)} | ${it.expected?.toString()?.padEnd(40)} -- ${it.actual?.toString()?.padEnd(40)}"
            )
          }
          // missing fields (present in expected, absent in actual)
          result.fieldMissing.forEach { fieldComparisonFailure ->
            System.err.println("Missing: ${fieldComparisonFailure.field.padEnd(23)} | ${"present".padEnd(40)} -- ${"absent".padEnd(40)}")
          }
          // unexpected fields (absent in expected, present in actual)
          result.fieldUnexpected.forEach { fieldComparisonFailure ->
            System.err.println("Unexpected: ${fieldComparisonFailure.field.padEnd(20)} | ${"absent".padEnd(40)} -- ${"present".padEnd(40)}")
          }

          val expYaml = formatYaml(expected ?: "").trim()
          val actYaml = formatYaml(yamlMapper.writeValueAsString(actualTree)).trim()
          assertThat(expYaml).fail(expYaml, actYaml)
        }
      }
    } catch (e: JacksonYAMLParseException) {
      throw AssertionError("YAML parsing error (${e.message})\nActual:\n${formatYaml(actual ?: "").trim()}", e)
    } catch (e: Exception) {
      throw AssertionError("YAML processing failed: ${e.message}\nActual:\n${formatYaml(actual ?: "").trim()}", e)
    }
  }


  @JvmStatic
  @Nonnull
  fun formatYaml(yaml: String?): String {
    return try {
      val mapper = ObjectMapper(
        YAMLFactory()
          .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) // no '---'
      )
      val tree = mapper.readTree(yaml)
      val out = StringWriter()

      mapper.writerWithDefaultPrettyPrinter()
        .writeValue(out, tree)

      out.toString()
    } catch (_: Exception) {
      yaml.toString()
    }
  }
}

