import { expect, type Page } from "@playwright/test";

/** The username and password the sign-in form of Keycloak receives. */
interface KeycloakCredentials {
  username: string;
  password: string;
}

/** Every host of the local stack ends on it. */
const LocalHostSuffix = ".localdev";

/** Signs in every app of the local stack. */
const LocalKeycloakOrigin = "https://auth.localdev";

/** Signs in every deployed app. */
const ProductionKeycloakOrigin = "https://auth.neckar.it";

/** The test user each realm of the local Keycloak holds: `LocalKeycloak.TestUsername` and `TestPassword` of the build logic. */
const LocalTestUser: KeycloakCredentials = { username: "dev@auth.localdev", password: "dev-password" };

/**
 * Signs the user of [keycloakCredentials] in at Keycloak, to which the app at [baseUrl] leads every page without session,
 * and waits for the app's `h1`.
 */
export async function signInAtKeycloak(page: Page, baseUrl: string): Promise<void> {
  await submitKeycloakSignIn(page, baseUrl);
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible({ timeout: 10000 });
}

/** [signInAtKeycloak] up to the return to the app at [baseUrl], for an app whose first page has no `h1`. */
export async function submitKeycloakSignIn(page: Page, baseUrl: string): Promise<void> {
  const credentials = keycloakCredentials(baseUrl);

  await page.goto(`${baseUrl}/`);
  await page.waitForURL(keycloakOriginPattern(baseUrl), { timeout: 10000 });

  // IDs keep the form locale-independent.
  await page.locator("#username").fill(credentials.username);
  await page.locator("#password").fill(credentials.password);
  await page.locator("#kc-login").click();

  await page.waitForURL(baseOriginPattern(baseUrl), { timeout: 15000 });
}

/** The origin of the Keycloak that signs in the app at [baseUrl]: the local Keycloak for a host of the local stack. */
function keycloakOrigin(baseUrl: string): string {
  return isLocalTarget(baseUrl) ? LocalKeycloakOrigin : ProductionKeycloakOrigin;
}

/**
 * The test user of the local Keycloak for a local target; a production target requires `PLAYWRIGHT_KEYCLOAK_USER` and
 * `PLAYWRIGHT_KEYCLOAK_PASSWORD`, which name an account the local Keycloak does not hold.
 */
function keycloakCredentials(baseUrl: string): KeycloakCredentials {
  if (isLocalTarget(baseUrl)) {
    return LocalTestUser;
  }

  const username = process.env.PLAYWRIGHT_KEYCLOAK_USER;
  const password = process.env.PLAYWRIGHT_KEYCLOAK_PASSWORD;

  if (!username || !password) {
    throw new Error(
      "PLAYWRIGHT_KEYCLOAK_USER and PLAYWRIGHT_KEYCLOAK_PASSWORD environment variables are required " +
        `to sign in at ${ProductionKeycloakOrigin}.\n` +
        "Configure them in the project root .env file. See .env.example for setup instructions.",
    );
  }

  return { username, password };
}

/** Anchored on the whole origin: the Keycloak page carries the app's host inside its redirect_uri. */
export function baseOriginPattern(baseUrl: string): RegExp {
  return originPattern(new URL(baseUrl).origin);
}

/** Matches every page of the Keycloak that signs in the app at [baseUrl]. */
export function keycloakOriginPattern(baseUrl: string): RegExp {
  return originPattern(keycloakOrigin(baseUrl));
}

function isLocalTarget(baseUrl: string): boolean {
  return new URL(baseUrl).hostname.endsWith(LocalHostSuffix);
}

function originPattern(origin: string): RegExp {
  return new RegExp(`^${origin.replace(/[.:/]/g, (character) => `\\${character}`)}/`);
}
