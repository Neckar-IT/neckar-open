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
