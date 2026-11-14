import axios, { type InternalAxiosRequestConfig } from "axios";
import type Keycloak from "keycloak-js";
import { describe, expect, it, vi } from "vitest";
import { setKeycloakInstance } from "./keycloakTokenStore";
import { registerKeycloakAxiosInterceptor } from "./registerKeycloakAxiosInterceptor";

/**
 * An axios instance with the interceptor whose adapter records the Authorization header of each request.
 */
function createRecordingInstance(): { send: (backgroundRequest: boolean) => Promise<unknown>; authorizations: (string | undefined)[] } {
  const authorizations: (string | undefined)[] = [];
  const instance = axios.create({
    adapter: (config: InternalAxiosRequestConfig) => {
      authorizations.push(config.headers.get("Authorization")?.toString());
      return Promise.resolve({ data: {}, status: 200, statusText: "OK", headers: {}, config });
    },
  });
  registerKeycloakAxiosInterceptor(instance);
  return { send: (backgroundRequest) => instance.get("/api/huddle/health", { backgroundRequest }), authorizations };
}

describe("registerKeycloakAxiosInterceptor", () => {
  it("sends a background request without a token and keeps the page when the refresh fails", async () => {
    const login = vi.fn();
    setKeycloakInstance({
      token: "expired",
      updateToken: vi.fn().mockRejectedValue(new Error("Keycloak down")),
      login,
    } as unknown as Keycloak);
    const { send, authorizations } = createRecordingInstance();

    await send(true);

    expect(authorizations).toEqual([undefined]);
    expect(login).not.toHaveBeenCalled();
  });

  it("starts the login for a request of the user when the refresh fails", async () => {
    const login = vi.fn();
    setKeycloakInstance({
      token: "expired",
      updateToken: vi.fn().mockRejectedValue(new Error("Keycloak down")),
      login,
    } as unknown as Keycloak);
    const { send } = createRecordingInstance();

    await send(false);

    expect(login).toHaveBeenCalledOnce();
  });

  it("attaches the refreshed token to a background request", async () => {
    setKeycloakInstance({ token: "fresh", updateToken: vi.fn().mockResolvedValue(true), login: vi.fn() } as unknown as Keycloak);
    const { send, authorizations } = createRecordingInstance();

    await send(true);

    expect(authorizations).toEqual(["Bearer fresh"]);
  });
});
