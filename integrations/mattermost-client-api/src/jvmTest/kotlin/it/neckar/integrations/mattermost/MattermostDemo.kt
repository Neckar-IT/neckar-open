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
package it.neckar.integrations.mattermost

import it.neckar.commons.logback.LogbackConfigurer
import it.neckar.integrations.mattermost.rest.ChannelId
import it.neckar.open.collections.fastForEach
import it.neckar.open.formatting.formatUtc
import it.neckar.open.http.Url
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.time.now
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.minutes

/**
 * Manual demo against the live Mattermost API — run [main] directly.
 *
 * Reads the API token from the environment variable `MATTERMOST_TOKEN`.
 */
fun main(): Unit = runBlocking {
  LogbackConfigurer.configureLoggingConsoleOnly(Level.INFO, Level.DEBUG)

  val accessToken = System.getenv("MATTERMOST_TOKEN").requireNotNull {
    "Set the MATTERMOST_TOKEN environment variable to the Mattermost API token"
  }

  val client = MattermostClient(
    mattermostUrl = Url.absolute("https://mattermost.neckar.it"),
    botToken = accessToken,
  )

  val channelId = ChannelId("zf46a8dzs3nq5ywbpjsz4z6e1a") //atlas-connect

  println("Bot user: ${client.getBotUserId()}")
  println("Bot team: ${client.getBotTeamId()}")
  println("Town square: ${client.getDefaultChannelId(client.getBotTeamId())}")

  val posts = client.getChannelPosts(channelId, since = now() - 10.minutes)
  println("Posts: ${posts.size}")

  posts.fastForEach { post ->
    println("----------------------------------------------------------")
    println(post.id)
    println("   by: ${client.getUserDisplayName(post.userId)}")
    println("   at: ${post.createAt.formatUtc()}")
    println()
    println(post.message)
  }
}
