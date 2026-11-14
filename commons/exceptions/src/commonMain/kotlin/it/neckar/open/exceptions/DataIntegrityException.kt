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

import it.neckar.uuid.ValueClassUuid
import kotlin.reflect.KClass

/**
 * Persistent data violates a domain invariant: an entity references another entity that
 * does not exist, or one that is in a forbidden state (e.g. a Confirmed Book referencing
 * a Draft Author). Distinct from a domain error — the database is in a state the model
 * declares impossible. Maps to HTTP 500.
 *
 * Typed fields so a rename or cross-entity-id mixup is caught at compile time. Most
 * call sites use the DSL: [refTo] + [referencingFrom] + [ReferenceCheckScope].
 *
 * @param source                entity whose state holds the broken reference
 * @param relationship          name of the relationship (e.g. "author", "bookSelection.entries[].item")
 * @param target                missing or inconsistent referenced entity
 * @param violation             [Violation.Missing] or [Violation.InconsistentState]
 * @param expectedState         e.g. "Confirmed" — `null` for [Violation.Missing]
 * @param actualState           e.g. "Draft"     — `null` for [Violation.Missing]
 * @param tenantId              optional tenant identifier for multi-tenant log filtering
 */
class DataIntegrityException(
  val source: EntityRef,
  val relationship: String,
  val target: EntityRef,
  val violation: Violation,
  val expectedState: String? = null,
  val actualState: String? = null,
  val tenantId: ValueClassUuid? = null,
) : RuntimeException(buildMessage(source, relationship, target, violation, expectedState, actualState, tenantId)) {

  /** Type + ID pair, built ergonomically via the [refTo] infix. */
  data class EntityRef(val type: KClass<*>, val id: ValueClassUuid)

  enum class Violation {
    /** The referenced entity does not exist. */
    Missing,

    /** The referenced entity exists but is in a state the source's invariants forbid. */
    InconsistentState,
  }

  companion object {
    fun missingReference(
      source: EntityRef,
      relationship: String,
      target: EntityRef,
      tenantId: ValueClassUuid? = null,
    ) = DataIntegrityException(source, relationship, target, Violation.Missing, tenantId = tenantId)

    fun inconsistentReferenceState(
      source: EntityRef,
      relationship: String,
      target: EntityRef,
      expectedState: String,
      actualState: String,
      tenantId: ValueClassUuid? = null,
    ) = DataIntegrityException(source, relationship, target, Violation.InconsistentState, expectedState, actualState, tenantId)

    private fun buildMessage(
      source: EntityRef,
      relationship: String,
      target: EntityRef,
      violation: Violation,
      expectedState: String?,
      actualState: String?,
      tenantId: ValueClassUuid?,
    ): String = buildString {
      append("Data integrity violation: ")
      append("${source.type.simpleName}<${source.id}>.$relationship -> ")
      append("${target.type.simpleName}<${target.id}> ")
      when (violation) {
        Violation.Missing -> append("does not exist")
        Violation.InconsistentState -> {
          append("is in inconsistent state")
          if (expectedState != null || actualState != null) {
            append(" (expected=${expectedState ?: "?"}, actual=${actualState ?: "?"})")
          }
        }
      }
      if (tenantId != null) {
        append(" [tenant=$tenantId]")
      }
    }
  }
}

/** `Book::class refTo bookId` → [DataIntegrityException.EntityRef]. */
infix fun KClass<*>.refTo(id: ValueClassUuid): DataIntegrityException.EntityRef =
  DataIntegrityException.EntityRef(this, id)

/**
 * Binds the [source] entity (and optional [tenantId]) for a block of reference checks.
 * Inside [block], call [ReferenceCheckScope.missing] / [ReferenceCheckScope.inconsistentState]
 * to throw a [DataIntegrityException] without restating source/tenant every time.
 */
inline fun <T> referencingFrom(
  source: DataIntegrityException.EntityRef,
  tenantId: ValueClassUuid? = null,
  block: ReferenceCheckScope.() -> T,
): T = ReferenceCheckScope(source, tenantId).block()

class ReferenceCheckScope(
  private val source: DataIntegrityException.EntityRef,
  private val tenantId: ValueClassUuid? = null,
) {
  fun missing(
    relationship: String,
    target: DataIntegrityException.EntityRef,
  ): Nothing = throw DataIntegrityException.missingReference(source, relationship, target, tenantId)

  fun inconsistentState(
    relationship: String,
    target: DataIntegrityException.EntityRef,
    expected: String,
    actual: String,
  ): Nothing = throw DataIntegrityException.inconsistentReferenceState(source, relationship, target, expected, actual, tenantId)
}
