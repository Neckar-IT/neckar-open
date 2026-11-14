import type { MeSessionConfig } from "./MeSessionConfig";

/**
 * Configuration for the `ProxyAuthProvider` — no Keycloak coordinates, because the login
 * happens at the middleware and `onGetMe` carries no token of its own.
 */
export type ProxyAuthConfig = MeSessionConfig;
