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
package it.neckar.common.featureflags

/**
 * Marks a feature flag as an Ops Toggle.
 *
 * Ops toggles simulate error conditions, delays, and edge cases for testing and diagnostics.
 * They are permanent — useful throughout the lifetime of the application.
 *
 * Examples:
 * - `slowServer` — add artificial delay to HTTP responses
 * - `forceServerError` — force HTTP 500 on every request
 * - `throwException` — trigger uncaught exception to test error handling
 */
@Target(AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class OpsToggle
