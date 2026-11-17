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
package it.neckar.open.coroutines

import assertk.*
import assertk.assertions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test


class CoroutinesExceptionTest {
  @Test
  fun testErrorHandler() = runTest {
    val errors = mutableListOf<Throwable>()

    val appScope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
      errors.add(throwable)
    })

    assertThat(errors).isEmpty()

    appScope.launch(testScheduler) {
      throw RuntimeException("Uuups")
    }.join()

    assertThat(errors).hasSize(1)
    assertThat(errors[0]).isInstanceOf(RuntimeException::class)


    //The result of deferred is *not* thrown, instead should be accessed using the deferred
    val deferred = appScope.async {
      throw IllegalStateException("bar")
    }

    assertThat(errors).hasSize(1)
    deferred.join()
    assertThat(errors).hasSize(1)
    assertThat(errors[0]).isInstanceOf(RuntimeException::class)
  }
}
