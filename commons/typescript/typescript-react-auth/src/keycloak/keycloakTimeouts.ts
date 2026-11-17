/**
 * How long `Keycloak.init()` may take before [KeycloakAuthProvider] gives up and shows
 * `AuthServiceNotReachableScreen`.
 *
 * keycloak-js resolves the `check-sso` init only once the hidden silent-check-sso iframe posts a
 * message back. When Keycloak answers the iframe request with a rendered page instead of a redirect
 * to `silentCheckSsoRedirectUri` — an unknown `client_id`, an unregistered `redirect_uri`, a realm
 * that is down — that page carries `frame-ancestors 'self'`, the browser blocks it, and no message
 * is ever posted. keycloak-js neither resolves nor rejects, so without this timeout the app stays on
 * the loading indicator forever and the user never learns why.
 */
export const KeycloakInitTimeoutMs = 20_000;

/**
 * How long the token lookups of `keycloakTokenStore` wait for the provider's outcome before giving up on a provider
 * that never mounted. Must stay above [KeycloakInitTimeoutMs] — the provider reports its outcome
 * only once its own init budget is spent, so a shorter wait fails requests mid-initialization. Must stay below the 45 s
 * export timeout of `initializeOtel`, whose abort timer already runs while `keycloakOtlpHeaders` waits here.
 */
export const KeycloakReadyTimeoutMs = 30_000;
