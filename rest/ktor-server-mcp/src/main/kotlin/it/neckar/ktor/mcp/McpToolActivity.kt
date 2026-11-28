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
package it.neckar.ktor.mcp

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

/**
 * One activity of a concept in the spec catalogue that a tool carries out, e.g. `BookOrdering/issue-quote`.
 *
 * [concept] is the `name` of the concept file under `specs/01-domain/concepts/`, [id] the `id` of one entry of its `activities:`.
 */
data class McpToolActivity(val concept: String, val id: String) {
  /** `<Concept>/<id>`, the form the tool lists in its `_meta`. */
  val reference: String
    get() = "$concept/$id"

  companion object {
    /** The key of `_meta` under which a tool lists the [reference] of every activity it carries out. */
    const val MetaKey: String = "it.neckar/activities"
  }
}

/**
 * [meta] with the references of [activities] added under [McpToolActivity.MetaKey]; [meta] unchanged when [activities] is empty.
 */
internal fun withActivities(meta: JsonObject?, activities: List<McpToolActivity>): JsonObject? {
  if (activities.isEmpty()) {
    return meta
  }
  return buildJsonObject {
    meta?.forEach { (key, value) -> put(key, value) }
    putJsonArray(McpToolActivity.MetaKey) {
      activities.forEach { add(JsonPrimitive(it.reference)) }
    }
  }
}
