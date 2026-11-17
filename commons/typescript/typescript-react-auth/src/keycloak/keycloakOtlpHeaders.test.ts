import { afterEach, describe, expect, it, vi } from "vitest";
import { keycloakOtlpHeaders } from "./keycloakOtlpHeaders";
import { getKeycloakAccessTokenWithoutLogin } from "./keycloakTokenStore";

vi.mock("./keycloakTokenStore", () => ({ getKeycloakAccessTokenWithoutLogin: vi.fn() }));

describe("keycloakOtlpHeaders", () => {
  afterEach(() => {
    vi.mocked(getKeycloakAccessTokenWithoutLogin).mockReset();
  });

  it("sends the access token as Bearer", async () => {
    vi.mocked(getKeycloakAccessTokenWithoutLogin).mockResolvedValue("my-token");

    expect(await keycloakOtlpHeaders()).toEqual({ Authorization: "Bearer my-token" });
  });

  it("sends no header without a session", async () => {
    vi.mocked(getKeycloakAccessTokenWithoutLogin).mockResolvedValue(null);

    expect(await keycloakOtlpHeaders()).toEqual({});
  });

  it("sends no header when the token lookup throws", async () => {
    vi.mocked(getKeycloakAccessTokenWithoutLogin).mockRejectedValue(new Error("Keycloak initialization timed out"));

    expect(await keycloakOtlpHeaders()).toEqual({});
  });
});
