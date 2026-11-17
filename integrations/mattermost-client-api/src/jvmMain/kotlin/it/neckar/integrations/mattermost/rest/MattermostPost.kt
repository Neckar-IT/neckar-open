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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The body of every post the client creates in Mattermost.
 */
@Serializable
data class MattermostPost(
  @SerialName("channel_id")
  val channelId: ChannelId,
  /**
   * The message contents. Can be formatted with Markdown
   */
  @SerialName("message")
  val message: String,

  /**
   * Custom post type for plugin-rendered posts.
   *
   * When set to a `custom_*` value, Mattermost plugins can register a component
   * via `registerPostTypeComponent` to override the post's rendering.
   */
  @SerialName("type")
  val type: PostType? = null,

  /**
   * The post ID to comment on
   */
  @SerialName("root_id")
  val rootId: PostId? = null,

  /**
   * A list of file IDs to associate with the post. Note that posts are limited to 5 files maximum. Please use additional posts for more files.
   */
  @SerialName("file_ids")
  val fileIds: List<String>? = null,

  /**
   * A JSON object to add post metadata, e.g the post's priority
   */
  @SerialName("metadata")
  val metadata: Metadata? = null,

  /**
   * A general JSON property bag to attach to the post
   */
  @SerialName("props")
  val props: MattermostPostProps? = null,
)
