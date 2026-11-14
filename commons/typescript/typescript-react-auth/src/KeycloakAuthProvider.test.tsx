/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { KeycloakInitTimeoutMs } from "./keycloak/keycloakTimeouts";
import { getKeycloakAccessToken } from "./keycloak/keycloakTokenStore";
import type { KeycloakAuthConfig } from "./KeycloakAuthConfig";
import { KeycloakAuthProvider } from "./KeycloakAuthProvider";

/**
 * A Keycloak double whose `init()` by default never settles — the shape keycloak-js takes when the
 * silent-check-sso iframe never posts back. That happens whenever Keycloak renders a page
 * instead of redirecting to `silentCheckSsoRedirectUri`: an unknown `client_id`, an
 * unregistered `redirect_uri`, or a realm that is down. The rendered page carries
 * `frame-ancestors 'self'`, the browser blocks it inside the hidden iframe, and no message
 * ever arrives.
 */
const keycloakInit = vi.fn(() => new Promise<boolean>(() => {}));
const login = vi.fn(() => Promise.resolve());
const { reportClientError } = vi.hoisted(() => ({ reportClientError: vi.fn() }));

vi.mock("@neckar-hub/typescript-otel-tracing", () => ({ reportClientError }));

vi.mock("keycloak-js", () => ({
  default: class {
    init = keycloakInit;
    login = login;
  },
}));

const config: KeycloakAuthConfig = {
  url: "https://auth.neckar.it",
  realm: "internal",
  clientId: "huddle.neckar.it",
  onGetMe: () => Promise.resolve({ userName: "irrelevant" }),
} as unknown as KeycloakAuthConfig;

/**
 * Tests
 */

describe("KeycloakAuthProvider with a failing Keycloak init", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    keycloakInit.mockClear();
    login.mockClear();
    reportClientError.mockClear();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("shows the loading indicator while the init is still within its budget", async () => {
    render(
      <I18nProvider>
        <KeycloakAuthProvider config={config}>
          <div>app content</div>
        </KeycloakAuthProvider>
      </I18nProvider>,
    );

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1_000);
    });

    expect(screen.getByText("Authenticating ...")).toBeInTheDocument();
  });

  it("falls back to the auth-service-not-reachable screen instead of loading forever", async () => {
    // Without a timeout the provider stays on "Authenticating ..." indefinitely: keycloak-js
    // never rejects, so the existing catch branch is unreachable and the user gets a spinner
    // with no explanation. Observed live on https://huddle.neckar.it while the Keycloak client
    // `huddle.neckar.it` was missing from realm `internal`.
    render(
      <I18nProvider>
        <KeycloakAuthProvider config={config}>
          <div>app content</div>
        </KeycloakAuthProvider>
      </I18nProvider>,
    );

    await act(async () => {
      await vi.advanceTimersByTimeAsync(60_000);
    });

    expect(screen.queryByText("Authenticating ...")).not.toBeInTheDocument();
    expect(screen.queryByText("app content")).not.toBeInTheDocument();
    expect(screen.getByText("Authentication service not reachable")).toBeInTheDocument();
  });

  it("releases requests waiting for a token once it gives up on the init", async () => {
    // Every request goes through the Keycloak Axios interceptor, unauthenticated endpoints
    // included. While the provider gave up but never reported that outcome, those requests kept
    // waiting for a token that would never arrive and then failed — which showed a heartbeat poll
    // as "server unreachable" although the backend was answering.
    render(
      <I18nProvider>
        <KeycloakAuthProvider config={config}>
          <div>app content</div>
        </KeycloakAuthProvider>
      </I18nProvider>,
    );

    const tokenPromise = getKeycloakAccessToken();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(KeycloakInitTimeoutMs);
    });

    await expect(tokenPromise).resolves.toBeNull();
  });

  it("reports the init timeout as AuthInitFailed", async () => {
    render(
      <I18nProvider>
        <KeycloakAuthProvider config={config}>
          <div>app content</div>
        </KeycloakAuthProvider>
      </I18nProvider>,
    );

    await act(async () => {
      await vi.advanceTimersByTimeAsync(KeycloakInitTimeoutMs);
    });

    expect(reportClientError).toHaveBeenCalledExactlyOnceWith("AuthInitFailed", new Error("keycloak.init timed out after 20000 ms"));
  });

  it("reports a rejected init as AuthInitFailed", async () => {
    keycloakInit.mockImplementationOnce(() => Promise.reject(new Error("realm internal not found")));

    render(
      <I18nProvider>
        <KeycloakAuthProvider config={config}>
          <div>app content</div>
        </KeycloakAuthProvider>
      </I18nProvider>,
    );

    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });

    expect(reportClientError).toHaveBeenCalledExactlyOnceWith("AuthInitFailed", new Error("realm internal not found"));
  });
});
