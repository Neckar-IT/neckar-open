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
package it.neckar.timeline

import it.neckar.open.annotations.GermanTerm
import it.neckar.open.kotlin.serializers.NativeInstant
import it.neckar.open.unit.other.Inclusive
import it.neckar.open.unit.other.Order
import it.neckar.open.unit.other.Sorted
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * An immutable timeline of a single value of type [T] that changes over time — the
 * value-timeline pattern. Each entry declares "from [Entry.validFrom] onwards, the value
 * is [Entry.value]" until the next entry takes over.
 *
 * Used directly with a domain value type, typically behind a domain typealias:
 * `typealias PriceTimeline = ValueTimeline<Money.Net>`.
 *
 * See `internal/patterns/docs/kotlin/value-timeline.md`.
 */
@GermanTerm("Wert-Zeitachse")
@Serializable
data class ValueTimeline<T>(
  val entries: @Sorted(by = "validFrom", value = Order.DESC) List<Entry<T>>,
) {

  init {
    //ensure the entries are sorted correctly
    for (i in 1 until entries.size) {
      val lastEntry = entries[i - 1]
      val currentEntry = entries[i]

      require(lastEntry.validFrom != currentEntry.validFrom) {
        "Entries have the same validFrom date: ${lastEntry.validFrom}"
      }

      require(lastEntry.validFrom > currentEntry.validFrom) {
        "Entries are not sorted by validFrom descending: ${lastEntry.validFrom} < ${currentEntry.validFrom}"
      }
    }
  }

  /**
   * Adds the given entry and returns a new timeline. A duplicate `validFrom` fails the
   * constructor validation — use [withAdded] for the explicit Add-Forward error message.
   */
  operator fun plus(entry: Entry<T>): ValueTimeline<T> {
    return ValueTimeline((entries + entry).sortedByDescending { it.validFrom })
  }

  /**
   * Returns the entry valid for the given date (latest entry with `validFrom <= date`),
   * or null if no such entry exists.
   */
  fun entryAt(date: Instant): Entry<T>? {
    return entries.find { it.validFrom <= date }
  }

  /**
   * Returns the value valid for the given date. Throws if no entry is valid at [date].
   */
  fun valueAt(date: Instant): T {
    return valueAtOrNull(date) ?: throw IllegalStateException("No timeline entry available at date $date — ${describeCoverage()}")
  }

  /**
   * What this timeline covers, as the fragment a lookup failure appends to name its reason.
   *
   * A lookup finds nothing for two different reasons, and the requested date alone tells them
   * apart for neither: the timeline was never seeded, or its entries all start after the
   * requested date. The two call for different fixes, so the message says which one it was.
   */
  internal fun describeCoverage(): String {
    //entries are sorted newest-first, so the last one is the earliest
    val earliest = entries.lastOrNull() ?: return "the timeline is empty"
    return "the earliest entry is valid from ${earliest.validFrom}"
  }

  /**
   * Returns the value valid for the given date, or null if no entry is valid at [date].
   */
  fun valueAtOrNull(date: Instant): T? {
    return entryAt(date)?.value
  }

  /**
   * Returns the next upcoming entry with `validFrom > now` — the one that will
   * become effective next, if any. Returns null when there are no future-dated
   * entries.
   *
   * Entries are stored newest-first; the last future-dated entry in that order
   * is the one closest to `now`.
   */
  fun firstFutureEntry(now: Instant): Entry<T>? {
    return entries.lastOrNull { it.validFrom > now }
  }

  /**
   * Returns the newest value - might be a future value!
   */
  fun newestValue(): T {
    return newestValueOrNull() ?: throw IllegalStateException("No timeline entries available")
  }

  /**
   * Returns the newest value, or null if the timeline is empty.
   */
  fun newestValueOrNull(): T? {
    return entries.firstOrNull()?.value
  }

  fun isEmpty(): Boolean {
    return entries.isEmpty()
  }

  /**
   * Returns true if an entry for the given instant exists
   */
  fun containsEntryWithExactValidFrom(instant: Instant): Boolean {
    return entries.any { it.validFrom == instant }
  }

  /**
   * Returns a new timeline with the given entry added. Throws [IllegalArgumentException]
   * if an entry with the same `validFrom` already exists — Add-Forward semantics: entries
   * are immutable records, use [withAddedOrReplaced] only when overwriting is intended.
   */
  fun withAdded(entry: Entry<T>): ValueTimeline<T> {
    require(containsEntryWithExactValidFrom(entry.validFrom).not()) {
      "Entry for validFrom=${entry.validFrom} already exists — use withAddedOrReplaced() explicitly if overwriting is intended, or add a new entry with a later validFrom per the Add-Forward pattern."
    }
    return plus(entry)
  }

  /**
   * Returns a new timeline with the given entry added or replaced (if an entry with the same validFrom exists)
   */
  fun withAddedOrReplaced(entry: Entry<T>): ValueTimeline<T> {
    val updatedEntries = entries.toMutableList()
    updatedEntries.removeAll { it.validFrom == entry.validFrom }
    updatedEntries.add(entry)

    updatedEntries.sortByDescending { it.validFrom }

    return ValueTimeline(updatedEntries)
  }

  /**
   * Returns this timeline unchanged if [entry] is null,
   * otherwise returns a new timeline with the entry added or replaced.
   */
  fun withAddedOrReplacedIfNotNull(entry: Entry<T>?): ValueTimeline<T> {
    return if (entry == null) this else withAddedOrReplaced(entry)
  }

  /**
   * A single timeline entry: the [value] that becomes effective at [validFrom].
   *
   * Covariant in [T]: an entry only ever hands its value out. A timeline of a supertype therefore
   * accepts entries built from a subtype — `TimelineEntry<BareArticleConditions>` is a
   * `TimelineEntry<ArticleConditions>`, so fixtures and generators can construct the concrete
   * implementation their article has without restating the timeline's type.
   */
  @GermanTerm("Zeitachsen-Eintrag")
  @Serializable
  data class Entry<out T>(
    /**
     * The set value
     */
    val value: T,

    /**
     * The timestamp from which this value will be the new valid value.
     */
    @GermanTerm("Gültig ab")
    @Inclusive
    val validFrom: NativeInstant,
  )

  companion object {
    /**
     * An empty timeline
     */
    fun <T> empty(): ValueTimeline<T> {
      return ValueTimeline(entries = emptyList())
    }

    /**
     * Creates a timeline with a single entry
     */
    fun <T> single(entry: Entry<T>): ValueTimeline<T> {
      return ValueTimeline(entries = listOf(entry))
    }

    val DistantPastForSingle: Instant = Instant.parse("2000-01-01T00:00:00Z")

    /**
     * Creates a timeline with a single entry
     */
    fun <T> single(value: T, validFrom: Instant = DistantPastForSingle): ValueTimeline<T> {
      return ValueTimeline(entries = listOf(Entry(value, validFrom)))
    }
  }
}
