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
package it.neckar.ksp.ts.generation.sample.collision

// Own package: the domain types `String` and `List` shadow the Kotlin builtins for their whole package,
// so they must not live in the shared `sample` package used by the other fixtures.

import it.neckar.open.annotations.TsExport

/**
 * A domain type whose simple name collides with kotlin.String.
 * Must be generated as its own type, not the TypeScript `string` primitive.
 */
@TsExport
class String(val label: kotlin.String)

/**
 * A domain type whose simple name collides with kotlin.collections.List.
 * Must be generated as its own type, not a TypeScript array (it has no type argument,
 * so the builtin `List` branch would even crash on `check(arguments.size == 1)`).
 */
@TsExport
class List(val label: kotlin.String)

/**
 * Holds properties of the domain [String] and [List] types, not the Kotlin builtins.
 */
@TsExport
data class ExampleClassWithBuiltinNameCollision(
  val collidingString: String,
  val collidingList: List,
)
