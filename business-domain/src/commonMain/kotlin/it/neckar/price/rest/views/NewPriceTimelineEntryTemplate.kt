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
package it.neckar.price.rest.views

import it.neckar.financial.currency.Money
import it.neckar.open.annotations.DataView
import kotlinx.serialization.Serializable

/**
 * Starting values of a new price timeline entry, answered by
 * `POST /api/<parent>/{id}/price-timeline/new` and never persisted.
 *
 * Not a `NewEntityTemplate`: it carries no id, because an entry is addressed by its owner, and no
 * timing, because when the new entry takes effect is the caller's choice.
 */
@DataView("Starting values of a new price timeline entry — never persisted")
@Serializable
data class NewPriceTimelineEntryTemplate(
  /** The price of the entry in effect now, or `null` when no entry is in effect yet. */
  val price: Money.Net?,
)
