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
import it.neckar.open.http.Url
import it.neckar.open.kotlin.serializers.JsonInclusionStrategy
import it.neckar.open.serialization.roundTrip
import it.neckar.rest.patch.PatchValue.Delete
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Realistic example demonstrating [PatchValue] and [RequiredPatchValue] usage for REST PATCH requests.
 *
 * This test simulates a typical scenario where:
 * 1. A user profile entity exists in the database
 * 2. A PATCH request is sent to update specific fields
 * 3. The server applies the patch to the existing entity
 *
 * Key concepts:
 * - [RequiredPatchValue]: For **non-nullable** fields - only [RequiredPatchValue.Update] and [RequiredPatchValue.Keep]
 * - [PatchValue]: For **nullable** fields - [PatchValue.Update], [PatchValue.Keep], and [PatchValue.Delete]
 *
 * Rule of thumb:
 * - Entity field is `String` → use `RequiredPatchValue<String>`
 * - Entity field is `String?` → use `PatchValue<String>`
 */
class PatchValueRealisticTest {

  // Domain model - the entity stored in the database

  /**
   * The actual entity stored in the database.
   * Note the mix of nullable and non-nullable fields.
   */
  private data class UserProfile(
    val id: Long,
    val username: String,               // Required, non-nullable
    val displayName: String,            // Required, non-nullable
    val email: String?,                 // Optional, nullable
    val age: Int?,                      // Optional, nullable
    val bio: String?,                   // Optional, nullable
    val avatarImage: Url.DataScheme?,   // Optional, nullable (complex type)
  )

  // PATCH request model - what the client sends

  /**
   * The PATCH request DTO demonstrating type-safe patch values.
   *
   * - Non-nullable entity fields use [RequiredPatchValue]: Only Update or Keep
   * - Nullable entity fields use [PatchValue]: Update, Keep, or Delete
   *
   * This provides **compile-time safety**: You cannot accidentally use [Delete]
   * on a non-nullable field!
   */
  @Serializable
  private data class UserProfilePatchRequest(
    // Non-nullable fields: Use RequiredPatchValue (no Delete possible!)
    val username: RequiredPatchValue<String> = RequiredPatchValue.Keep,
    val displayName: RequiredPatchValue<String> = RequiredPatchValue.Keep,

    // Nullable fields: Use PatchValue (Delete allowed)
    val email: PatchValue<String> = PatchValue.Keep,
    val age: PatchValue<Int> = PatchValue.Keep,
    val bio: PatchValue<String> = PatchValue.Keep,
    val avatarImage: PatchValue<Url.DataScheme> = PatchValue.Keep,
  )

  // Service layer - applying patches

  /**
   * Applies a [UserProfilePatchRequest] to an existing [UserProfile].
   * This simulates what a service layer would do.
   *
   * Note:
   * - [RequiredPatchValue.getValueOr] returns non-null (safe for non-nullable fields)
   * - [PatchValue.getValueOr] returns nullable (can become null via Delete)
   */
  private fun UserProfile.applyPatch(patch: UserProfilePatchRequest): UserProfile {
    return copy(
      // Non-nullable fields: getValueOr returns T (never null)
      username = patch.username.getValueOr(username),
      displayName = patch.displayName.getValueOr(displayName),

      // Nullable fields: getValueOr returns T? (can be null via Delete)
      email = patch.email.getValueOr(email),
      age = patch.age.getValueOr(age),
      bio = patch.bio.getValueOr(bio),
      avatarImage = patch.avatarImage.getValueOr(avatarImage),
    )
  }

  // Test Data

  private val sampleAvatar = Url.DataScheme("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
  private val newAvatar = Url.DataScheme("data:image/png;base64,AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==")

  private val existingUser = UserProfile(
    id = 42,
    username = "johndoe",
    displayName = "John Doe",
    email = "john@example.com",
    age = 30,
    bio = "Software developer",
    avatarImage = sampleAvatar,
  )

  private val json = JsonInclusionStrategy.Default.json()

  // Test cases - RequiredPatchValue (non-nullable fields)

  @Test
  fun `RequiredPatchValue - Update changes value`() {
    val patch = UserProfilePatchRequest(
      username = RequiredPatchValue.Update("new_username"),
      displayName = RequiredPatchValue.Update("New Name"),
    )

    val updated = existingUser.applyPatch(patch)

    assertThat(updated.username).isEqualTo("new_username")
    assertThat(updated.displayName).isEqualTo("New Name")
  }

  @Test
  fun `RequiredPatchValue - Keep preserves value`() {
    val patch = UserProfilePatchRequest(
      username = RequiredPatchValue.Keep,
      displayName = RequiredPatchValue.Keep,
    )

    val updated = existingUser.applyPatch(patch)

    assertThat(updated.username).isEqualTo(existingUser.username)
    assertThat(updated.displayName).isEqualTo(existingUser.displayName)
  }

  // Note: There is NO test for "RequiredPatchValue - Delete" because
  // Delete does not exist on RequiredPatchValue! This is compile-time enforced.

  // Test cases - PatchValue (nullable fields)

  @Test
  fun `PatchValue - Update changes value`() {
    val patch = UserProfilePatchRequest(
      email = PatchValue.Update("newemail@example.com"),
      age = PatchValue.Update(31),
    )

    val updated = existingUser.applyPatch(patch)

    assertThat(updated.email).isEqualTo("newemail@example.com")
    assertThat(updated.age).isEqualTo(31)
  }

  @Test
  fun `PatchValue - Keep preserves value`() {
    val patch = UserProfilePatchRequest(
      email = PatchValue.Keep,
      age = PatchValue.Keep,
    )

    val updated = existingUser.applyPatch(patch)

    assertThat(updated.email).isEqualTo(existingUser.email)
    assertThat(updated.age).isEqualTo(existingUser.age)
  }

  @Test
  fun `PatchValue - Delete sets to null`() {
    val patch = UserProfilePatchRequest(
      email = Delete,
      bio = Delete,
      avatarImage = Delete,
    )

    val updated = existingUser.applyPatch(patch)

    assertThat(updated.email).isNull()
    assertThat(updated.bio).isNull()
    assertThat(updated.avatarImage).isNull()
  }

  // Test cases - Mixed operations

  @Test
  fun `PATCH with all Keep - no changes`() {
    val patch = UserProfilePatchRequest() // All fields default to Keep

    val updated = existingUser.applyPatch(patch)

    assertThat(updated).isEqualTo(existingUser)

    // JSON representation: empty object
    val serialized = json.encodeToString(patch)
    assertThat(serialized).isEqualTo("{}")
  }

  @Test
  fun `PATCH with mixed operations - Update, Keep, Delete`() {
    val patch = UserProfilePatchRequest(
      username = RequiredPatchValue.Keep,           // Keep existing (non-nullable)
      displayName = RequiredPatchValue.Update("Jane Doe"),  // Update (non-nullable)
      email = Delete,                               // Remove (nullable)
      age = PatchValue.Update(25),                  // Update (nullable)
      bio = PatchValue.Keep,                        // Keep existing (nullable)
      avatarImage = PatchValue.Update(newAvatar),   // Update (nullable)
    )

    val updated = existingUser.applyPatch(patch)

    assertThat(updated.username).isEqualTo("johndoe")           // Kept
    assertThat(updated.displayName).isEqualTo("Jane Doe")       // Updated
    assertThat(updated.email).isNull()                          // Deleted
    assertThat(updated.age).isEqualTo(25)                       // Updated
    assertThat(updated.bio).isEqualTo("Software developer")     // Kept
    assertThat(updated.avatarImage).isEqualTo(newAvatar)        // Updated
  }

  @Test
  fun `PATCH on entity with null fields - Update adds value`() {
    val userWithNulls = existingUser.copy(
      email = null,
      bio = null,
      avatarImage = null,
    )

    val patch = UserProfilePatchRequest(
      email = PatchValue.Update("new@example.com"),
      bio = PatchValue.Update("New bio"),
      avatarImage = PatchValue.Update(sampleAvatar),
    )

    val updated = userWithNulls.applyPatch(patch)

    assertThat(updated.email).isEqualTo("new@example.com")
    assertThat(updated.bio).isEqualTo("New bio")
    assertThat(updated.avatarImage).isEqualTo(sampleAvatar)
  }

  // JSON Serialization

  @Test
  fun `JSON serialization - RequiredPatchValue Update`() {
    roundTrip(
      UserProfilePatchRequest(
        username = RequiredPatchValue.Update("newuser"),
        displayName = RequiredPatchValue.Update("New User"),
      ),
      inclusionStrategy = JsonInclusionStrategy.Default,
    ) {
      //language=JSON
      $$"""
        {
          "username": "newuser",
          "displayName": "New User"
        }
      """.trimIndent()
    }
  }

  @Test
  fun `JSON serialization - PatchValue Delete (explicit nulls)`() {
    roundTrip(
      UserProfilePatchRequest(
        email = Delete,
        age = Delete,
        bio = Delete,
        avatarImage = Delete,
      ),
      inclusionStrategy = JsonInclusionStrategy.Default,
    ) {
      //language=JSON
      $$"""
        {
          "email": null,
          "age": null,
          "bio": null,
          "avatarImage": null
        }
      """.trimIndent()
    }
  }

  @Test
  fun `JSON serialization - all Keep (empty object)`() {
    roundTrip(
      UserProfilePatchRequest(),
      inclusionStrategy = JsonInclusionStrategy.Default,
    ) {
      //language=JSON
      $$"""
        {}
      """.trimIndent()
    }
  }

  @Test
  fun `JSON serialization - mixed RequiredPatchValue and PatchValue`() {
    roundTrip(
      UserProfilePatchRequest(
        username = RequiredPatchValue.Update("updated_user"),  // RequiredPatchValue: value in JSON
        displayName = RequiredPatchValue.Keep,                 // RequiredPatchValue: omitted
        email = Delete,                                        // PatchValue: null in JSON
        age = PatchValue.Update(42),                           // PatchValue: value in JSON
        bio = PatchValue.Keep,                                 // PatchValue: omitted
        avatarImage = Delete,                                  // PatchValue: null in JSON
      ),
      inclusionStrategy = JsonInclusionStrategy.Default,
    ) {
      //language=JSON
      $$"""
        {
          "username": "updated_user",
          "email": null,
          "age": 42,
          "avatarImage": null
        }
      """.trimIndent()
    }
  }

  @Test
  fun `JSON serialization - complex type (Url DataScheme)`() {
    roundTrip(
      UserProfilePatchRequest(
        avatarImage = PatchValue.Update(sampleAvatar),
      ),
      inclusionStrategy = JsonInclusionStrategy.Default,
    ) {
      //language=JSON
      $$"""
        {
          "avatarImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        }
      """.trimIndent()
    }
  }

  // Conversion

  @Test
  fun `RequiredPatchValue can be converted to PatchValue`() {
    val required: RequiredPatchValue<String> = RequiredPatchValue.Update("test")
    val patch: PatchValue<String> = required.toPatchValue()

    assertThat(patch).isInstanceOf<PatchValue.Update<String>>()
    assertThat((patch as PatchValue.Update).value).isEqualTo("test")

    val keepRequired: RequiredPatchValue<String> = RequiredPatchValue.Keep
    val keepPatch: PatchValue<String> = keepRequired.toPatchValue()

    assertThat(keepPatch).isEqualTo(PatchValue.Keep)
  }

  @Test
  fun `PatchValue Update and Keep can be converted to RequiredPatchValue`() {
    val update: PatchValue<String> = PatchValue.Update("test")
    val required: RequiredPatchValue<String> = update.toRequiredOrThrow()

    assertThat(required).isInstanceOf<RequiredPatchValue.Update<String>>()

    val keep: PatchValue<String> = PatchValue.Keep
    val requiredKeep: RequiredPatchValue<String> = keep.toRequiredOrThrow()

    assertThat(requiredKeep).isEqualTo(RequiredPatchValue.Keep)
  }

  @Test
  fun `PatchValue Delete throws when converted to RequiredPatchValue`() {
    val delete: PatchValue<String> = Delete

    assertFailure {
      delete.toRequiredOrThrow()
    }.isInstanceOf<IllegalStateException>().hasMessage("Cannot convert Delete to RequiredPatchValue")
  }

  @Test
  fun `PatchValue Delete becomes Keep with toRequiredOrKeep`() {
    val delete: PatchValue<String> = Delete
    val required: RequiredPatchValue<String> = delete.toRequiredOrKeep()

    assertThat(required).isEqualTo(RequiredPatchValue.Keep)
  }

  // Helper Method

  @Test
  fun `RequiredPatchValue wouldChange - detects actual changes`() {
    // Update with different value -> would change
    assertThat(RequiredPatchValue.Update("new").wouldChange("old")).isTrue()

    // Update with same value -> would NOT change
    assertThat(RequiredPatchValue.Update("same").wouldChange("same")).isFalse()

    // Keep -> never changes
    assertThat((RequiredPatchValue.Keep as RequiredPatchValue<String>).wouldChange("any")).isFalse()
  }

  @Test
  fun `PatchValue wouldChange - detects actual changes including Delete`() {
    // Update with different value -> would change
    assertThat(PatchValue.Update("new").wouldChange("old")).isTrue()

    // Update with same value -> would NOT change
    assertThat(PatchValue.Update("same").wouldChange("same")).isFalse()

    // Keep -> never changes
    assertThat((PatchValue.Keep as PatchValue<String>).wouldChange("any")).isFalse()

    // Delete on non-null -> would change
    assertThat((Delete as PatchValue<String>).wouldChange("value")).isTrue()

    // Delete on null -> would NOT change
    assertThat((Delete as PatchValue<String>).wouldChange(null)).isFalse()
  }

  @Test
  fun `valueOrNull - extracts value from Update`() {
    // RequiredPatchValue
    assertThat(RequiredPatchValue.Update("test").valueOrNull()).isEqualTo("test")
    assertThat((RequiredPatchValue.Keep as RequiredPatchValue<String>).valueOrNull()).isNull()

    // PatchValue
    assertThat(PatchValue.Update("test").valueOrNull()).isEqualTo("test")
    assertThat((PatchValue.Keep as PatchValue<String>).valueOrNull()).isNull()
    assertThat((Delete as PatchValue<String>).valueOrNull()).isNull()
  }
}
