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
package it.neckar.playwright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.ConsoleMessage
import com.microsoft.playwright.Page
import com.microsoft.playwright.Response
import it.neckar.open.http.Url

/**
 * Navigate to the provided [url].
 *
 * Playwright returns no response for a same-document navigation (anchor or History API).
 * Every call site navigates to a document URL, so a missing response is a bug.
 */
fun Page.navigate(url: Url): Response {
  return requireNotNull(navigate(url.value)) { "Navigation to <${url.value}> returned no response" }
}

fun ConsoleMessage.isError(): Boolean {
  return type() == "error"
}

fun ConsoleMessage.isNotError(): Boolean {
  return isError().not()
}

/**
 * Extracts the major version for the browser.
 */
fun Browser.majorVersion(): Int {
  return this.version().substringBefore(".").toInt()
}
