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
@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package it.neckar.react.common

import it.neckar.common.featureflags.FeatureFlag
import it.neckar.common.featureflags.FeatureFlagsSupport
import it.neckar.open.annotations.NonBlocking
import it.neckar.react.common.toast.*
import kotlinx.coroutines.*

/**
 * Launches an action. Returns immediately.
 *
 * Helper methods that does not return anything.
 *
 * * Use [deferAndNotify] if the return type shall be used.
 * * Use [executeAndNotify] if called from a suspend function.
 *
 * !!! Do *not* call from a suspend function.
 */
@NonBlocking
fun <T> launchAndNotify(successMessage: String? = null, action: suspend () -> T) {
  AppScope.launch {
    executeAndNotify(successMessage, action)
  }
}

/**
 * Executes a REST action.
 * Returns immediately.
 *
 * ATTENTION: Use the return type!
 *
 * * Use [launchAndNotify] if the return type shall not be used.
 * * Use [executeAndNotify] if called from a suspend function.
 *
 * !!! Do *not* call from a suspend function.
 *
 *
 * Avoid using this method if possible. This method should only be used, to combine `suspend` functions with old code.
 * Instead, create and execute a suspend method.
 */
@NonBlocking
fun <T> deferAndNotify(successMessage: String? = null, action: suspend () -> T): Deferred<T> {
  return AppScope.async {
    executeAndNotify(successMessage, action)
  }
}

/**
 * Executes an action and shows the success message if provided.
 *
 * Shows a success method on success.
 */
suspend fun <T> executeAndNotify(successMessage: String?, action: suspend () -> T): T {
  delayIfSlowUiEnabled()
  val result = action()
  if (successMessage != null) {
    notifySuccess(successMessage)
  }

  return result
}

/**
 * This method delays the UI if the corresponding feature flag is enabled
 * Blocks for 1000 milliseconds if the corresponding feature flag ([FeatureFlag.slowUi]) is set
 */
suspend fun delayIfSlowUiEnabled() {
  if (FeatureFlagsSupport.flags.contains(FeatureFlag.slowUi)) {
    delay(1000)
  }
}
