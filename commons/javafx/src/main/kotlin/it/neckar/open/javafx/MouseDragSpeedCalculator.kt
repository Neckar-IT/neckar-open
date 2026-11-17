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
@file:Suppress("UnstableApiUsage")

package it.neckar.open.javafx

import it.neckar.open.annotations.UiThread
import it.neckar.open.unit.other.px
import it.neckar.open.unit.other.px_ns
import it.neckar.open.unit.si.ns
import com.google.common.collect.EvictingQueue
import java.util.Collections
import java.util.Locale
import java.util.function.ToDoubleFunction
import javax.annotation.Nonnull

/**
 * Calculates mouse speeds when dragging
 */
class MouseDragSpeedCalculator {
  /**
   * Contains the last speed elements of the last n dragging events
   */
  @UiThread
  private val entries: @px_ns EvictingQueue<Entry> = EvictingQueue.create(SpeedQueueSize)

  @UiThread
  private fun calculateSpeed(getSpeed: ToDoubleFunction<Entry>): @px_ns Double {
    if (entries.isEmpty()) {
      return 0.0
    }

    // The algorithm is taken from http://mortoray.com/2015/04/08/measuring-finger-mouse-velocity-at-release-time/ .
    var totalTime = 0.0
    for (entry in entries) {
      totalTime += entry.deltaTime.toDouble()
    }
    var speed = 0.0
    for (entry in entries) {
      val alpha = 1.0 - Math.pow(Math.E, -entry.deltaTime / totalTime)
      speed = alpha * getSpeed.applyAsDouble(entry) + (1.0 - alpha) * speed
    }
    return speed
  }

  @UiThread
  fun calculateSpeedX(): @px_ns Double {
    return calculateSpeed { obj: Entry -> obj.speedX }
  }

  @UiThread
  fun calculateSpeedY(): @px_ns Double {
    return calculateSpeed { obj: Entry -> obj.speedY }
  }

  /**
   * Adds a new entry
   *
   * @param deltaTime the delta time
   */
  @UiThread
  fun add(deltaTime: @ns Long, deltaX: @px Double, deltaY: @px Double) {
    if (deltaTime <= 0) {
      return
    }
    val now: @ns Long = System.nanoTime()

    //first remove entries that might be too old
    entries.removeIf { entry: Entry -> now - entry.time > MaxAge }
    entries.add(Entry(now, deltaTime, deltaX / deltaTime.toDouble(), deltaY / deltaTime.toDouble()))
  }

  /**
   * Clears the speed calculation
   */
  @UiThread
  fun clear() {
    entries.clear()
  }

  @Nonnull
  fun getEntries(): Collection<Entry> {
    return Collections.unmodifiableCollection(entries)
  }

  class Entry(val time: @ns Long, val deltaTime: @ns Long, val speedX: @px_ns Double, val speedY: @px_ns Double) {
    override fun toString(): String {
      return String.format(Locale.US, "Entry{time=%d, deltaTime=%d, speedX=%s, speedY=%s}", time, deltaTime, speedX, speedY)
    }
  }

  companion object {
    const val SpeedQueueSize: Int = 15
    const val MaxAge: @ns Int = 400 * 1000 * 1000
  }
}
