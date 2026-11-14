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
package it.neckar.rest.draft

/**
 * Marks elements that carry a [ConfirmationStatus] property.
 *
 * Use this for read-only carriers of the status — e.g. REST view DTOs that
 * mirror the status of a [Confirmable] domain object without being confirmable
 * themselves. Domain objects that *can be* confirmed implement [Confirmable],
 * which extends this interface.
 */
interface HasConfirmationStatus {
  /**
   * The confirmation status of this element.
   */
  val confirmationStatus: ConfirmationStatus
}

/**
 * Asserts that this carrier reports [ConfirmationStatus.Confirmed]. Used for invariants on
 * persisted views that must only embed confirmed entities — e.g. line items inside a
 * finalized book order can only reference Confirmed books.
 *
 * Distinct from the per-domain `Book?.requireConfirmed()` / `Author?.requireConfirmed()`
 * extensions: those narrow a nullable domain object to its `Confirmed` subtype via Kotlin
 * contracts. This one only checks the flag — view DTOs are flat data classes and have no
 * narrower subtype to land on.
 */
fun HasConfirmationStatus.requireConfirmed(
  lazyMessage: () -> Any = { "Expected ConfirmationStatus.Confirmed but was $confirmationStatus" },
) {
  require(confirmationStatus == ConfirmationStatus.Confirmed, lazyMessage)
}

/**
 * Asserts that this carrier reports [ConfirmationStatus.Draft]. Mirror of [requireConfirmed]
 * for invariants on views that must only embed draft entities — e.g. a draft-only sub-form
 * that should not accept already-confirmed children.
 */
fun HasConfirmationStatus.requireDraft(
  lazyMessage: () -> Any = { "Expected ConfirmationStatus.Draft but was $confirmationStatus" },
) {
  require(confirmationStatus == ConfirmationStatus.Draft, lazyMessage)
}
