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
package it.neckar.open.exceptions

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import it.neckar.uuid.NativeUuid
import it.neckar.uuid.ValueClassUuid
import kotlin.jvm.JvmInline
import org.junit.jupiter.api.Test

class DataIntegrityExceptionTest {

  @Test
  fun `Missing builds expected message`() {
    val ex = DataIntegrityException.missingReference(
      source = SourceEntity::class refTo SourceId(uuidA),
      relationship = "child",
      target = TargetEntity::class refTo TargetId(uuidB),
    )

    assertThat(ex).hasMessage(
      "Data integrity violation: SourceEntity<$uuidA>.child -> TargetEntity<$uuidB> does not exist"
    )
    assertThat(ex.violation).isEqualTo(DataIntegrityException.Violation.Missing)
    assertThat(ex.expectedState).isNull()
    assertThat(ex.actualState).isNull()
    assertThat(ex.tenantId).isNull()
  }

  @Test
  fun `Missing with tenantId appends tenant suffix`() {
    val tenant = SourceId(uuidC)
    val ex = DataIntegrityException.missingReference(
      source = SourceEntity::class refTo SourceId(uuidA),
      relationship = "child",
      target = TargetEntity::class refTo TargetId(uuidB),
      tenantId = tenant,
    )

    assertThat(ex).hasMessage(
      "Data integrity violation: SourceEntity<$uuidA>.child -> TargetEntity<$uuidB> does not exist [tenant=$uuidC]"
    )
    assertThat(ex.tenantId).isEqualTo(tenant)
  }

  @Test
  fun `InconsistentState includes expected and actual in message`() {
    val ex = DataIntegrityException.inconsistentReferenceState(
      source = SourceEntity::class refTo SourceId(uuidA),
      relationship = "child",
      target = TargetEntity::class refTo TargetId(uuidB),
      expectedState = "Confirmed",
      actualState = "Draft",
    )

    assertThat(ex).hasMessage(
      "Data integrity violation: SourceEntity<$uuidA>.child -> TargetEntity<$uuidB> is in inconsistent state (expected=Confirmed, actual=Draft)"
    )
    assertThat(ex.violation).isEqualTo(DataIntegrityException.Violation.InconsistentState)
    assertThat(ex.expectedState).isEqualTo("Confirmed")
    assertThat(ex.actualState).isEqualTo("Draft")
  }

  @Test
  fun `refTo builds EntityRef`() {
    val id = SourceId(uuidA)
    val ref = SourceEntity::class refTo id

    assertThat(ref).isEqualTo(DataIntegrityException.EntityRef(SourceEntity::class, id))
  }

  @Test
  fun `referencingFrom missing throws DataIntegrityException with bound source and tenant`() {
    val tenant = SourceId(uuidC)
    assertFailure {
      referencingFrom(source = SourceEntity::class refTo SourceId(uuidA), tenantId = tenant) {
        missing("child", TargetEntity::class refTo TargetId(uuidB))
      }
    }.isInstanceOf(DataIntegrityException::class).given { ex ->
      assertThat(ex.violation).isEqualTo(DataIntegrityException.Violation.Missing)
      assertThat(ex.source.type).isEqualTo(SourceEntity::class)
      assertThat(ex.source.id).isEqualTo(SourceId(uuidA))
      assertThat(ex.target.type).isEqualTo(TargetEntity::class)
      assertThat(ex.target.id).isEqualTo(TargetId(uuidB))
      assertThat(ex.relationship).isEqualTo("child")
      assertThat(ex.tenantId).isEqualTo(tenant)
    }
  }

  @Test
  fun `referencingFrom inconsistentState throws with expected and actual`() {
    assertFailure {
      referencingFrom(source = SourceEntity::class refTo SourceId(uuidA)) {
        inconsistentState(
          relationship = "child",
          target = TargetEntity::class refTo TargetId(uuidB),
          expected = "Confirmed",
          actual = "Draft",
        )
      }
    }.isInstanceOf(DataIntegrityException::class).given { ex ->
      assertThat(ex.violation).isEqualTo(DataIntegrityException.Violation.InconsistentState)
      assertThat(ex.expectedState).isEqualTo("Confirmed")
      assertThat(ex.actualState).isEqualTo("Draft")
      assertThat(ex.tenantId).isNull()
    }
  }

  // Test fixtures — minimal types in the test package so the exception module stays free of
  // patterns-domain dependencies.
  private class SourceEntity
  private class TargetEntity

  @JvmInline
  private value class SourceId(override val uuid: NativeUuid) : ValueClassUuid {
    override fun toString(): String = uuid.toHexDashString()
  }

  @JvmInline
  private value class TargetId(override val uuid: NativeUuid) : ValueClassUuid {
    override fun toString(): String = uuid.toHexDashString()
  }

  companion object {
    private val uuidA = NativeUuid.parse("11111111-1111-1111-1111-111111111111")
    private val uuidB = NativeUuid.parse("22222222-2222-2222-2222-222222222222")
    private val uuidC = NativeUuid.parse("33333333-3333-3333-3333-333333333333")
  }
}
