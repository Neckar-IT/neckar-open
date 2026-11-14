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
import it.neckar.open.annotations.DataView
import it.neckar.open.kotlin.serializers.NativeInstant
import it.neckar.open.version.VersionInformation
import kotlinx.serialization.Serializable


/**
 * Contains version and build information about the service.
 */
@DataView("Version info: identifies the build a running artifact was cut from")
@Serializable
data class MetaTags(
  /**
   * Null unless the artifact names its commit (a fat-jar CLI); a container names its [imageDigest].
   */
  val gitHash: GitHash?,
  /**
   * Null unless the artifact names its commit (a fat-jar CLI); a container names its [imageDigest].
   */
  val gitHashShort: GitHashShort?,
  /**
   * Null unless the artifact names its commit (a fat-jar CLI); a container names its [imageDigest].
   */
  val gitCommitDateTime: NativeInstant?,
  /**
   * Null where the deploy injects no pinned digest (local development, branch deploy, the lizergy and bioexp deploys). Defaults to null
   * because a service built before the field existed omits it.
   */
  val imageDigest: ImageDigest? = null,
)

/**
 * Projects the build metadata of the running artifact onto its wire representation.
 */
fun VersionInformation.toMetaTags(): MetaTags {
  return MetaTags(
    gitHash = gitHashOrNull?.let { GitHash(it) },
    //Derived from the same source as the full hash - single length definition in VersionInformation
    gitHashShort = gitHashOrNull?.let { GitHashShort(VersionInformation.gitHashShort) },
    gitCommitDateTime = gitCommitInstantOrNull,
    imageDigest = imageDigestOrNull?.let { ImageDigest(it) },
  )
}
