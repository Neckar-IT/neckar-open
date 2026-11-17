# CORS

## Cors-Fehler Debuggen mit Ktor

- Breakpoint setzen in `io.ktor.server.plugins.cors.CORS:onCall`
- Durchsteppen bis man an die Stelle kommt bei der 403 returned wird.

