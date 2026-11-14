# Mattermost Client API

```kotlin
val client = MattermostClient(mattermostUrl = Url.absolute("https://mattermost.neckar.it"), botToken = botToken)
val teamId = client.getBotTeamId()
client.postChannelMessage(client.getDefaultChannelId(teamId), "Standup in five minutes")
```

`MattermostClient` is the only place in the monorepo that knows the Mattermost REST API. Every id it takes and
returns is a value class — `UserId`, `ChannelId`, `PostId`, `TeamId` — and the wire models live in
`it.neckar.integrations.mattermost.rest`, one per Mattermost response.

Session tokens, display names and the user behind an email address are cached for `cacheTtl`, one minute by default.

`MattermostDemo` in `src/jvmTest/` runs the client against the live API.
