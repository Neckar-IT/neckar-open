import type Keycloak from "keycloak-js";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { KeycloakInitTimeoutMs, KeycloakReadyTimeoutMs } from "./keycloakTimeouts";
import type * as KeycloakTokenStoreModule from "./keycloakTokenStore";

function createMockKeycloak(token: string = "test-token") {
  const updateToken = vi.fn().mockResolvedValue(true);
  const login = vi.fn();
  const keycloak = { token, updateToken, login } as unknown as Keycloak;
  return { keycloak, updateToken, login };
}

async function importFreshStore() {
  vi.resetModules();
  // @ts-expect-error -- dynamic import needs .ts extension for Vitest module reset
  return import("./keycloakTokenStore.ts") as Promise<typeof KeycloakTokenStoreModule>;
}

describe("keycloakTokenStore", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("waits for setKeycloakInstance and returns the token", async () => {
    const store = await importFreshStore();
    const { keycloak } = createMockKeycloak("my-token");

    const tokenPromise = store.getKeycloakAccessToken();
    store.setKeycloakInstance(keycloak);

    expect(await tokenPromise).toBe("my-token");
  });

  it("returns the token immediately when already initialized", async () => {
    const store = await importFreshStore();
    const { keycloak } = createMockKeycloak("my-token");

    store.setKeycloakInstance(keycloak);

    expect(await store.getKeycloakAccessToken()).toBe("my-token");
  });

  it("refreshes the token before returning", async () => {
    const store = await importFreshStore();
    const { keycloak, updateToken } = createMockKeycloak();

    store.setKeycloakInstance(keycloak);
    await store.getKeycloakAccessToken();

    expect(updateToken).toHaveBeenCalledWith(30);
  });

  it("keeps waiting while the provider is still inside its init budget", async () => {
    const store = await importFreshStore();

    const tokenPromise = store.getKeycloakAccessToken();
    let settled = false;
    void tokenPromise.then(
      () => (settled = true),
      () => (settled = true),
    );
    vi.advanceTimersByTime(KeycloakInitTimeoutMs);
    await Promise.resolve();

    expect(settled).toBe(false);

    // Keep the rejection handled so the run does not report an unhandled rejection.
    store.markKeycloakSessionUnavailable();
    await tokenPromise;
  });

  it("throws when no outcome arrives at all", async () => {
    const store = await importFreshStore();

    const tokenPromise = store.getKeycloakAccessToken();
    vi.advanceTimersByTime(KeycloakReadyTimeoutMs);

    await expect(tokenPromise).rejects.toThrow("Keycloak initialization timed out");
  });

  it("resolves a waiting request with null once initialization ends without a session", async () => {
    const store = await importFreshStore();

    const tokenPromise = store.getKeycloakAccessToken();
    store.markKeycloakSessionUnavailable();

    expect(await tokenPromise).toBeNull();
  });

  it("returns null without waiting once initialization ended without a session", async () => {
    const store = await importFreshStore();
    store.markKeycloakSessionUnavailable();

    // No timer is advanced: a request that still waited would never settle here.
    expect(await store.getKeycloakAccessToken()).toBeNull();
  });

  it("returns the token when a session arrives after an earlier unavailable signal", async () => {
    const store = await importFreshStore();
    const { keycloak } = createMockKeycloak("my-token");

    store.markKeycloakSessionUnavailable();
    store.setKeycloakInstance(keycloak);

    expect(await store.getKeycloakAccessToken()).toBe("my-token");
  });

  it("redirects to login when token refresh fails", async () => {
    const store = await importFreshStore();
    const { keycloak, updateToken, login } = createMockKeycloak();
    updateToken.mockRejectedValue(new Error("refresh failed"));

    store.setKeycloakInstance(keycloak);

    const token = await store.getKeycloakAccessToken();
    expect(token).toBeNull();
    expect(login).toHaveBeenCalled();
  });

  it("returns null without a login redirect when a background refresh fails", async () => {
    const store = await importFreshStore();
    const { keycloak, updateToken, login } = createMockKeycloak();
    updateToken.mockRejectedValue(new Error("refresh failed"));

    store.setKeycloakInstance(keycloak);

    expect(await store.getKeycloakAccessTokenWithoutLogin()).toBeNull();
    expect(login).not.toHaveBeenCalled();
  });

  it("returns the refreshed token for a background request", async () => {
    const store = await importFreshStore();
    const { keycloak, updateToken } = createMockKeycloak("my-token");

    store.setKeycloakInstance(keycloak);

    expect(await store.getKeycloakAccessTokenWithoutLogin()).toBe("my-token");
    expect(updateToken).toHaveBeenCalledWith(30);
  });
});
