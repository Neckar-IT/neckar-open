/**
 * Imports
 */

import type { UserDetails } from "@neckar-hub/typescript-utils/openapi-types";
import { renderHook } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { UserContextProvider } from "../context/UserContext";
import { ImpersonationStorageKey } from "../impersonation/impersonationStorage";
import { useOtelIdentity } from "./useOtelIdentity";

const { endClientErrorReporter } = vi.hoisted(() => ({ endClientErrorReporter: vi.fn() }));

vi.mock("@neckar-hub/typescript-otel-tracing", () => ({
  endClientErrorReporter,
  getIdentitySpanProcessor: () => null,
}));

const user = { loginName: "jschneider" } as unknown as UserDetails;

function wrapper({ children }: { children: ReactNode }): ReactNode {
  return <UserContextProvider user={user}>{children}</UserContextProvider>;
}

/**
 * Tests
 */

describe("useOtelIdentity", () => {
  afterEach(() => {
    endClientErrorReporter.mockClear();
    window.sessionStorage.clear();
  });

  it("ends the error reporter before the login once a user is known", () => {
    renderHook(() => useOtelIdentity(), { wrapper });

    expect(endClientErrorReporter).toHaveBeenCalled();
  });

  it("ends the error reporter while the tab impersonates", () => {
    window.sessionStorage.setItem(ImpersonationStorageKey, "rschelling");

    renderHook(() => useOtelIdentity(), { wrapper });

    expect(endClientErrorReporter).toHaveBeenCalled();
  });
});
