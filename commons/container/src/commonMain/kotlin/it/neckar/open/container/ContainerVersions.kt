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
package it.neckar.open.container

import it.neckar.open.version.Version
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** True for a version [Version.format] writes as `<major>.<minor>.<patch>`, the only form a container holds. */
internal val Version.isContainerVersion: Boolean
  get() = suffix.isNullOrBlank()

/** Parses [text] as container version; throws [IllegalArgumentException] unless it formats back to exactly [text]. */
fun parseContainerVersion(text: String): Version {
  val version = try {
    Version.parse(text)
  } catch (e: IllegalArgumentException) {
    throw IllegalArgumentException("Version <$text> is no plain <major>.<minor>.<patch>", e)
  }
  require(version.isContainerVersion) { "Version <$text> has a suffix" }
  require(version.format() == text) { "Version <$text> is no plain <major>.<minor>.<patch>" }
  return version
}

/** A container version as JSON string, decoded by [parseContainerVersion]: `"02.3.0"` is rejected like in an entry name. */
internal object ContainerVersionSerializer : KSerializer<Version> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("it.neckar.open.container.ContainerVersion", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Version) {
    encoder.encodeString(value.format())
  }

  override fun deserialize(decoder: Decoder): Version {
    return parseContainerVersion(decoder.decodeString())
  }
}
