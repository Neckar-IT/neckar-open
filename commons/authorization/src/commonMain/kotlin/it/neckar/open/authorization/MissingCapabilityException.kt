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
package it.neckar.open.authorization

import it.neckar.open.exceptions.ErrorCode
import it.neckar.open.exceptions.PermissionDeniedException

/**
 * Thrown when an authorization check fails because the user does not have a
 * required capability.
 *
 * Subclass of [PermissionDeniedException] so the global StatusPages handler
 * keeps mapping to HTTP 403. A dedicated handler installed via
 * `installStatusPages` extracts [missingCapability] and forwards it to the
 * client as a structured attribute (`missingCapability`), allowing the
 * frontend to render a precise message ("Sie benötigen die Berechtigung
 * *Bestellung abschließen*") instead of a generic 403.
 *
 * The error message is built from the capability key so that server-side
 * logs and stack traces remain self-describing without consulting the
 * structured payload.
 */
class MissingCapabilityException(
  val missingCapability: CapabilityKey,
) : PermissionDeniedException("Capability $missingCapability required") {

  companion object {
    /**
     * Error code carried in the structured 403 response. The frontend may use
     * this as the i18n key for the "permission denied" toast.
     */
    val Code: ErrorCode = ErrorCode(ErrorCode.Prefix("AUTHZ"), 1)

    /**
     * Key for the [missingCapability] entry inside
     * `ExpectedApplicationError.attributes`. Stable contract between server
     * and frontend.
     */
    const val MissingCapabilityAttribute: String = "missingCapability"
  }
}
