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
package it.neckar.commons.logback

import ch.qos.logback.classic.Logger


/**
 * Converts a logback level to a slf4j log level
 */
fun ch.qos.logback.classic.Level.toSlf4j(): org.slf4j.event.Level {
  return when (this) {
    ch.qos.logback.classic.Level.ERROR -> org.slf4j.event.Level.ERROR
    ch.qos.logback.classic.Level.WARN -> org.slf4j.event.Level.WARN
    ch.qos.logback.classic.Level.INFO -> org.slf4j.event.Level.INFO
    ch.qos.logback.classic.Level.DEBUG -> org.slf4j.event.Level.DEBUG
    ch.qos.logback.classic.Level.TRACE -> org.slf4j.event.Level.TRACE
    else -> org.slf4j.event.Level.INFO
  }
}

/**
 * Converts a slf4j level to a logback level
 */
fun org.slf4j.event.Level.toLogback(): ch.qos.logback.classic.Level {
  return when (this) {
    org.slf4j.event.Level.ERROR -> ch.qos.logback.classic.Level.ERROR
    org.slf4j.event.Level.WARN -> ch.qos.logback.classic.Level.WARN
    org.slf4j.event.Level.INFO -> ch.qos.logback.classic.Level.INFO
    org.slf4j.event.Level.DEBUG -> ch.qos.logback.classic.Level.DEBUG
    org.slf4j.event.Level.TRACE -> ch.qos.logback.classic.Level.TRACE
  }
}

/**
 * Returns the corresponding logback logger
 */
fun org.slf4j.Logger.toLogback(): Logger {
  return LogbackConfigurer.getLogbackLogger(this)
}

/**
 * Allows setting the level on the logger directly
 */
var org.slf4j.Logger.level: org.slf4j.event.Level
  get() {
    return LogbackConfigurer.getLoggerLevel(this)
  }
  set(level) {
    LogbackConfigurer.setLoggerLevel(this, level)
  }


/**
 * Conditional debug action that is only executed if debug is enabled
 */
inline fun org.slf4j.Logger.trace(action: () -> String) {
  if (this.isTraceEnabled) {
    this.trace(action())
  }
}

/**
 * Conditional debug action that is only executed if debug is enabled
 */
inline fun org.slf4j.Logger.debug(action: () -> String) {
  if (this.isDebugEnabled) {
    this.debug(action())
  }
}

/**
 * Conditional debug action that is only executed if info is enabled
 */
inline fun org.slf4j.Logger.info(action: () -> String) {
  if (this.isInfoEnabled) {
    this.info(action())
  }
}

/**
 * Conditional debug action that is only executed if warn is enabled
 */
inline fun org.slf4j.Logger.warn(action: () -> String) {
  if (this.isWarnEnabled) {
    this.warn(action())
  }
}

/**
 * Conditional debug action that is only executed if error is enabled
 */
inline fun org.slf4j.Logger.error(action: () -> String) {
  if (this.isErrorEnabled) {
    this.error(action())
  }
}

