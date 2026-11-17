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
package it.neckar.rest.patch

import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.serializers.JsonInclusionStrategy
import it.neckar.open.serialization.roundTrip
import it.neckar.open.test.utils.isJsonEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * Unit tests for [RequiredPatchValue].
 *
 * [RequiredPatchValue] is designed for **non-nullable** fields where [PatchValue.Delete] is not valid.
 * It only supports [RequiredPatchValue.Update] and [RequiredPatchValue.Keep].
 */
class RequiredPatchValueTest {

  private val json = JsonInclusionStrategy.Default.json()

  @Serializable
  private data class ConfigPatchRequest(
    val name: RequiredPatchValue<String> = RequiredPatchValue.Keep,
    val maxRetries: RequiredPatchValue<Int> = RequiredPatchValue.Keep,
    val enabled: RequiredPatchValue<Boolean> = RequiredPatchValue.Keep,
  )

  // Serialization

  @Test
  fun `serialize Update value`() {
    val patch = ConfigPatchRequest(name = RequiredPatchValue.Update("NewConfig"))
    val serialized = json.encodeToString(patch)

    assertThat(serialized).isJsonEqualTo("""{"name":"NewConfig"}""")
  }

  @Test
  fun `encoding a Keep is refused, so no document is written that cannot be read back`() {
    val encodingDefaults = Json { encodeDefaults = true }

    assertFailure { encodingDefaults.encodeToString(ConfigPatchRequest()) }
      .messageContains("Cannot serialize RequiredPatchValue.Keep, which stands for an absent field")
  }

  @Test
  fun `serialize Keep value - field omitted`() {
    val patch = ConfigPatchRequest() // all Keep
    val serialized = json.encodeToString(patch)

    assertThat(serialized).isJsonEqualTo("""{}""")
  }

  @Test
  fun `serialize mixed values`() {
    val patch = ConfigPatchRequest(
      name = RequiredPatchValue.Update("UpdatedName"),
      maxRetries = RequiredPatchValue.Keep,
      enabled = RequiredPatchValue.Update(false),
    )
    val serialized = json.encodeToString(patch)

    assertThat(serialized).isJsonEqualTo("""{"name":"UpdatedName","enabled":false}""")
  }

  // Deserialization

  @Test
  fun `deserialize Update value`() {
    val serialized = """{"name":"TestName"}"""
    val patch = json.decodeFromString<ConfigPatchRequest>(serialized)

    assertThat(patch.name).isInstanceOf<RequiredPatchValue.Update<String>>()
    assertThat((patch.name as RequiredPatchValue.Update).value).isEqualTo("TestName")
    assertThat(patch.maxRetries).isEqualTo(RequiredPatchValue.Keep)
    assertThat(patch.enabled).isEqualTo(RequiredPatchValue.Keep)
  }

  @Test
  fun `deserialize absent field as Keep`() {
    val serialized = """{}"""
    val patch = json.decodeFromString<ConfigPatchRequest>(serialized)

    assertThat(patch.name).isEqualTo(RequiredPatchValue.Keep)
    assertThat(patch.maxRetries).isEqualTo(RequiredPatchValue.Keep)
    assertThat(patch.enabled).isEqualTo(RequiredPatchValue.Keep)
  }

  @Test
  fun `deserialize null throws exception`() {
    val serialized = """{"name":null}"""

    assertFailure {
      json.decodeFromString<ConfigPatchRequest>(serialized)
    }.isInstanceOf<SerializationException>().cause().isNotNull().isInstanceOf<IllegalArgumentException>().hasMessage("Cannot deserialize null for RequiredPatchValue. Use PatchValue for nullable fields.")
  }

  // JSON Roundtrip

  @Test
  fun `roundtrip - Update all fields`() {
    roundTrip(
      ConfigPatchRequest(
        name = RequiredPatchValue.Update("Config1"),
        maxRetries = RequiredPatchValue.Update(5),
        enabled = RequiredPatchValue.Update(true),
      ),
      inclusionStrategy = JsonInclusionStrategy.Default,
    ) {
      //language=JSON
      $$"""
        {
          "name": "Config1",
          "maxRetries": 5,
          "enabled": true
        }
      """.trimIndent()
    }
  }

  @Test
  fun `roundtrip - Keep all fields`() {
    roundTrip(
      ConfigPatchRequest(
        name = RequiredPatchValue.Keep,
        maxRetries = RequiredPatchValue.Keep,
        enabled = RequiredPatchValue.Keep,
      ),
      inclusionStrategy = JsonInclusionStrategy.Default,
    ) {
      //language=JSON
      $$"""
        {}
      """.trimIndent()
    }
  }

  @Test
  fun `roundtrip - mixed Update and Keep`() {
    roundTrip(
      ConfigPatchRequest(
        name = RequiredPatchValue.Update("UpdatedName"),
        maxRetries = RequiredPatchValue.Keep,
        enabled = RequiredPatchValue.Update(false),
      ),
      inclusionStrategy = JsonInclusionStrategy.Default,
    ) {
      //language=JSON
      $$"""
        {
          "name": "UpdatedName",
          "enabled": false
        }
      """.trimIndent()
    }
  }

  // applyTo

  @Test
  fun `applyTo with Update returns new value`() {
    val patch: RequiredPatchValue<String> = RequiredPatchValue.Update("new")

    assertThat(patch.getValueOr("old")).isEqualTo("new")
  }

  @Test
  fun `applyTo with Keep returns existing value`() {
    val patch: RequiredPatchValue<String> = RequiredPatchValue.Keep

    assertThat(patch.getValueOr("existing")).isEqualTo("existing")
  }

  @Test
  fun `applyTo always returns non-null`() {
    val update: RequiredPatchValue<String> = RequiredPatchValue.Update("value")
    val keep: RequiredPatchValue<String> = RequiredPatchValue.Keep

    // Both return non-null String (not String?)
    val result1: String = update.getValueOr("current")
    val result2: String = keep.getValueOr("current")

    assertThat(result1).isEqualTo("value")
    assertThat(result2).isEqualTo("current")
  }

  // wouldChange

  @Test
  fun `wouldChange - Update with different value returns true`() {
    assertThat(RequiredPatchValue.Update("new").wouldChange("old")).isTrue()
  }

  @Test
  fun `wouldChange - Update with same value returns false`() {
    assertThat(RequiredPatchValue.Update("same").wouldChange("same")).isFalse()
  }

  @Test
  fun `wouldChange - Keep always returns false`() {
    assertThat((RequiredPatchValue.Keep as RequiredPatchValue<String>).wouldChange("any")).isFalse()
  }

  // valueOrNull

  @Test
  fun `valueOrNull - Update returns value`() {
    val patch: RequiredPatchValue<String> = RequiredPatchValue.Update("test")
    assertThat(patch.valueOrNull()).isEqualTo("test")
  }

  @Test
  fun `valueOrNull - Keep returns null`() {
    val patch: RequiredPatchValue<String> = RequiredPatchValue.Keep
    assertThat(patch.valueOrNull()).isNull()
  }

  // isUpdate / isKeep

  @Test
  fun `isUpdate and isKeep properties`() {
    val update: RequiredPatchValue<String> = RequiredPatchValue.Update("value")
    val keep: RequiredPatchValue<String> = RequiredPatchValue.Keep

    assertThat(update.isUpdate).isTrue()
    assertThat(update.isKeep).isFalse()

    assertThat(keep.isUpdate).isFalse()
    assertThat(keep.isKeep).isTrue()
  }

  @Test
  fun `companion factory methods`() {
    assertThat(RequiredPatchValue.update("test")).isEqualTo(RequiredPatchValue.Update("test"))
    assertThat(RequiredPatchValue.keep<String>()).isEqualTo(RequiredPatchValue.Keep)
  }

  // Conversion

  @Test
  fun `toPatchValue - Update converts to PatchValue Update`() {
    val required: RequiredPatchValue<String> = RequiredPatchValue.Update("test")
    val patch: PatchValue<String> = required.toPatchValue()

    assertThat(patch).isInstanceOf<PatchValue.Update<String>>()
    assertThat((patch as PatchValue.Update).value).isEqualTo("test")
  }

  @Test
  fun `toPatchValue - Keep converts to PatchValue Keep`() {
    val required: RequiredPatchValue<String> = RequiredPatchValue.Keep
    val patch: PatchValue<String> = required.toPatchValue()

    assertThat(patch).isEqualTo(PatchValue.Keep)
  }
}
