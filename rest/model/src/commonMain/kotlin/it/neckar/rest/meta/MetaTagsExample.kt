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
package it.neckar.rest.meta

import it.neckar.gradle.git.GitHash
import it.neckar.gradle.git.GitHashShort
import kotlin.time.Instant

/**
 * Example data for [MetaTags] used in OpenAPI documentation
 */
object MetaTagsExample {
  val example1: MetaTags = MetaTags(
    gitHash = GitHash("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0"),
    gitHashShort = GitHashShort("a1b2c3d4e5f6"),
    gitCommitDateTime = Instant.parse("2024-06-01T12:34:56Z"),
    imageDigest = ImageDigest("sha256:ad2453b02334597670fc64befad42c10ec0870d092c58b8ae0ca6ef1e903d221"),
  )
}
