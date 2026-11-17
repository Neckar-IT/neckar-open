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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import it.neckar.contact.EmailAddress
import it.neckar.integrations.mattermost.rest.ChannelId
import it.neckar.integrations.mattermost.rest.ChannelName
import it.neckar.integrations.mattermost.rest.MattermostAttachment
import it.neckar.integrations.mattermost.rest.MattermostChannelType
import it.neckar.integrations.mattermost.rest.MattermostPresenceStatus
import it.neckar.integrations.mattermost.rest.PostId
import it.neckar.integrations.mattermost.rest.TeamId
import it.neckar.integrations.mattermost.rest.TeamName
import it.neckar.integrations.mattermost.rest.UserId
import it.neckar.open.http.Url
import it.neckar.open.resources.getResourceSafe
import it.neckar.open.test.utils.WithVirtualTime
import it.neckar.open.time.VirtualNowProvider
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * What the client sends, and what it answers without sending anything: the team board asks for a name
 * per member on every poll (Decision 019), the census for the user behind an email of every occupant.
 */
class MattermostClientTest {

  @Test
  suspend fun `a repeated display name lookup is served from the cache`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWithUser(requests)

    assertThat(client.getUserDisplayName(UserId("user-alice"))).isEqualTo("Nickname of user-alice")
    assertThat(client.getUserDisplayName(UserId("user-alice"))).isEqualTo("Nickname of user-alice")

    assertThat(requests.size).isEqualTo(1)
  }

  @Test
  suspend fun `each user is looked up once`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWithUser(requests)

    assertThat(client.getUserDisplayName(UserId("user-alice"))).isEqualTo("Nickname of user-alice")
    assertThat(client.getUserDisplayName(UserId("user-bob"))).isEqualTo("Nickname of user-bob")
    assertThat(client.getUserDisplayName(UserId("user-alice"))).isEqualTo("Nickname of user-alice")

    assertThat(requests.size).isEqualTo(2)
  }

  @Test
  @WithVirtualTime
  suspend fun `a display name is looked up again once its cache entry expired`(nowProvider: VirtualNowProvider) {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWithUser(requests, cacheTtl = 1.minutes)

    assertThat(client.getUserDisplayName(UserId("user-alice"))).isEqualTo("Nickname of user-alice")
    nowProvider.advanceBy(2.minutes.inWholeMilliseconds.toDouble())
    assertThat(client.getUserDisplayName(UserId("user-alice"))).isEqualTo("Nickname of user-alice")

    assertThat(requests.size).isEqualTo(2)
  }

  @Test
  suspend fun `an unresolvable user is null instead of an exception`() {
    val client = clientRespondingWith { respondNotFound() }

    assertThat(client.getUserDisplayName(UserId("user-ghost"))).isNull()
  }

  @Test
  suspend fun `a repeated token validation is served from the cache`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) { respondJson("""{"id":"user-alice","username":"alice"}""") }

    assertThat(client.validateToken("session-token")).isEqualTo(UserId("user-alice"))
    assertThat(client.validateToken("session-token")).isEqualTo(UserId("user-alice"))

    assertThat(requests.size).isEqualTo(1)
    assertThat(requests.single().headers[HttpHeaders.Authorization]).isEqualTo("Bearer session-token")
  }

  @Test
  suspend fun `an email nobody has is cached as null and asked once`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) { respondNotFound() }

    assertThat(client.findUserIdByEmail(EmailAddress("guest@example.com"))).isNull()
    assertThat(client.findUserIdByEmail(EmailAddress("guest@example.com"))).isNull()

    assertThat(requests.size).isEqualTo(1)
  }

  @Test
  suspend fun `an email of a colleague resolves to their user id`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) { respondJson("""{"id":"user-alice","username":"alice"}""") }

    assertThat(client.findUserIdByEmail(EmailAddress("alice@neckar.it"))).isEqualTo(UserId("user-alice"))

    assertThat(requests.single().url.encodedPath).isEqualTo("/api/v4/users/email/alice@neckar.it")
  }

  @Test
  suspend fun `the presence of a user is read from their status`() {
    val client = clientRespondingWith { respondJson("""{"user_id":"user-alice","status":"away"}""") }

    assertThat(client.getUserPresence(UserId("user-alice"))).isEqualTo(MattermostPresenceStatus.Away)
  }

  @Test
  suspend fun `setting a status sends the value Mattermost expects`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) { respondJson("""{"user_id":"user-alice","status":"dnd"}""") }

    client.setUserStatus(UserId("user-alice"), MattermostPresenceStatus.Dnd)

    val request = requests.single()
    assertThat(request.method).isEqualTo(HttpMethod.Put)
    assertThat(request.url.encodedPath).isEqualTo("/api/v4/users/user-alice/status")
    assertThat((request.body as TextContent).text).contains(""""status":"dnd"""")
  }

  @Test
  suspend fun `a created post answers with its post id`() {
    val client = clientRespondingWith { respondJson("""{"id":"post-1","channel_id":"channel-1","message":""}""") }

    val postId = client.postChannelMessageWithAttachment(ChannelId("channel-1"), MattermostAttachment(title = "Standup"))

    assertThat(postId).isEqualTo(PostId("post-1"))
  }

  @Test
  suspend fun `an updated post is addressed by its id`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) { respondJson("""{"id":"post-1","channel_id":"channel-1","message":""}""") }

    client.updatePost(PostId("post-1"), MattermostAttachment(title = "Meeting Ended"))

    assertThat(requests.single().url.encodedPath).isEqualTo("/api/v4/posts/post-1")
  }

  @Test
  suspend fun `a post with an attachment sends only the set fields`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) { respondJson("""{"id":"post-1","channel_id":"channel-1","message":""}""") }

    val _ = client.postChannelMessageWithAttachment(
      ChannelId("channel-1"),
      MattermostAttachment(color = "#2389d7", title = "Huddle 09:30", text = "Einladung", footer = "huddle.neckar.it"),
    )

    assertThat((requests.single().body as TextContent).text).isEqualTo(
      """{"channel_id":"channel-1","message":"","props":{"attachments":[{"color":"#2389d7","title":"Huddle 09:30","text":"Einladung","footer":"huddle.neckar.it"}]}}"""
    )
  }

  @Test
  suspend fun `an updated post sends its id, an empty message and the attachment`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) { respondJson("""{"id":"post-1","channel_id":"channel-1","message":""}""") }

    client.updatePost(PostId("post-1"), MattermostAttachment(title = "Meeting Ended"))

    assertThat((requests.single().body as TextContent).text).isEqualTo(
      """{"id":"post-1","message":"","props":{"attachments":[{"title":"Meeting Ended"}]}}"""
    )
  }

  @Test
  suspend fun `a channel message sends the channel and the message`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) { respondJson("""{"id":"post-1","channel_id":"channel-1","message":"Hello"}""") }

    client.postChannelMessage(ChannelId("channel-1"), "Hello")

    assertThat((requests.single().body as TextContent).text).isEqualTo("""{"channel_id":"channel-1","message":"Hello"}""")
  }

  @Test
  suspend fun `the town square channel is resolved per team`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) { request ->
      val teamId = request.url.encodedPath.substringAfter("/teams/").substringBefore("/")
      respondJson("""{"id":"channel-of-$teamId","name":"town-square","display_name":"Town Square","team_id":"$teamId"}""")
    }

    assertThat(client.getDefaultChannelId(TeamId("team-one"))).isEqualTo(ChannelId("channel-of-team-one"))
    assertThat(client.getDefaultChannelId(TeamId("team-two"))).isEqualTo(ChannelId("channel-of-team-two"))
    assertThat(client.getDefaultChannelId(TeamId("team-one"))).isEqualTo(ChannelId("channel-of-team-one"))

    assertThat(requests.size).isEqualTo(2)
  }

  @Test
  suspend fun `bots, deactivated accounts and excluded usernames are no team members`() {
    val members = """[{"user_id":"user-alice"},{"user_id":"user-bot"},{"user_id":"user-gone"},{"user_id":"user-admin"}]"""
    val users = mapOf(
      "user-alice" to """{"id":"user-alice","username":"alice","first_name":"Alice","last_name":"Schneider"}""",
      "user-bot" to """{"id":"user-bot","username":"huddlebot","first_name":"Huddle","last_name":"Bot","is_bot":true}""",
      "user-gone" to """{"id":"user-gone","username":"gone","first_name":"Gerd","last_name":"Gone","delete_at":1700000000000}""",
      "user-admin" to """{"id":"user-admin","username":"admin","first_name":"Ada","last_name":"Admin"}""",
    )
    val client = clientRespondingWith { request ->
      val path = request.url.encodedPath
      when {
        path.endsWith("/members") -> respondJson(members)
        else -> respondJson(users.getValue(path.substringAfterLast('/')))
      }
    }

    assertThat(client.getTeamMembers(TeamId("team-one"), excludedUsernames = setOf("admin")))
      .containsExactly(UserId("user-alice"))
  }

  @Test
  suspend fun `a search answers with the hits of every team, newest first`() {
    val searchResponse = javaClass.getResourceSafe("/it/neckar/integrations/mattermost/search-posts-response.json").readText()
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) { respondJson(searchResponse) }

    val hits = client.searchPosts(listOf(TeamId("team-one")), terms = "deploy in:standup")

    assertThat(hits.map { it.id }).containsExactly(PostId("post-standup"), PostId("post-deploy"))
    assertThat(hits.first().channelId).isEqualTo(ChannelId("channel-standup"))
    assertThat(hits.first().userId).isEqualTo(UserId("user-bob"))
    assertThat(hits.first().message).isEqualTo("Standup: the deploy pipeline is green again.")

    val request = requests.single()
    assertThat(request.method).isEqualTo(HttpMethod.Post)
    assertThat(request.url.encodedPath).isEqualTo("/api/v4/teams/team-one/posts/search")
    assertThat((request.body as TextContent).text).contains(""""terms":"deploy in:standup"""")
  }

  @Test
  suspend fun `a thread is answered oldest first, whatever order Mattermost sends`() {
    //language=JSON
    val threadResponse = """
      {
        "order": ["post-reply", "post-root"],
        "posts": {
          "post-root": {"create_at": 1000, "user_id": "user-alice", "channel_id": "channel-dev", "message": "Who deploys?"},
          "post-reply": {"create_at": 2000, "user_id": "user-bob", "channel_id": "channel-dev", "message": "I do."}
        }
      }
    """.trimIndent()
    val client = clientRespondingWith { respondJson(threadResponse) }

    assertThat(client.getThread(PostId("post-root")).map { it.id }).containsExactly(PostId("post-root"), PostId("post-reply"))
  }

  @Test
  suspend fun `the display names of a whole output are asked for in one request`() {
    val requests = mutableListOf<HttpRequestData>()
    val users = """[{"id":"user-alice","username":"alice","nickname":"Alice"},{"id":"user-bob","username":"bob","nickname":"Bob"}]"""
    val client = clientRespondingWith(requests) { respondJson(users) }

    assertThat(client.getUsers(listOf(UserId("user-alice"), UserId("user-bob"))).map { it.displayName() })
      .containsExactly("Alice", "Bob")

    assertThat(requests.single().url.encodedPath).isEqualTo("/api/v4/users/ids")
  }

  @Test
  suspend fun `a missing channel is absent`() {
    val client = clientRespondingWith { respondNotFound() }

    assertThat(client.findChannelIdByName(TeamId("team-developers"), ChannelName("daily"))).isNull()
  }

  @Test
  suspend fun `a channel is created open in its team`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) {
      respondJson("""{"id":"channel-daily","name":"daily","display_name":"Daily","team_id":"team-developers"}""")
    }

    assertThat(client.createChannel(TeamId("team-developers"), ChannelName("daily"), "Daily", MattermostChannelType.Open)).isEqualTo(ChannelId("channel-daily"))

    assertThat(requests.single().url.encodedPath).isEqualTo("/api/v4/channels")
    assertThat((requests.single().body as TextContent).text)
      .isEqualTo("""{"team_id":"team-developers","name":"daily","display_name":"Daily","type":"O"}""")
  }

  @Test
  suspend fun `the channel members are read page by page`() {
    val requests = mutableListOf<HttpRequestData>()
    val fullPage: String = (1..200).joinToString(",", "[", "]") { """{"user_id":"user-$it"}""" }
    val client = clientRespondingWith(requests) { request ->
      when (request.url.parameters["page"]) {
        "0" -> respondJson(fullPage)
        else -> respondJson("""[{"user_id":"user-201"}]""")
      }
    }

    assertThat(client.getChannelMemberIds(ChannelId("channel-daily")).size).isEqualTo(201)
    assertThat(requests.map { it.url.parameters["page"] }).containsExactly("0", "1")
  }

  @Test
  suspend fun `a refused channel post throws with the status`() {
    val client = clientRespondingWith {
      respond(content = """{"message":"no permission"}""", status = HttpStatusCode.Forbidden, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }

    val failure = assertFailure { client.createChannelPost(ChannelId("channel-daily"), "Daily") }

    failure.isInstanceOf(MattermostRequestFailedException::class).transform { it.status }.isEqualTo(HttpStatusCode.Forbidden)
  }

  @Test
  suspend fun `a channel post answers with the id of the post`() {
    val client = clientRespondingWith {
      respondJson("""{"id":"post-1","channel_id":"channel-daily","message":"Daily"}""")
    }

    assertThat(client.createChannelPost(ChannelId("channel-daily"), "Daily")).isEqualTo(PostId("post-1"))
  }

  @Test
  suspend fun `a team is read by its name`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) { respondJson("""{"id":"team-developers","name":"developers"}""") }

    assertThat(client.getTeamByName(TeamName("developers")).id).isEqualTo(TeamId("team-developers"))
    assertThat(requests.single().url.encodedPath).isEqualTo("/api/v4/teams/name/developers")
  }

  @Test
  suspend fun `no user to resolve sends no request`() {
    val requests = mutableListOf<HttpRequestData>()
    val client = clientRespondingWith(requests) { respondJson("[]") }

    assertThat(client.getUsers(emptyList())).isEmpty()

    assertThat(requests).isEmpty()
  }

  private fun MockRequestHandleScope.respondJson(content: String): HttpResponseData {
    return respond(content = content, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
  }

  private fun MockRequestHandleScope.respondNotFound(): HttpResponseData {
    return respond(content = "Not Found", status = HttpStatusCode.NotFound, headers = headersOf(HttpHeaders.ContentType, "text/plain"))
  }

  private fun clientRespondingWith(
    requests: MutableList<HttpRequestData> = mutableListOf(),
    cacheTtl: Duration = 1.minutes,
    respondTo: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
  ): MattermostClient {
    val engine = MockEngine { request ->
      requests.add(request)
      respondTo(request)
    }
    return MattermostClient(mattermostUrl = Url.absolute("http://mattermost"), botToken = "bot-token", engine = engine, cacheTtl = cacheTtl)
  }

  /** Answers every user lookup with the requested id, so a wrong id shows up as a wrong nickname. */
  private fun clientRespondingWithUser(requests: MutableList<HttpRequestData>, cacheTtl: Duration = 1.minutes): MattermostClient {
    return clientRespondingWith(requests, cacheTtl) { request ->
      val userId = request.url.encodedPath.substringAfterLast('/')
      respondJson("""{"id":"$userId","username":"$userId","nickname":"Nickname of $userId"}""")
    }
  }
}
