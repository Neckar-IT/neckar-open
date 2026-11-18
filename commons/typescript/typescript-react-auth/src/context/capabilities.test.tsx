import type { UserDetails } from "@neckar-hub/typescript-utils/openapi-types";
import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useTenantId } from "./capabilities";
import { UserContextProvider } from "./UserContext";

function userOf(tenant: UserDetails["tenant"]): UserDetails {
  return {
    id: "00000000-0000-0000-0000-000000000001",
    email: "test-user@email.com",
    firstName: "Test",
    lastName: "User",
    loginName: "test-user",
    environment: [],
    avatarUrl: null,
    tenant,
    capabilities: [],
    roleLabel: null,
  };
}

function tenantIdOf(user: UserDetails): string | null {
  return renderHook(() => useTenantId(), {
    wrapper: ({ children }) => <UserContextProvider user={user}>{children}</UserContextProvider>,
  }).result.current;
}

describe("useTenantId", () => {
  it("answers the id of the tenant the user acts for", () => {
    expect(tenantIdOf(userOf({ id: "b2c3d4e5-f6a7-8901-bcde-f12345678901", name: "Buchclub Tübingen" }))).toBe(
      "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    );
  });

  it("answers null for a user without tenant", () => {
    expect(tenantIdOf(userOf(null))).toBeNull();
  });
});
