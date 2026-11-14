/**
 * Imports
 */

import type { LinkPropsPrimitive, NoParams } from "@neckar-hub/typescript-react-router";
import { renderHook } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import { createUseIsActive } from "./createUseIsActive";

/**
 * The hook reads the current path from the router's `useLocation`. Mock it so the test controls the
 * path without mounting a router.
 */

const { mockUseLocation } = vi.hoisted(() => ({ mockUseLocation: vi.fn() }));

vi.mock("@neckar-hub/typescript-react-router", () => ({
  useLocation: () => mockUseLocation(),
}));

/**
 * Test fixtures
 */

type Routes = { dashboard: NoParams; settings: NoParams; settingsArchive: NoParams; home: NoParams };

const generateUrl = (link: LinkPropsPrimitive<Routes>): string =>
  link.to === "home" ? "/" : link.to === "settingsArchive" ? "/settings-archive" : `/${link.to}`;

const link = (to: keyof Routes): LinkPropsPrimitive<Routes> => ({ to }) as unknown as LinkPropsPrimitive<Routes>;

const useIsActive = createUseIsActive<Routes>(generateUrl);

function isActive(pathname: string, to: keyof Routes, end: boolean): boolean {
  mockUseLocation.mockReturnValue({ pathname });
  const { result } = renderHook(() => useIsActive(link(to), end));
  return result.current;
}

/**
 * Tests
 */

describe("createUseIsActive", () => {
  it("matches exactly when end is true", () => {
    expect(isActive("/settings", "settings", true)).toBe(true);
  });

  it("does not match a sub-path when end is true", () => {
    expect(isActive("/settings/profile", "settings", true)).toBe(false);
  });

  it("matches a sub-path by prefix when end is false", () => {
    expect(isActive("/settings/profile", "settings", false)).toBe(true);
  });

  it("matches the target itself by prefix when end is false", () => {
    expect(isActive("/settings", "settings", false)).toBe(true);
  });

  it("does not match an unrelated path when end is false", () => {
    expect(isActive("/dashboard", "settings", false)).toBe(false);
  });

  it("requires an exact match for the root target even when end is false", () => {
    expect(isActive("/", "home", false)).toBe(true);
  });

  it("does not treat every path as active for the root target", () => {
    // Without the "/" special case, a prefix match would make every path active for the root link.
    expect(isActive("/dashboard", "home", false)).toBe(false);
  });

  it("does not match a sibling whose path shares a string prefix without a boundary", () => {
    // "/settings-archive".startsWith("/settings") is true, but it is a different section.
    expect(isActive("/settings-archive", "settings", false)).toBe(false);
  });
});
