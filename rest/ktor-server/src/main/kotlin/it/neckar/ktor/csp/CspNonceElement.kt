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
package it.neckar.ktor.csp

import it.neckar.html.csp.CspNonce
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext

/**
 * Carries the nonce of the current response in the coroutine context, so whoever renders the HTML
 * reads it without it being threaded through every signature in between.
 *
 * Installed by [ContentSecurityPolicyPlugin] for the duration of a call.
 */
class CspNonceElement(val nonce: CspNonce) : AbstractCoroutineContextElement(Key) {
  companion object Key : CoroutineContext.Key<CspNonceElement>
}

/**
 * Returns the [CspNonce] of the current response — the same value the policy header announces. Put
 * it on every inline `<script>` / `<style>` block the response embeds; the browser blocks any
 * block without it.
 *
 * Throws if [ContentSecurityPolicyPlugin] is not installed: a nonce no policy announces yields a
 * silently broken page, so there is no sensible fallback.
 */
suspend fun cspNonce(): CspNonce {
  return currentCoroutineContext()[CspNonceElement]?.nonce
    ?: error("ContentSecurityPolicyPlugin not installed. Call installContentSecurityPolicy() in Application.configure().")
}
