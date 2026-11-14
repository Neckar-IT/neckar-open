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
import it.neckar.open.serialization.roundTrip
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Tests for [Patchable] and [PatchableRequired].
 *
 * These classes serialize to explicit discriminated unions:
 * - Update: `{ "type": "Update", "value": ... }`
 * - Keep: `{ "type": "Keep" }`
 * - Delete: `{ "type": "Delete" }` (only for Patchable)
 */
class PatchableTest {

  @Serializable
  private data class UserPatch(
    val name: Patchable<String>,
    val age: Patchable<Int>,
  )

  @Serializable
  private data class UserPatchWithRequired(
    val username: PatchableRequired<String>,
    val avatar: Patchable<String>,
  )

  // --- Patchable Tests ---

  @Test
  fun `Patchable Update serializes with type discriminator`() {
    roundTrip(
      UserPatch(
        name = Patchable.Update("John"),
        age = Patchable.Keep,
      )
    ) {
      //language=JSON
      """
      {
        "name": { "type": "Update", "value": "John" },
        "age": { "type": "Keep" }
      }
      """.trimIndent()
    }
  }

  @Test
  fun `Patchable Keep serializes with type discriminator`() {
    roundTrip(
      UserPatch(
        name = Patchable.Keep,
        age = Patchable.Keep,
      )
    ) {
      //language=JSON
      """
      {
        "name": { "type": "Keep" },
        "age": { "type": "Keep" }
      }
      """.trimIndent()
    }
  }

  @Test
  fun `Patchable Delete serializes with type discriminator`() {
    roundTrip(
      UserPatch(
        name = Patchable.Delete,
        age = Patchable.Update(42),
      )
    ) {
      //language=JSON
      """
      {
        "name": { "type": "Delete" },
        "age": { "type": "Update", "value": 42 }
      }
      """.trimIndent()
    }
  }

  @Test
  fun `Patchable mixed values`() {
    roundTrip(
      UserPatch(
        name = Patchable.Update("Jane"),
        age = Patchable.Delete,
      )
    ) {
      //language=JSON
      """
      {
        "name": { "type": "Update", "value": "Jane" },
        "age": { "type": "Delete" }
      }
      """.trimIndent()
    }
  }

  @Test
  fun `Patchable getValueOr`() {
    assertThat(Patchable.Update("new").getValueOr("old")).isEqualTo("new")
    assertThat(Patchable.Keep.getValueOr("old")).isEqualTo("old")
    assertThat(Patchable.Delete.getValueOr("old")).isNull()
  }

  @Test
  fun `Patchable wouldChange`() {
    assertThat(Patchable.Update("new").wouldChange("old")).isTrue()
    assertThat(Patchable.Update("same").wouldChange("same")).isFalse()
    assertThat((Patchable.Keep as Patchable<String>).wouldChange("any")).isFalse()
    assertThat((Patchable.Delete as Patchable<String>).wouldChange("value")).isTrue()
    assertThat((Patchable.Delete as Patchable<String>).wouldChange(null)).isFalse()
  }

  @Test
  fun `Patchable conversion to implicit`() {
    assertThat(Patchable.Update("test").toImplicit()).isEqualTo(PatchValue.Update("test"))
    assertThat(Patchable.Keep.toImplicit()).isEqualTo(PatchValue.Keep)
    assertThat(Patchable.Delete.toImplicit()).isEqualTo(PatchValue.Delete)
  }

  @Test
  fun `PatchValue conversion to Patchable`() {
    assertThat(PatchValue.Update("test").toPatchable()).isEqualTo(Patchable.Update("test"))
    assertThat(PatchValue.Keep.toPatchable()).isEqualTo(Patchable.Keep)
    assertThat(PatchValue.Delete.toPatchable()).isEqualTo(Patchable.Delete)
  }

  // --- PatchableRequired Tests ---

  @Test
  fun `PatchableRequired Update serializes with type discriminator`() {
    roundTrip(
      UserPatchWithRequired(
        username = PatchableRequired.Update("john_doe"),
        avatar = Patchable.Keep,
      )
    ) {
      //language=JSON
      """
      {
        "username": { "type": "Update", "value": "john_doe" },
        "avatar": { "type": "Keep" }
      }
      """.trimIndent()
    }
  }

  @Test
  fun `PatchableRequired Keep serializes with type discriminator`() {
    roundTrip(
      UserPatchWithRequired(
        username = PatchableRequired.Keep,
        avatar = Patchable.Delete,
      )
    ) {
      //language=JSON
      """
      {
        "username": { "type": "Keep" },
        "avatar": { "type": "Delete" }
      }
      """.trimIndent()
    }
  }

  @Test
  fun `PatchableRequired getValueOr`() {
    assertThat(PatchableRequired.Update("new").getValueOr("old")).isEqualTo("new")
    assertThat(PatchableRequired.Keep.getValueOr("old")).isEqualTo("old")
  }

  @Test
  fun `PatchableRequired getValueOrNull`() {
    assertThat(PatchableRequired.Update("new").getValueOrNull("old")).isEqualTo("new")
    assertThat(PatchableRequired.Update("new").getValueOrNull(null)).isEqualTo("new")
    assertThat(PatchableRequired.Keep.getValueOrNull("old")).isEqualTo("old")
    assertThat(PatchableRequired.Keep.getValueOrNull(null)).isNull()
  }

  @Test
  fun `PatchableRequired conversion to implicit`() {
    assertThat(PatchableRequired.Update("test").toImplicit()).isEqualTo(RequiredPatchValue.Update("test"))
    assertThat(PatchableRequired.Keep.toImplicit()).isEqualTo(RequiredPatchValue.Keep)
  }

  @Test
  fun `RequiredPatchValue conversion to Patchable`() {
    assertThat(RequiredPatchValue.Update("test").toPatchableRequired()).isEqualTo(PatchableRequired.Update("test"))
    assertThat(RequiredPatchValue.Keep.toPatchableRequired()).isEqualTo(PatchableRequired.Keep)
  }

  @Test
  fun `PatchableRequired conversion to Patchable`() {
    assertThat(PatchableRequired.Update("test").toPatchable())
      .isEqualTo(Patchable.Update("test"))
    assertThat(PatchableRequired.Keep.toPatchable())
      .isEqualTo(Patchable.Keep)
  }

  @Test
  fun `Patchable conversion to PatchableRequired`() {
    assertThat(Patchable.Update("test").toPatchableRequiredOrThrow())
      .isEqualTo(PatchableRequired.Update("test"))
    assertThat(Patchable.Keep.toPatchableRequiredOrThrow())
      .isEqualTo(PatchableRequired.Keep)

    assertFailure { Patchable.Delete.toPatchableRequiredOrThrow() }
      .hasClass(IllegalStateException::class)
  }

  @Test
  fun `Patchable conversion to PatchableRequired with fallback`() {
    assertThat(Patchable.Update("test").toPatchableRequiredOrKeep())
      .isEqualTo(PatchableRequired.Update("test"))
    assertThat(Patchable.Keep.toPatchableRequiredOrKeep())
      .isEqualTo(PatchableRequired.Keep)
    assertThat(Patchable.Delete.toPatchableRequiredOrKeep())
      .isEqualTo(PatchableRequired.Keep)
  }

  // --- Comparison with implicit serialization ---

  @Test
  fun `compare explicit vs implicit serialization`() {
    // Implicit - compact JSON
    @Serializable
    data class ImplicitPatch(
      val name: PatchValue<String> = PatchValue.Keep,
    )

    // Explicit - verbose but clear JSON
    @Serializable
    data class ExplicitPatch(
      val name: Patchable<String>,
    )

    // Same semantic meaning, different JSON representation
    val implicitUpdate = ImplicitPatch(name = PatchValue.Update("John"))
    val explicitUpdate = ExplicitPatch(name = Patchable.Update("John"))

    // Implicit: {"name":"John"}
    roundTrip(implicitUpdate) {
      //language=JSON
      """{"name":"John"}""".trimIndent()
    }

    // Explicit: {"name":{"type":"Update","value":"John"}}
    roundTrip(explicitUpdate) {
      //language=JSON
      """{"name":{"type":"Update","value":"John"}}""".trimIndent()
    }
  }
}
