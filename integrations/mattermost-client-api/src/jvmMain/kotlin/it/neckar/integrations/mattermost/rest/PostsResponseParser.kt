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
package it.neckar.integrations.mattermost.rest

import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.unit.other.Sorted
import it.neckar.open.unit.si.ms
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object PostsResponseParser {
  fun parse(json: String): List<Post> {
    val jsonTree = Json.parseToJsonElement(json)

    val order = jsonTree.jsonObject["order"].requireNotNull { "order is missing" }

    @Sorted val postIds = order.jsonArray.map { id ->
      PostId(id.jsonPrimitive.content)
    }

    val postsJson = jsonTree.jsonObject["posts"].requireNotNull { "posts is missing" }

    val postsById = postsJson.jsonObject.entries.associate { (key, value) ->
      val postId = PostId(key)
      val postJson = value.jsonObject

      postId to Post(
        id = postId,
        createAt = postJson.requireMillis("create_at", postId),
        message = postJson.requireString("message", postId),
        userId = UserId(postJson.requireString("user_id", postId)),
        channelId = ChannelId(postJson.requireString("channel_id", postId)),
      )
    }

    //Return the posts in the sequence defined by the "order" array (map iteration order is undefined).
    return postIds.map { postId ->
      postsById[postId].requireNotNull { "post <$postId> from order is missing in posts" }
    }
  }

  /**
   * Returns the string value of the given property - naming the post in the exception if the property is missing or JSON null.
   *
   * A JSON null must not silently become the text "null": that turns a broken response into a post that looks valid.
   */
  private fun JsonObject.requireString(propertyName: String, postId: PostId): String {
    val property = this[propertyName].requireNotNull { "<$propertyName> is missing for post <$postId>" }
    return property.jsonPrimitive.contentOrNull.requireNotNull { "<$propertyName> of post <$postId> is null" }
  }

  /**
   * Returns the timestamp value of the given property - naming the post and the offending value in the exception.
   */
  private fun JsonObject.requireMillis(propertyName: String, postId: PostId): @ms Double {
    val property = this[propertyName].requireNotNull { "<$propertyName> is missing for post <$postId>" }
    return property.jsonPrimitive.doubleOrNull.requireNotNull { "<$propertyName> of post <$postId> is not a number but <$property>" }
  }
}
