# Authentication Debugging

## Falls man unerklärliche 401-Fehler bekommt

- Breakpoint setzen in `io.ktor.server.auth.jwt.JWTAuthenticationProvider.onAuthenticate`
- Logging Debug Level für `io.ktor.auth.jwt` setzen
- Logging Debug Level für `io.ktor.server.auth` setzen

- Debugging `com.auth0.jwt.JWTVerifier.verify(com.auth0.jwt.interfaces.DecodedJWT)`
- Debugging `com.auth0.jwt.JWTVerifier.verifyClaims`
