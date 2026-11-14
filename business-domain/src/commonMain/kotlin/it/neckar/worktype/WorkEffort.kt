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
package it.neckar.worktype

import it.neckar.open.annotations.CrossLayer
import it.neckar.open.annotations.GermanTerm
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * One typed portion of estimated installation work: [hours] of a specific work type ([workType]).
 *
 * A catalog position's installation time is a list of these, so "8 h assembly" becomes
 * "2 h master electrician + 6 h journeyman + …" — each part costed against its work type's own
 * hourly rate, yielding its own contribution margin.
 *
 * [hours] is a [Duration] at this trade-neutral layer (the type ArticleConditions already uses for
 * time); the product presents it to the user as hours.
 */
@Serializable
@CrossLayer("WorkEffort — identical shape in Domain, Entity and REST")
@GermanTerm("Arbeitsaufwand")
data class WorkEffort(
  /** The work type these hours are booked against — resolved to a rate via the product's catalog. */
  @GermanTerm("Arbeitsart")
  val workType: WorkTypeId,

  /** Estimated working time of [workType]. Non-negative. */
  @GermanTerm("Stunden")
  val hours: Duration,
) {
  init {
    require(hours >= Duration.ZERO) { "hours must not be negative but was <$hours>" }
  }
}
