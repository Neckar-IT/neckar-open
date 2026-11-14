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
package it.neckar.commons.tags

import kotlinx.serialization.Serializable

/**
 * Contains 0..n tags
 */
@Serializable(with = TagsSerializer::class)
data class Tags(
  val tags: Set<Tag>,
) {

  fun contains(tag: Tag): Boolean {
    return this.tags.contains(tag)
  }

  fun isEmpty(): Boolean {
    return this.tags.isEmpty()
  }

  operator fun plus(additionalTag: Tag): Tags {
    return Tags(buildSet {
      addAll(tags)
      add(additionalTag)
    })
  }

  fun format(): String {
    return tags.joinToString { it.id }
  }

  companion object {
    operator fun invoke(tag: Tag): Tags {
      return Tags(setOf(tag))
    }

    operator fun invoke(tag0: Tag, tag1: Tag): Tags {
      return Tags(setOf(tag0, tag1))
    }

    operator fun invoke(tag0: Tag, tag1: Tag, tag2: Tag): Tags {
      return Tags(setOf(tag0, tag1, tag2))
    }

    val empty: Tags = Tags(setOf())
    val demo: Tags = Tags(setOf(Tag("ElectricityWork")))
  }
}

fun Tags?.contains(tag: Tag): Boolean {
  return this != null && this.contains(tag)
}

/**
 * Returns true if this is null or none of the provided tags are within this.
 *
 * Attention: This method does not use varargs to avoid unnecessary instantiations of objects
 */
fun Tags?.containsNone(tag0: Tag, tag1: Tag, tag2: Tag, tag3: Tag, tag4: Tag): Boolean {
  if (this == null) {
    return true
  }

  return this.contains(tag0).not()
    && this.contains(tag1).not()
    && this.contains(tag2).not()
    && this.contains(tag3).not()
    && this.contains(tag4).not()
}

/**
 * Wraps the receiver in a single-element [Tags], or returns [Tags.empty] when the receiver is null.
 */
fun Tag?.toTagsOrEmpty(): Tags = this?.toTags() ?: Tags.empty

/**
 * Wraps the receiver in a single-element [Tags].
 *
 * There is deliberately no `Tag?.toTags(): Tags?` overload: a nullable receiver either has a
 * meaningful empty case - then [toTagsOrEmpty] is the right call - or the null has to be handled
 * at the call site, where `tag?.toTags()` says so in one character.
 */
fun Tag.toTags(): Tags {
  return Tags(this)
}
