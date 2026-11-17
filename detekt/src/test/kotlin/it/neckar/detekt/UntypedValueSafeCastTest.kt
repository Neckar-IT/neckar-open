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
package it.neckar.detekt

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class UntypedValueSafeCastTest {

  private val rule = UntypedValueSafeCast(Config.empty)

  @Test
  fun `flags a JSON element dropped from a list by mapNotNull`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.JsonObject
      fun sections(content: JsonObject, names: List<String>): List<JsonObject> {
        return names.mapNotNull { content[it] as? JsonObject }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("`as? JsonObject`")
  }

  @Test
  fun `flags a BSON field answered as absent on a wrong type`() {
    val findings = rule.lint(
      """
      import org.bson.BsonDocument
      import org.bson.BsonString
      fun BsonDocument.isConfirmable(): Boolean {
        val name: String = (get("name") as? BsonString)?.value ?: return false
        return name.isNotBlank()
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("`as? BsonString`")
  }

  @Test
  fun `flags a Document field read through a safe call`() {
    val findings = rule.lint(
      """
      import org.bson.Document
      import java.util.Date
      fun Document.readInstant(key: String): Long? {
        return (get(key) as? Date)?.time
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("`as? Date`")
  }

  @Test
  fun `flags a YAML node narrowed without a throw`() {
    val findings = rule.lint(
      """
      import com.charleskorn.kaml.Yaml
      import com.charleskorn.kaml.YamlMap
      import com.charleskorn.kaml.YamlScalar
      fun description(frontmatter: String): String? {
        return (Yaml.default.parseToYamlNode(frontmatter) as? YamlMap)?.get<YamlScalar>("description")?.content
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("`as? YamlMap`")
  }

  @Test
  fun `flags a list type by its simple name regardless of the type argument`() {
    val findings = rule.lint(
      """
      import org.bson.Document
      fun Document.readIds(key: String): List<String>? {
        return (get(key) as? List<String>)?.map { it }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("`as? List`")
  }

  @Test
  fun `flags a qualified and a nullable type name`() {
    val findings = rule.lint(
      """
      fun first(element: Any): Any? {
        val qualified = element as? kotlinx.serialization.json.JsonArray
        val nullable = element as? String?
        return qualified ?: nullable
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
  }

  @Test
  fun `accepts a cast followed by a throw`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.JsonElement
      import kotlinx.serialization.json.JsonObject
      fun JsonObject.parent(current: JsonElement, key: String): JsonObject {
        return current as? JsonObject ?: throw IllegalArgumentException("<" + key + "> holds no object")
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a cast chained through safe calls into an error call`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.JsonElement
      import kotlinx.serialization.json.JsonPrimitive
      fun version(declared: JsonElement): Int {
        return (declared as? JsonPrimitive)?.takeIf { it.isString.not() }?.content?.toIntOrNull()
          ?: error("version has to be a whole number")
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a fallback chain whose last elvis throws`() {
    val findings = rule.lint(
      """
      import org.bson.Document
      fun Document.name(): String = get("name") as? String ?: get("title") as? String ?: kotlin.error("<name> and <title> hold no string")
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a cast followed by fail and one inside checkNotNull`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.JsonArray
      import kotlinx.serialization.json.JsonElement
      import kotlinx.serialization.json.JsonObject
      fun keys(element: JsonElement): JsonArray = element as? JsonArray ?: fail("expected an array")
      fun schema(element: JsonElement): JsonObject = checkNotNull(element as? JsonObject) { "expected an object" }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a cast as the receiver of the requireNotNull extension`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.JsonElement
      import kotlinx.serialization.json.JsonObject
      fun schema(element: JsonElement): JsonObject = (element as? JsonObject).requireNotNull { "expected an object" }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags both casts of a chain ending in a default and a cast before a logger call named error`() {
    val findings = rule.lint(
      """
      import org.bson.Document
      fun Document.name(logger: Any): String {
        val defaulted: String = get("name") as? String ?: get("title") as? String ?: "unnamed"
        val logged: String = get("name") as? String ?: logger.error("no name")
        return defaulted + logged
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(3)
  }

  @Test
  fun `accepts a TODO, a parenthesized throw, a qualified test fail and the checkNotNull extension`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.JsonArray
      import kotlinx.serialization.json.JsonElement
      import kotlinx.serialization.json.JsonObject
      fun a(element: JsonElement): JsonObject = element as? JsonObject ?: TODO("objects only")
      fun b(element: JsonElement): JsonObject = element as? JsonObject ?: (throw IllegalArgumentException("expected an object"))
      fun c(element: JsonElement): JsonArray = element as? JsonArray ?: kotlin.test.fail("expected an array")
      fun d(element: JsonElement): JsonObject = (element as? JsonObject).checkNotNull { "expected an object" }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts an inner cast whose null flows through an outer cast into a throw`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.JsonElement
      import kotlinx.serialization.json.JsonObject
      import kotlinx.serialization.json.JsonPrimitive
      fun kind(element: JsonElement): JsonPrimitive = (element as? JsonObject)?.get("kind") as? JsonPrimitive ?: error("<kind> holds no primitive")
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags a cast whose not-null assertion sits behind a safe call`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.JsonElement
      import kotlinx.serialization.json.JsonObject
      fun schema(element: JsonElement): JsonObject? = (element as? JsonObject)?.requireNotNull { "expected an object" }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags a cast whose fallback value stands before the not-null assertion`() {
    val findings = rule.lint(
      """
      import org.bson.Document
      fun Document.name(): String = requireNotNull(get("name") as? String ?: "") { "never null" }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags a cast whose dot call turns the null into a value before the throw`() {
    val findings = rule.lint(
      """
      import org.bson.Document
      fun Document.name(): String = (get("name") as? String).orEmpty() ?: error("never null")
      fun Document.title(): String = get("name") as? String ?: (get("title") as? String).orEmpty() ?: error("never null")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(3)
  }

  @Test
  fun `flags a cast whose nested elvis ends in a default`() {
    val findings = rule.lint(
      """
      import org.bson.Document
      fun Document.name(): String = get("a") as? String ?: (get("b") as? CharSequence ?: "x") ?: error("never null")
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `accepts a chain whose intermediate step is a safe call on a cast`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.JsonObject
      import org.bson.Document
      fun Document.name(json: Any): String = get("a") as? String ?: (json as? JsonObject)?.get("x") as? String ?: error("<a> and <x> hold no string")
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a parenthesized elvis chain that ends in a throw`() {
    val findings = rule.lint(
      """
      import org.bson.Document
      fun Document.name(): String = get("a") as? String ?: (get("b") as? String ?: get("c") as? String) ?: error("<a>, <b> and <c> hold no string")
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a cast inside requireNotNull`() {
    val findings = rule.lint(
      """
      import org.bson.BsonDocument
      import org.bson.BsonValue
      fun wallbox(value: BsonValue): BsonDocument {
        return requireNotNull(value as? BsonDocument) { "wallbox holds no document" }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a cast to a type outside the untyped set`() {
    val findings = rule.lint(
      """
      sealed interface Roof { data class Confirmed(val name: String) : Roof }
      fun name(roof: Roof): String? = (roof as? Roof.Confirmed)?.name
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a cast the declaration suppresses with the rule name`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.JsonElement
      import kotlinx.serialization.json.JsonPrimitive
      // A part that is not a string is an image or a tool payload: the text of a message leaves it out.
      @Suppress("UntypedValueSafeCast")
      fun text(parts: List<JsonElement>): String = parts.mapNotNull { (it as? JsonPrimitive)?.content }.joinToString()
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores an unsafe cast and an instance check`() {
    val findings = rule.lint(
      """
      import kotlinx.serialization.json.JsonElement
      import kotlinx.serialization.json.JsonObject
      fun size(element: JsonElement): Int = if (element is JsonObject) (element as JsonObject).size else 0
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
