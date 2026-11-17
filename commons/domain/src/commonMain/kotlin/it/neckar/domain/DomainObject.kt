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
package it.neckar.domain

/**
 * Marks the canonical domain representation of a business concept.
 *
 * A domain object is the central, infrastructure-agnostic model
 * that carries business rules and validation logic.
 * It is not tied to a specific persistence layer or transfer format.
 *
 * ## Layering
 *
 * ```
 * Frontend ←→ REST Response/DTO ←→ DomainObject ←→ Persistence Entity
 * ```
 *
 * **A [DomainObject] is never sent to the frontend directly.**
 * The REST layer defines its own response/DTO types that control
 * exactly which fields are exposed. The backend converts between
 * domain objects and REST DTOs explicitly.
 *
 * This separation ensures that:
 * - Internal domain structure does not leak into the public API
 * - Domain objects can hold references to other domain objects (e.g., `Author.Confirmed`)
 *   while REST responses use flat IDs or snapshots
 * - The API contract remains stable even when domain internals change
 */
interface DomainObject
