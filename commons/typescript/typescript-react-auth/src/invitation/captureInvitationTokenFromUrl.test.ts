import { beforeEach, describe, expect, it } from "vitest";
import { captureInvitationTokenFromUrl } from "./captureInvitationTokenFromUrl";
import { PendingInvitationTokenKey, readPendingInvitationToken } from "./pendingInvitationToken";

describe("captureInvitationTokenFromUrl", () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    window.history.replaceState({}, "", "/");
  });

  it("parks the token and strips the param, so the token survives the Keycloak login redirect", () => {
    window.history.replaceState({}, "", "/?invitation=c3d4e5f6-a7b8-9012-cdef-123456789012");

    captureInvitationTokenFromUrl();

    expect(readPendingInvitationToken()).toBe("c3d4e5f6-a7b8-9012-cdef-123456789012");
    expect(window.location.search).toBe("");
  });

  it("keeps the other query params when stripping the invitation", () => {
    window.history.replaceState({}, "", "/?invitation=token-1&keep=yes");

    captureInvitationTokenFromUrl();

    expect(window.location.search).toBe("?keep=yes");
  });

  it("stores nothing without the param", () => {
    captureInvitationTokenFromUrl();

    expect(window.sessionStorage.getItem(PendingInvitationTokenKey)).toBeNull();
  });

  it("stores nothing for an empty param, which would otherwise park a token the backend rejects", () => {
    window.history.replaceState({}, "", "/?invitation=");

    captureInvitationTokenFromUrl();

    expect(window.sessionStorage.getItem(PendingInvitationTokenKey)).toBeNull();
  });
});
