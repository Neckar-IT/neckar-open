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

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import it.neckar.contact.EmailAddress
import it.neckar.integrations.mattermost.rest.ChannelId
import it.neckar.integrations.mattermost.rest.ChannelName
import it.neckar.integrations.mattermost.rest.MattermostAttachment
import it.neckar.integrations.mattermost.rest.MattermostChannelMember
import it.neckar.integrations.mattermost.rest.MattermostChannelResponse
import it.neckar.integrations.mattermost.rest.MattermostChannelType
import it.neckar.integrations.mattermost.rest.MattermostCreateChannelRequest
import it.neckar.integrations.mattermost.rest.MattermostPost
import it.neckar.integrations.mattermost.rest.MattermostPostProps
import it.neckar.integrations.mattermost.rest.MattermostPostResponse
import it.neckar.integrations.mattermost.rest.MattermostPresenceStatus
import it.neckar.integrations.mattermost.rest.MattermostSearchPostsRequest
import it.neckar.integrations.mattermost.rest.MattermostSetStatusRequest
import it.neckar.integrations.mattermost.rest.MattermostStatusResponse
import it.neckar.integrations.mattermost.rest.MattermostTeamMemberResponse
import it.neckar.integrations.mattermost.rest.MattermostTeamResponse
import it.neckar.integrations.mattermost.rest.MattermostUpdatePostRequest
import it.neckar.integrations.mattermost.rest.MattermostUserResponse
import it.neckar.integrations.mattermost.rest.Post
import it.neckar.integrations.mattermost.rest.PostId
import it.neckar.integrations.mattermost.rest.PostsResponseParser
import it.neckar.integrations.mattermost.rest.TeamId
import it.neckar.integrations.mattermost.rest.TeamName
import it.neckar.integrations.mattermost.rest.UserId
import it.neckar.logging.Logger
import it.neckar.logging.LoggerDelegate
import it.neckar.open.http.Url
import it.neckar.open.time.now
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * HTTP client for the Mattermost REST API — the only place in the monorepo that knows it.
 */
class MattermostClient(
  val mattermostUrl: Url.Absolute,
  val botToken: String,
  /** Injectable so a test can drive the client with a mock engine. */
  engine: HttpClientEngine = CIO.create(),
  private val cacheTtl: Duration = 1.minutes,
) {
  private val httpClient = HttpClient(engine) {
    install(ContentNegotiation) {
      json(
        Json {
          ignoreUnknownKeys = true
          isLenient = true
          explicitNulls = false
        }
      )
    }
  }

  private val tokenCache = ConcurrentHashMap<String, CachedValue<UserId>>()

  private val displayNameCache = ConcurrentHashMap<UserId, CachedValue<String>>()

  /** A null value is a cached "nobody has this email" — a guest, asked about on every reading. */
  private val userIdByEmailCache = ConcurrentHashMap<EmailAddress, CachedValue<UserId?>>()

  suspend fun validateToken(sessionToken: String): UserId? {
    tokenCache.current(sessionToken)?.let { return it.value }

    val user = try {
      getUserOfToken(sessionToken)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logger.warn("Token validation failed: ${e.message}")
      return null
    }

    tokenCache.store(sessionToken, user.id)
    return user.id
  }

  suspend fun getUser(userId: UserId): MattermostUserResponse {
    return httpClient.get("$mattermostUrl/api/v4/users/$userId") {
      authorizedAsBot()
    }.body<MattermostUserResponse>()
  }

  /** The user the token belongs to — a session token names its owner, the bot token names the bot. */
  private suspend fun getUserOfToken(token: String): MattermostUserResponse {
    return httpClient.get("$mattermostUrl/api/v4/users/me") {
      header(HttpHeaders.Authorization, "Bearer $token")
    }.body<MattermostUserResponse>()
  }

  /**
   * Cached because the team board resolves a name per member on every poll (Decision 019).
   * A renamed user shows up with the next cache expiry.
   */
  suspend fun getUserDisplayName(userId: UserId): String? {
    displayNameCache.current(userId)?.let { return it.value }

    val displayName = try {
      getUser(userId).displayName()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logger.warn("Failed to get user display name for $userId: ${e.message}")
      return null
    }

    displayNameCache.store(userId, displayName)
    return displayName
  }

  /** Null is the answer for a guest who joined through a forwarded room link, not a failure. */
  suspend fun findUserIdByEmail(email: EmailAddress): UserId? {
    userIdByEmailCache.current(email)?.let { return it.value }

    val response = try {
      httpClient.get("$mattermostUrl/api/v4/users/email/${email.value.encodeURLPathPart()}") {
        authorizedAsBot()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logger.warn("Failed to resolve the Mattermost user of an email address: ${e.message}")
      return null
    }

    // The 404 goes into the cache as well, or every census reading asks again for every guest in
    // the room — and the census reads every room every 30 seconds.
    val userId: UserId? = when {
      response.status == HttpStatusCode.NotFound -> null
      response.status.isSuccess().not() -> {
        logger.warn("Resolving a Mattermost user by email answered ${response.status}")
        return null
      }

      else -> response.body<MattermostUserResponse>().id
    }

    userIdByEmailCache.store(email, userId)
    return userId
  }

  suspend fun getUserPresence(userId: UserId): MattermostPresenceStatus? {
    return try {
      httpClient.get("$mattermostUrl/api/v4/users/$userId/status") {
        authorizedAsBot()
      }.body<MattermostStatusResponse>().status
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logger.warn("Failed to get presence for $userId: ${e.message}")
      null
    }
  }

  suspend fun setUserStatus(userId: UserId, status: MattermostPresenceStatus) {
    try {
      httpClient.put("$mattermostUrl/api/v4/users/$userId/status") {
        authorizedAsBot()
        contentType(ContentType.Application.Json)
        setBody<MattermostSetStatusRequest>(MattermostSetStatusRequest(userId = userId, status = status))
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logger.warn("Failed to set status for $userId: ${e.message}")
    }
  }

  suspend fun postDirectMessage(userId: UserId, message: String) {
    try {
      createPost(getDirectChannelIdBetween(getBotUserId(), userId), message)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logger.warn("Failed to send DM to $userId: ${e.message}")
    }
  }

  suspend fun updatePost(postId: PostId, attachment: MattermostAttachment) {
    try {
      httpClient.put("$mattermostUrl/api/v4/posts/$postId") {
        authorizedAsBot()
        contentType(ContentType.Application.Json)
        setBody<MattermostUpdatePostRequest>(MattermostUpdatePostRequest(id = postId, message = "", attachments = listOf(attachment)))
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logger.warn("Failed to update post $postId: ${e.message}")
    }
  }

  /** The card goes into the direct channel between the two users, so it shows who is inviting. */
  suspend fun postAttachmentInDirectChannel(firstUser: UserId, secondUser: UserId, attachment: MattermostAttachment): PostId? {
    return try {
      createPostWithAttachment(getDirectChannelIdBetween(firstUser, secondUser), attachment)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logger.warn("Failed to post attachment in DM between $firstUser and $secondUser: ${e.message}")
      null
    }
  }

  private suspend fun getDirectChannelIdBetween(firstUser: UserId, secondUser: UserId): ChannelId {
    return httpClient.post("$mattermostUrl/api/v4/channels/direct") {
      authorizedAsBot()
      contentType(ContentType.Application.Json)
      setBody<List<UserId>>(listOf(firstUser, secondUser))
    }.body<MattermostChannelResponse>().id
  }

  suspend fun postChannelMessage(channelId: ChannelId, message: String) {
    try {
      createPost(channelId, message)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logger.warn("Failed to post channel message: ${e.message}")
    }
  }

  suspend fun postChannelMessageWithAttachment(channelId: ChannelId, attachment: MattermostAttachment): PostId? {
    return try {
      createPostWithAttachment(channelId, attachment)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logger.warn("Failed to post channel message with attachment: ${e.message}")
      null
    }
  }

  private suspend fun createPost(channelId: ChannelId, message: String) {
    httpClient.post("$mattermostUrl/api/v4/posts") {
      authorizedAsBot()
      contentType(ContentType.Application.Json)
      setBody<MattermostPost>(MattermostPost(channelId = channelId, message = message))
    }
  }

  /** The attachment carries the whole card, so the post itself has no message text. */
  private suspend fun createPostWithAttachment(channelId: ChannelId, attachment: MattermostAttachment): PostId {
    return httpClient.post("$mattermostUrl/api/v4/posts") {
      authorizedAsBot()
      contentType(ContentType.Application.Json)
      setBody<MattermostPost>(MattermostPost(channelId = channelId, message = "", props = MattermostPostProps(attachments = listOf(attachment))))
    }.body<MattermostPostResponse>().id
  }

  suspend fun getChannelIdByName(teamId: TeamId, channelName: ChannelName): ChannelId {
    return findChannelIdByName(teamId, channelName) ?: throw MattermostRequestFailedException("read channel $channelName of team $teamId", HttpStatusCode.NotFound, "no such channel")
  }

  /** The channel [channelName] of [teamId], or `null` when the team has no channel of that name. */
  suspend fun findChannelIdByName(teamId: TeamId, channelName: ChannelName): ChannelId? {
    val response: HttpResponse = httpClient.get("$mattermostUrl/api/v4/teams/$teamId/channels/name/$channelName") {
      authorizedAsBot()
    }
    if (response.status == HttpStatusCode.NotFound) {
      return null
    }
    return response.requireSuccess("read channel $channelName of team $teamId").body<MattermostChannelResponse>().id
  }

  suspend fun createChannel(teamId: TeamId, channelName: ChannelName, displayName: String, type: MattermostChannelType): ChannelId {
    return httpClient.post("$mattermostUrl/api/v4/channels") {
      authorizedAsBot()
      contentType(ContentType.Application.Json)
      setBody<MattermostCreateChannelRequest>(MattermostCreateChannelRequest(teamId = teamId, name = channelName, displayName = displayName, type = type))
    }.requireSuccess("create channel $channelName in team $teamId").body<MattermostChannelResponse>().id
  }

  suspend fun getTeamByName(teamName: TeamName): MattermostTeamResponse {
    return httpClient.get("$mattermostUrl/api/v4/teams/name/$teamName") {
      authorizedAsBot()
    }.requireSuccess("read team $teamName").body<MattermostTeamResponse>()
  }

  suspend fun getChannelMemberIds(channelId: ChannelId): List<UserId> {
    val members = mutableListOf<MattermostChannelMember>()
    var page = 0

    while (true) {
      val batch: List<MattermostChannelMember> = httpClient.get("$mattermostUrl/api/v4/channels/$channelId/members") {
        authorizedAsBot()
        parameter("page", page)
        parameter("per_page", MembersPerPage)
      }.requireSuccess("list the members of channel $channelId").body<List<MattermostChannelMember>>()

      members.addAll(batch)
      if (batch.size < MembersPerPage) break
      page++
    }

    return members.map { it.userId }
  }

  suspend fun addChannelMember(channelId: ChannelId, userId: UserId) {
    httpClient.post("$mattermostUrl/api/v4/channels/$channelId/members") {
      authorizedAsBot()
      contentType(ContentType.Application.Json)
      setBody<MattermostChannelMember>(MattermostChannelMember(userId = userId))
    }.requireSuccess("add $userId to channel $channelId")
  }

  /** Throws on every failure, unlike [postChannelMessage], which only logs it. */
  suspend fun createChannelPost(channelId: ChannelId, message: String): PostId {
    return httpClient.post("$mattermostUrl/api/v4/posts") {
      authorizedAsBot()
      contentType(ContentType.Application.Json)
      setBody<MattermostPost>(MattermostPost(channelId = channelId, message = message))
    }.requireSuccess("create a post in channel $channelId").body<MattermostPostResponse>().id
  }

  /**
   * The channels of [teamId] the token is a member of — direct and group channels included.
   *
   * This is the reach of [searchPosts] as well: a search answers from these channels and no others.
   */
  suspend fun getOwnChannels(teamId: TeamId): List<MattermostChannelResponse> {
    return httpClient.get("$mattermostUrl/api/v4/users/me/teams/$teamId/channels") {
      authorizedAsBot()
    }.body<List<MattermostChannelResponse>>()
  }

  /**
   * The posts matching [terms] across [teamIds], most recent first per team.
   *
   * [terms] is the string a user types into the Mattermost search box, operators and all. The
   * search covers the channels the token is a member of; a term shorter than three characters and
   * a stop word are dropped by Mattermost before the search runs.
   */
  suspend fun searchPosts(teamIds: List<TeamId>, terms: String, page: Int = 0, perPage: Int = 60): List<Post> {
    return teamIds.flatMap { teamId ->
      val response = httpClient.post("$mattermostUrl/api/v4/teams/$teamId/posts/search") {
        authorizedAsBot()
        contentType(ContentType.Application.Json)
        setBody<MattermostSearchPostsRequest>(MattermostSearchPostsRequest(terms = terms, page = page, perPage = perPage))
      }.bodyAsText()
      PostsResponseParser.parse(response)
    }
  }

  /** The whole thread [postId] belongs to — the root post and every reply, oldest first. */
  suspend fun getThread(postId: PostId): List<Post> {
    val response = httpClient.get("$mattermostUrl/api/v4/posts/$postId/thread") {
      authorizedAsBot()
    }.bodyAsText()
    return PostsResponseParser.parse(response).sortedBy { it.createAt }
  }

  /** One request for every name, so an output resolves its authors once instead of per post. */
  suspend fun getUsers(userIds: Collection<UserId>): List<MattermostUserResponse> {
    if (userIds.isEmpty()) return emptyList()

    return httpClient.post("$mattermostUrl/api/v4/users/ids") {
      authorizedAsBot()
      contentType(ContentType.Application.Json)
      setBody<List<UserId>>(userIds.toList())
    }.body<List<MattermostUserResponse>>()
  }

  /** In the order of the `order` array Mattermost answers with. */
  suspend fun getChannelPosts(channelId: ChannelId, since: Instant? = null): List<Post> {
    val response = httpClient.get("$mattermostUrl/api/v4/channels/$channelId/posts") {
      authorizedAsBot()
      since?.let { parameter("since", it.toEpochMilliseconds()) }
    }.bodyAsText()
    return PostsResponseParser.parse(response)
  }

  private val defaultChannelIdByTeam = ConcurrentHashMap<TeamId, ChannelId>()

  suspend fun getDefaultChannelId(teamId: TeamId): ChannelId {
    defaultChannelIdByTeam[teamId]?.let { return it }
    return getChannelIdByName(teamId, ChannelName.TownSquare).also { defaultChannelIdByTeam[teamId] = it }
  }

  private var cachedBotUserId: UserId? = null

  suspend fun getBotUserId(): UserId {
    cachedBotUserId?.let { return it }
    return getUserOfToken(botToken).id.also { cachedBotUserId = it }
  }

  private var cachedBotTeamId: TeamId? = null

  /** The first of the bot's teams — the hub runs against a single Mattermost team. */
  suspend fun getBotTeamId(): TeamId {
    cachedBotTeamId?.let { return it }
    val teamId = getTeams().firstOrNull()?.id
      ?: error("Bot is not a member of any team")
    return teamId.also { cachedBotTeamId = it }
  }

  /** The teams the token is a member of — the scope every search runs in. */
  suspend fun getTeams(): List<MattermostTeamResponse> {
    return httpClient.get("$mattermostUrl/api/v4/users/me/teams") {
      authorizedAsBot()
    }.body<List<MattermostTeamResponse>>()
  }

  /** Bots, deactivated accounts and the excluded usernames are left out. */
  suspend fun getTeamMembers(teamId: TeamId, excludedUsernames: Set<String> = emptySet()): List<UserId> {
    val allMembers = mutableListOf<MattermostTeamMemberResponse>()
    var page = 0

    while (true) {
      val batch = httpClient.get("$mattermostUrl/api/v4/teams/$teamId/members") {
        authorizedAsBot()
        parameter("page", page)
        parameter("per_page", MembersPerPage)
      }.body<List<MattermostTeamMemberResponse>>()

      allMembers.addAll(batch)
      if (batch.size < MembersPerPage) break
      page++
    }

    val humanUserIds = mutableListOf<UserId>()
    for (member in allMembers) {
      val user = try {
        getUser(member.userId)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        logger.warn("Failed to fetch user ${member.userId} during team sync: ${e.message}")
        continue
      }

      if (user.isActiveHuman && user.username !in excludedUsernames) {
        humanUserIds.add(user.id)
      }
    }

    return humanUserIds
  }

  @IgnorableReturnValue
  private suspend fun HttpResponse.requireSuccess(action: String): HttpResponse {
    if (status.isSuccess().not()) {
      throw MattermostRequestFailedException(action, status, bodyAsText())
    }
    return this
  }

  private fun HttpRequestBuilder.authorizedAsBot() {
    header(HttpHeaders.Authorization, "Bearer $botToken")
  }

  private fun <K : Any, V> ConcurrentHashMap<K, CachedValue<V>>.current(key: K): CachedValue<V>? {
    return this[key]?.takeIf { it.isExpired().not() }
  }

  /**
   * Storing drops what expired: a session token seen once would otherwise stay in the map for the
   * lifetime of the process, and the hub validates a token per request.
   */
  private fun <K : Any, V> ConcurrentHashMap<K, CachedValue<V>>.store(key: K, value: V) {
    values.removeIf { it.isExpired() }
    this[key] = CachedValue(value, now())
  }

  private inner class CachedValue<T>(
    val value: T,
    val cachedAt: Instant,
  ) {
    fun isExpired(): Boolean = now() - cachedAt > cacheTtl
  }

  companion object {
    private val logger: Logger by LoggerDelegate()

    /** The largest page Mattermost answers a member list with. */
    private const val MembersPerPage: Int = 200
  }
}
