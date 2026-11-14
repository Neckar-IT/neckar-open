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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class PostsResponseParserTest {
  //language=JSON
  private val json = """
    {
      "order": ["p2", "p1"],
      "posts": {
        "p1": { "create_at": 1000, "user_id": "u1", "channel_id": "c1", "message": "hello" },
        "p2": { "create_at": 2000, "user_id": "u2", "channel_id": "c2", "message": "say \"hi\"" }
      }
    }
  """.trimIndent()

  @Test
  fun `message is the raw text, not the quoted JSON representation`() {
    val posts = PostsResponseParser.parse(json).associateBy { it.id }
    assertThat(posts.getValue(PostId("p1")).message).isEqualTo("hello")
    assertThat(posts.getValue(PostId("p2")).message).isEqualTo("say \"hi\"")
  }

  @Test
  fun `posts are returned in the order given by the order array`() {
    val posts = PostsResponseParser.parse(json)
    assertThat(posts.map { it.id }).containsExactly(PostId("p2"), PostId("p1"))
  }

  @Test
  fun `a post carries the channel it was written in`() {
    val posts = PostsResponseParser.parse(json).associateBy { it.id }
    assertThat(posts.getValue(PostId("p1")).channelId).isEqualTo(ChannelId("c1"))
    assertThat(posts.getValue(PostId("p2")).channelId).isEqualTo(ChannelId("c2"))
  }

  @Test
  fun `a missing property names the post it is missing from`() {
    //language=JSON
    val jsonWithoutCreateAt = """
      {
        "order": ["p1"],
        "posts": {
          "p1": { "user_id": "u1", "channel_id": "c1", "message": "hello" }
        }
      }
    """.trimIndent()

    assertFailure { PostsResponseParser.parse(jsonWithoutCreateAt) }
      .hasMessage("<create_at> is missing for post <p1>")
  }

  @Test
  fun `a non numeric create_at names the post and the offending value`() {
    //language=JSON
    val jsonWithTextualCreateAt = """
      {
        "order": ["p1"],
        "posts": {
          "p1": { "create_at": "yesterday", "user_id": "u1", "channel_id": "c1", "message": "hello" }
        }
      }
    """.trimIndent()

    assertFailure { PostsResponseParser.parse(jsonWithTextualCreateAt) }
      .hasMessage("<create_at> of post <p1> is not a number but <\"yesterday\">")
  }

  @Test
  fun `a null message fails instead of becoming the text null`() {
    //language=JSON
    val jsonWithNullMessage = """
      {
        "order": ["p1"],
        "posts": {
          "p1": { "create_at": 1000, "user_id": "u1", "channel_id": "c1", "message": null }
        }
      }
    """.trimIndent()

    assertFailure { PostsResponseParser.parse(jsonWithNullMessage) }
      .hasMessage("<message> of post <p1> is null")
  }
}
