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
package it.neckar.open.concurrent

import it.neckar.open.time.monotonicTimeSource
import java.awt.AWTEvent
import java.awt.EventQueue
import java.awt.Toolkit
import java.lang.ref.WeakReference
import java.util.Timer
import java.util.TimerTask
import kotlin.time.ComparableTimeMark

/**
 * Event queue that checks if the EDT is blocked
 *
 */
public class EventQueueWithBlockingChecker
private constructor() : EventQueue() {
  // Main timer
  private val timer = Timer("EventQueue With EDT Blocking Checker", true)

  private val eventChangeLock = Any()
  @Volatile
  private var eventDispatchingStart: ComparableTimeMark? = null

  @Volatile
  private var event: AWTEvent? = null

  private fun kill() {
    timer.cancel()
  }

  /**
   * Record the event and continue with usual dispatching.
   */
  override fun dispatchEvent(event: AWTEvent) {
    setEventDispatchingStart(event, monotonicTimeSource.markNow())
    super.dispatchEvent(event)
    setEventDispatchingStart(null, null)
  }

  /**
   * Register event and dispatching start time.
   *
   * @param anEvent   event.
   * @param start     dispatching start, null when no event is dispatched.
   */
  private fun setEventDispatchingStart(anEvent: AWTEvent?, start: ComparableTimeMark?) {
    synchronized(eventChangeLock) {
      event = anEvent
      eventDispatchingStart = start
    }
  }

  /**
   * Add watchdog timer. Timer will trigger `listener`
   * if the queue dispatching event longer than specified
   * `maxProcessingTime`. If the timer is
   * `repetitive` then it will trigger additional
   * events if the processing 2x, 3x and further longer than
   * `maxProcessingTime`.
   *
   * @param maxProcessingTime maximum processing time.
   * @param listener          listener for events. The listener
   * will receive `AWTEvent`
   * as source of event.
   * @param repetitive        TRUE to trigger consequent events
   * for 2x, 3x and further periods.
   */
  public fun addWatchdog(maxProcessingTime: Long, listener: Listener, repetitive: Boolean) {
    val checker = Watchdog(maxProcessingTime, listener, repetitive)
    timer.schedule(checker, maxProcessingTime, maxProcessingTime)
  }

  /**
   * Checks if the processing of the event is longer than the
   * specified `maxProcessingTime`. If so then
   * listener is notified.
   */
  private inner class Watchdog
  /**
   * Creates timer.
   *
   * @param maxProcessingTime maximum event processing time
   * before listener is notified.
   * @param listener          listener to notify.
   * @param repetitive        TRUE to allow consequent
   * notifications for the same event
   */
  constructor(
    private val maxProcessingTime: Long,
    private val listener: Listener,
    private val repetitive: Boolean
  ) : TimerTask() {

    // Event reported as "lengthy" for the last time. Used to
    // prevent repetitive behaviour in non-repeatitive timers.
    private var lastReportedEvent = WeakReference<AWTEvent>(null)

    init {
      require(maxProcessingTime >= 0) { "Max locking period should be greater than zero" }
    }

    override fun run() {
      val start: ComparableTimeMark?
      val currentEvent: AWTEvent?

      // Get current event requisites
      synchronized(eventChangeLock) {
        start = eventDispatchingStart
        currentEvent = event
      }

      if (currentEvent == null || start == null) {
        return
      }

      // Check if event is being processed longer than allowed
      val delta: Long = start.elapsedNow().inWholeMilliseconds
      if (delta <= maxProcessingTime) {
        return
      }

      if (repetitive || currentEvent !== lastReportedEvent.get()) {
        listener.edtBlocked(delta, currentEvent)
        lastReportedEvent = WeakReference(currentEvent)
      }
    }
  }

  /**
   * Is notified when the edt has been blocked
   */
  public interface Listener {
    /**
     * Is called if the edt is blocked
     *
     * @param blockedTime  the blocked time
     * @param currentEvent the current event
     */
    public fun edtBlocked(blockedTime: Long, currentEvent: AWTEvent)
  }

  public companion object {

    /**
     * Install alternative queue.
     */
    public fun install(): EventQueueWithBlockingChecker {
      val eventQueue = Toolkit.getDefaultToolkit().systemEventQueue
      val newEventQueue = EventQueueWithBlockingChecker()
      eventQueue.push(newEventQueue)
      return newEventQueue
    }

    /**
     * Uninstalls the event queue if necessary
     */
    public fun uninstallIfNecessary(): Boolean {
      val eventQueue = Toolkit.getDefaultToolkit().systemEventQueue as? EventQueueWithBlockingChecker ?: return false

      eventQueue.push(EventQueue())
      eventQueue.kill()

      return true
    }

    /**
     * Uninstalls the EventQueueWithChecker
     */
    @Throws(IllegalStateException::class)
    public fun uninstall() {
      val eventQueue = Toolkit.getDefaultToolkit().systemEventQueue
      if (eventQueue !is EventQueueWithBlockingChecker) {
        throw IllegalStateException("Invalid eventQueue detected. Was <$eventQueue> but expected instance of EventQueueWithChecker")
      }

      eventQueue.push(EventQueue())
      eventQueue.kill()
    }
  }
}
