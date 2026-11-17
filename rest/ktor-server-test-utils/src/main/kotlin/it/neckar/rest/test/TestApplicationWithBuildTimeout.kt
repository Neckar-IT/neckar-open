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
package it.neckar.rest.test

import io.ktor.server.testing.*
import io.ktor.test.dispatcher.runTestWithRealTime
import kotlinx.coroutines.test.TestResult
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The kotlinx.coroutines property that holds the wall-clock budget of a `runTest` body.
 * `configureJunit()` of `it.neckar.buildplatform:conventions` writes it on every test task.
 */
private const val CoroutinesTestTimeoutProperty: String = "kotlinx.coroutines.test.default_timeout"

/** The budget of `runTest` when [CoroutinesTestTimeoutProperty] is unset. */
private val LibraryDefaultTestTimeout: Duration = 60.seconds

/**
 * Starts a Ktor test server under the wall-clock budget of the build, which
 * [io.ktor.server.testing.testApplication] replaces with the 60s default argument of
 * [runTestWithRealTime].
 */
fun testApplicationWithBuildTimeout(
  parentCoroutineContext: CoroutineContext = EmptyCoroutineContext,
  block: suspend ApplicationTestBuilder.() -> Unit,
): TestResult = runTestWithRealTime(parentCoroutineContext, timeout = buildTestTimeout()) {
  runTestApplication(parentCoroutineContext, block)
}

/**
 * The value of [CoroutinesTestTimeoutProperty], or [LibraryDefaultTestTimeout] outside a Gradle
 * test task.
 */
internal fun buildTestTimeout(): Duration {
  val configured = System.getProperty(CoroutinesTestTimeoutProperty) ?: return LibraryDefaultTestTimeout
  return Duration.parse(configured)
}
