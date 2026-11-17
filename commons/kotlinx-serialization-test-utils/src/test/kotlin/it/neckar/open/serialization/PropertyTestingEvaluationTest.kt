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
package it.neckar.open.serialization

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

/**
 * Proof-of-concept for property-based testing with KoTest Property.
 *
 * Demonstrates how `kotest-property` integrates with JUnit 5 + AssertK
 * to test serialization round-trip invariants with generated inputs.
 *
 * See evaluation: [docs/research/property-testing-evaluation.md]
 */
class PropertyTestingEvaluationTest {

  private val json = Json {
    prettyPrint = false
  }

  @Serializable
  @JvmInline
  private value class Name(val value: String)

  @Serializable
  @JvmInline
  private value class ProductId(val value: Uuid)

  @Serializable
  private enum class Color {
    Red, Green, Blue, Yellow, Black, White,
  }

  @Serializable
  private data class Product(
    val id: ProductId,
    val name: Name,
    val price: Double,
    val color: Color,
  )

  private val arbName: Arb<Name> = Arb.string(1..200).map { Name(it) }

  private val arbProductId: Arb<ProductId> = Arb.uuid().map { ProductId(it.toKotlinUuid()) }

  private val arbProduct: Arb<Product> = arbitrary {
    Product(
      id = arbProductId.bind(),
      name = arbName.bind(),
      price = Arb.double().filter { it.isFinite() }.bind(),
      color = Arb.enum<Color>().bind(),
    )
  }

  @Test
  suspend fun `value class string serialization round-trip`() {
    checkAll(arbName) { original ->
      val serialized = json.encodeToString(Name.serializer(), original)
      val deserialized = json.decodeFromString(Name.serializer(), serialized)
      assertThat(deserialized).isEqualTo(original)
    }
  }

  @Test
  suspend fun `value class uuid serialization round-trip`() {
    checkAll(arbProductId) { original ->
      val serialized = json.encodeToString(ProductId.serializer(), original)
      val deserialized = json.decodeFromString(ProductId.serializer(), serialized)
      assertThat(deserialized).isEqualTo(original)
    }
  }

  @Test
  suspend fun `enum serialization round-trip`() {
    checkAll(Arb.enum<Color>()) { original ->
      val serialized = json.encodeToString(Color.serializer(), original)
      val deserialized = json.decodeFromString(Color.serializer(), serialized)
      assertThat(deserialized).isEqualTo(original)
    }
  }

  @Test
  suspend fun `data class serialization round-trip`() {
    checkAll(PropTestConfig(iterations = 500), arbProduct) { original ->
      val serialized = json.encodeToString(Product.serializer(), original)
      val deserialized = json.decodeFromString(Product.serializer(), serialized)
      assertThat(deserialized).isEqualTo(original)
    }
  }

  @Test
  suspend fun `serialization preserves string content`() {
    checkAll(Arb.string(0..500)) { input ->
      val name = Name(input)
      val serialized = json.encodeToString(Name.serializer(), name)
      val deserialized = json.decodeFromString(Name.serializer(), serialized)
      assertThat(deserialized.value).isEqualTo(input)
    }
  }

  @Test
  suspend fun `format-parse round-trip for composite key`() {
    val arbPrefix = Arb.string(1..20).filter { it.contains('-').not() }
    val arbId = Arb.int(0..999_999)

    checkAll(arbPrefix, arbId) { prefix, id ->
      val formatted = "$prefix-$id"
      val parts = formatted.split('-')
      assertThat(parts[0]).isEqualTo(prefix)
      assertThat(parts[1].toInt()).isEqualTo(id)
    }
  }
}
