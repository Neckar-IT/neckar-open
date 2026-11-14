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

import it.neckar.open.version.ImageDigestText
import it.neckar.open.version.VersionInformation
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Identifies the artifact a service runs, for humans: the short image digest (`ad2453b02334`) in a
 * container whose deploy pins one, otherwise commit date plus short hash (`2026-07-15 - 1d79fb128769`),
 * or `unknown` where nothing is injected.
 *
 * A display value, not a queryable identity — [MetaTags] carries the digest and the commit typed,
 * for consumers that compare or parse them.
 */
@Serializable
@JvmInline
value class DeployedArtifact(val value: String) {
  init {
    require(value.isNotBlank()) { "DeployedArtifact must not be blank" }
  }

  override fun toString(): String = value

  companion object {
    /**
     * The deployed artifact of this process, from [VersionInformation].
     */
    fun fromVersionInformation(): DeployedArtifact {
      return of(VersionInformation.imageDigestOrNull, VersionInformation.commitDateAndShortHash)
    }

    /**
     * The short form of [imageDigest] where there is one, otherwise [commitDateAndShortHash].
     */
    fun of(imageDigest: String?, commitDateAndShortHash: String): DeployedArtifact {
      return DeployedArtifact(ImageDigestText.shortOrElse(imageDigest) { commitDateAndShortHash })
    }
  }
}
