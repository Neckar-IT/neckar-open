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
import it.neckar.open.collections.fastForEach
import it.neckar.open.http.Url
import it.neckar.open.kotlin.serializers.JsonInclusionStrategy
import it.neckar.open.serialization.roundTrip
import it.neckar.open.test.utils.isJsonEqualTo
import it.neckar.rest.patch.PatchValue.Delete
import it.neckar.rest.patch.PatchValue.Keep
import it.neckar.rest.patch.PatchValue.Update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class PatchValueTest {
  private val json = JsonInclusionStrategy.Default.json()

  private val avatarDataUrl = Url.DataScheme("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")

  @Serializable
  private data class UserPatch(
    val name: PatchValue<String> = Keep,
    val age: PatchValue<Int> = Keep,
    val email: PatchValue<String> = Keep,
  )

  /**
   * Example PATCH request for updating a user profile with name and avatar image.
   */
  @Serializable
  private data class UserProfilePatchRequest(
    val displayName: PatchValue<String> = Keep,
    val avatarImage: PatchValue<Url.DataScheme> = Keep,
  )

  @Test
  fun `serialize Update value`() {
    val patch = UserPatch(name = Update("John"))
    val serialized = json.encodeToString(patch)

    assertThat(serialized).isJsonEqualTo("""{"name":"John"}""")
  }

  @Test
  fun `serialize Delete value`() {
    val patch = UserPatch(name = Delete)
    val serialized = json.encodeToString(patch)

    assertThat(serialized).isJsonEqualTo("""{"name":null}""")
  }

  @Test
  fun `serialize Keep value - field omitted`() {
    val patch = UserPatch() // all Keep
    val serialized = json.encodeToString(patch)

    assertThat(serialized).isJsonEqualTo("""{}""")
  }

  @Test
  fun `encoding a Keep is refused, so no reader takes it for a Delete`() {
    val encodingDefaults = Json { encodeDefaults = true }

    assertFailure { encodingDefaults.encodeToString(UserPatch()) }
      .messageContains("Cannot serialize PatchValue.Keep, which stands for an absent field")
  }

  @Test
  fun `serialize mixed values`() {
    val patch = UserPatch(
      name = Update("John"),
      age = Delete,
      // email = Keep (default)
    )
    val serialized = json.encodeToString(patch)

    assertThat(serialized).isJsonEqualTo(
      //language=JSON
      """{"name":"John","age":null}"""
    )
  }

  @Test
  fun `deserialize Update value`() {
    val patch = json.decodeFromString<UserPatch>("""{"name":"John"}""")

    assertThat(patch.name).isInstanceOf<Update<String>>()
    assertThat((patch.name as Update).value).isEqualTo("John")
    assertThat(patch.age).isEqualTo(Keep)
    assertThat(patch.email).isEqualTo(Keep)
  }

  @Test
  fun `deserialize Delete value`() {
    val serialized = """{"name":null}"""
    val patch = json.decodeFromString<UserPatch>(serialized)

    assertThat(patch.name).isEqualTo(Delete)
    assertThat(patch.age).isEqualTo(Keep)
  }

  @Test
  fun `deserialize absent field as Keep`() {
    val serialized = """{}"""
    val patch = json.decodeFromString<UserPatch>(serialized)

    assertThat(patch.name).isEqualTo(Keep)
    assertThat(patch.age).isEqualTo(Keep)
    assertThat(patch.email).isEqualTo(Keep)
  }

  @Test
  fun `getValueOr with Update`() {
    val patch: PatchValue<String> = Update("new")
    assertThat(patch.getValueOr("old")).isEqualTo("new")
    assertThat(patch.getValueOr(null)).isEqualTo("new")
  }

  @Test
  fun `getValueOr with Keep`() {
    val patch: PatchValue<String> = Keep
    assertThat(patch.getValueOr("old")).isEqualTo("old")
    assertThat(patch.getValueOr(null)).isNull()
  }

  @Test
  fun `getValueOr with Delete`() {
    val patch: PatchValue<String> = Delete
    assertThat(patch.getValueOr("old")).isNull()
    assertThat(patch.getValueOr(null)).isNull()
  }

  @Test
  fun `valueOrNull returns value for Update`() {
    val patch: PatchValue<String> = Update("value")
    assertThat(patch.valueOrNull()).isEqualTo("value")
  }

  @Test
  fun `valueOrNull returns null for Keep and Delete`() {
    assertThat((Keep as PatchValue<String>).valueOrNull()).isNull()
    assertThat((Delete as PatchValue<String>).valueOrNull()).isNull()
  }

  @Test
  fun `wouldChange detects changes correctly`() {
    assertThat(Update("new").wouldChange("old")).isTrue()
    assertThat(Update("same").wouldChange("same")).isFalse()
    assertThat((Keep as PatchValue<String>).wouldChange("any")).isFalse()
    assertThat((Delete as PatchValue<String>).wouldChange("value")).isTrue()
    assertThat((Delete as PatchValue<String>).wouldChange(null)).isFalse()
  }

  @Test
  fun `companion factory methods`() {
    assertThat(PatchValue.update("test")).isEqualTo(Update("test"))
    assertThat(PatchValue.keep<String>()).isEqualTo(Keep)
    assertThat(PatchValue.delete<String>()).isEqualTo(Delete)
    assertThat(PatchValue.fromNullable("value")).isEqualTo(Update("value"))
    assertThat(PatchValue.fromNullable<String>(null)).isEqualTo(Delete)
  }

  @Test
  fun `isUpdate, isKeep, isDelete properties`() {
    val update: PatchValue<String> = Update("value")
    val keep: PatchValue<String> = Keep
    val delete: PatchValue<String> = Delete

    assertThat(update.isUpdate).isTrue()
    assertThat(update.isKeep).isFalse()
    assertThat(update.isDelete).isFalse()

    assertThat(keep.isUpdate).isFalse()
    assertThat(keep.isKeep).isTrue()
    assertThat(keep.isDelete).isFalse()

    assertThat(delete.isUpdate).isFalse()
    assertThat(delete.isKeep).isFalse()
    assertThat(delete.isDelete).isTrue()
  }

  @Test
  fun `UserProfilePatchRequest - update both name and avatar`() {
    val avatarDataUrl = Url.DataScheme("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")

    val request = UserProfilePatchRequest(
      displayName = Update("John Doe"),
      avatarImage = Update(avatarDataUrl),
    )

    val serialized = json.encodeToString(request)
    assertThat(serialized).isJsonEqualTo("""{"displayName":"John Doe","avatarImage":"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="}""")

    // Deserialize and verify
    val deserialized = json.decodeFromString<UserProfilePatchRequest>(serialized)
    assertThat(deserialized.displayName).isInstanceOf<Update<String>>()
    assertThat((deserialized.displayName as Update).value).isEqualTo("John Doe")
    assertThat(deserialized.avatarImage).isInstanceOf<Update<Url.DataScheme>>()
    assertThat((deserialized.avatarImage as Update).value).isEqualTo(avatarDataUrl)
  }

  @Test
  fun `UserProfilePatchRequest - update name only, keep avatar`() {
    val request = UserProfilePatchRequest(
      displayName = Update("Jane Doe"),
      // avatarImage = Keep (default)
    )

    val serialized = json.encodeToString(request)
    assertThat(serialized).isJsonEqualTo("""{"displayName":"Jane Doe"}""")

    val deserialized = json.decodeFromString<UserProfilePatchRequest>(serialized)
    assertThat(deserialized.displayName).isEqualTo(Update("Jane Doe"))
    assertThat(deserialized.avatarImage).isEqualTo(Keep)
  }

  @Test
  fun `UserProfilePatchRequest - delete avatar, keep name`() {
    val request = UserProfilePatchRequest(
      // displayName = Keep (default)
      avatarImage = Delete,
    )

    val serialized = json.encodeToString(request)
    assertThat(serialized).isJsonEqualTo("""{"avatarImage":null}""")

    val deserialized = json.decodeFromString<UserProfilePatchRequest>(serialized)
    assertThat(deserialized.displayName).isEqualTo(Keep)
    assertThat(deserialized.avatarImage).isEqualTo(Delete)
  }

  @Test
  fun `UserProfilePatchRequest - keep both (empty request)`() {
    val request = UserProfilePatchRequest()

    val serialized = json.encodeToString(request)
    assertThat(serialized).isJsonEqualTo("""{}""")

    val deserialized = json.decodeFromString<UserProfilePatchRequest>(serialized)
    assertThat(deserialized.displayName).isEqualTo(Keep)
    assertThat(deserialized.avatarImage).isEqualTo(Keep)
  }

  @Test
  fun `UserProfilePatchRequest - JSON roundtrip`() {
    // Test all combinations
    val testCases = listOf(
      UserProfilePatchRequest(displayName = Update("John"), avatarImage = Update(avatarDataUrl)),
      UserProfilePatchRequest(displayName = Update("Jane"), avatarImage = Keep),
      UserProfilePatchRequest(displayName = Update("Bob"), avatarImage = Delete),
      UserProfilePatchRequest(displayName = Keep, avatarImage = Update(avatarDataUrl)),
      UserProfilePatchRequest(displayName = Keep, avatarImage = Keep),
      UserProfilePatchRequest(displayName = Keep, avatarImage = Delete),
      UserProfilePatchRequest(displayName = Delete, avatarImage = Update(avatarDataUrl)),
      UserProfilePatchRequest(displayName = Delete, avatarImage = Keep),
      UserProfilePatchRequest(displayName = Delete, avatarImage = Delete),
    )

    testCases.fastForEach { original ->
      val serialized = json.encodeToString(original)
      val deserialized = json.decodeFromString<UserProfilePatchRequest>(serialized)

      assertThat(deserialized).isEqualTo(original)
    }
  }

  @Test
  fun `testJsonRoundtrip - Update, Update`() {
    roundTrip(UserProfilePatchRequest(displayName = Update("John"), avatarImage = Update(avatarDataUrl)), inclusionStrategy = JsonInclusionStrategy.Default) {
      //language=JSON
      $$"""
        {
          "displayName" : "John",
          "avatarImage" : "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        }
      """.trimIndent()
    }
  }

  @Test
  fun `testJsonRoundtrip - Keep, Keep`() {
    roundTrip(UserProfilePatchRequest(displayName = Keep, avatarImage = Keep), inclusionStrategy = JsonInclusionStrategy.Default) {
      //language=JSON
      $$"""
        {}
      """.trimIndent()
    }
  }

  @Test
  fun `testJsonRoundtrip - Delete, Delete`() {
    roundTrip(UserProfilePatchRequest(displayName = Delete, avatarImage = Delete), inclusionStrategy = JsonInclusionStrategy.Default) {
      //language=JSON
      $$"""
        {
          "displayName" : null,
          "avatarImage" : null
        }
      """.trimIndent()
    }
  }

  @Test
  fun `testJsonRoundtrip - Update, Delete`() {
    roundTrip(UserProfilePatchRequest(displayName = Update("Jane"), avatarImage = Delete), inclusionStrategy = JsonInclusionStrategy.Default) {
      //language=JSON
      $$"""
        {
          "displayName" : "Jane",
          "avatarImage" : null
        }
      """.trimIndent()
    }
  }
}
