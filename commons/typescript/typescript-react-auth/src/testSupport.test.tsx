import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useUser } from "./context/UserContext";
import { userWithCapabilities } from "./testSupport";

describe("userWithCapabilities", () => {
  it("provides a user holding exactly the given capabilities", () => {
    const { result } = renderHook(() => useUser(), { wrapper: userWithCapabilities(["data:write"]) });
    expect(result.current.capabilities).toEqual(["data:write"]);
  });

  it("puts the user into the default tenant when no tenant id is given", () => {
    const { result } = renderHook(() => useUser(), { wrapper: userWithCapabilities([]) });
    expect(result.current.tenant?.id).toBe("5b0c8d2e-0000-4000-8000-0000000000aa");
  });

  it("makes a user without tenant for tenant id null", () => {
    const { result } = renderHook(() => useUser(), { wrapper: userWithCapabilities([], { tenantId: null }) });
    expect(result.current.tenant).toBeNull();
  });

  it("puts the user into the tenant of a given tenant id", () => {
    const { result } = renderHook(() => useUser(), {
      wrapper: userWithCapabilities([], { tenantId: "5b0c8d2e-0000-4000-8000-0000000000bb" }),
    });
    expect(result.current.tenant?.id).toBe("5b0c8d2e-0000-4000-8000-0000000000bb");
  });
});
