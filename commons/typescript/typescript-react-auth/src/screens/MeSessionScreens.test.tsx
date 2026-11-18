/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { createQueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import type { UserDetails } from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ImpersonationStorageKey } from "../impersonation/impersonationStorage";
import { MeSessionScreens } from "./MeSessionScreens";

const registeredUser: UserDetails = {
  id: "00000000-0000-0000-0000-000000000001",
  email: "test-user@email.com",
  firstName: "Test",
  lastName: "User",
  loginName: "test-user",
  environment: [],
  avatarUrl: null,
  tenant: null,
  capabilities: [],
  roleLabel: null,
};

function renderRegistered(): void {
  render(
    <I18nProvider>
      <QueryClientProvider client={createQueryClient()}>
        <MeSessionScreens
          logoutPending={false}
          session={{ type: "registered", user: registeredUser }}
          onLogout={() => {}}
          onSignInAgain={() => {}}
        >
          <div>Application</div>
        </MeSessionScreens>
      </QueryClientProvider>
    </I18nProvider>,
  );
}

/**
 * Tests
 */

describe("MeSessionScreens with impersonation", () => {
  const originalLocation = window.location;
  const assign = vi.fn();

  beforeEach(() => {
    window.sessionStorage.clear();
    assign.mockReset();
    Object.defineProperty(window, "location", { value: { assign }, writable: true, configurable: true });
  });

  afterEach(() => {
    Object.defineProperty(window, "location", { value: originalLocation, writable: true, configurable: true });
    vi.restoreAllMocks();
  });

  it("shows no banner while the tab acts as the signed-in user", async () => {
    renderRegistered();

    expect(await screen.findByText("Application")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("names the impersonated user and leads back to the own account", async () => {
    window.sessionStorage.setItem(ImpersonationStorageKey, registeredUser.id);
    renderRegistered();

    expect(await screen.findByText("Signed in as Test User")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Back to my account" }));

    expect(window.sessionStorage.getItem(ImpersonationStorageKey)).toBeNull();
    expect(assign).toHaveBeenCalledWith("/");
  });

  it("fails when the backend answered as a user other than the impersonated one", () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    window.sessionStorage.setItem(ImpersonationStorageKey, "00000000-0000-0000-0000-000000000099");

    expect(() => renderRegistered()).toThrow("backend answered as user 00000000-0000-0000-0000-000000000001");
  });
});
