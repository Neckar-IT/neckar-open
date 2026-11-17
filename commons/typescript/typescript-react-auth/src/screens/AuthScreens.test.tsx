/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { LoadingUserDataScreen, OnboardingRejectedScreen } from "./AuthScreens";

/**
 * Tests
 */

describe("LoadingUserDataScreen", () => {
  it("renders a distinct title and description so the user-data phase is diagnosable separate from Keycloak init", () => {
    render(
      <I18nProvider>
        <LoadingUserDataScreen />
      </I18nProvider>,
    );

    // Phase 2 title and description are shown.
    expect(screen.getByText("Loading user data ...")).toBeInTheDocument();
    expect(screen.getByText("Authentication is complete. Loading your profile from the server.")).toBeInTheDocument();

    // Phase 1 text must NOT appear — the two loading phases must be visibly distinct.
    expect(screen.queryByText("Authenticating ...")).not.toBeInTheDocument();
  });
});

describe("OnboardingRejectedScreen", () => {
  it("explains the rejection and offers logout instead of a reload", async () => {
    const onLogout = vi.fn();
    render(
      <I18nProvider>
        <OnboardingRejectedScreen onLogout={onLogout} logoutPending={false} />
      </I18nProvider>,
    );

    expect(screen.getByText("Invitation required")).toBeInTheDocument();
    expect(
      screen.getByText(
        "You are signed in. This application is available by invitation — as soon as you have been invited, your account is ready to go.",
      ),
    ).toBeInTheDocument();

    // A reload cannot turn a missing invitation into an existing one, so the screen must not offer one.
    expect(screen.queryByRole("button", { name: "Reload page" })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Sign out" }));
    expect(onLogout).toHaveBeenCalledTimes(1);
  });

  it("names the product's contact address as a mailto link when one is configured", () => {
    render(
      <I18nProvider>
        <OnboardingRejectedScreen onLogout={vi.fn()} logoutPending={false} contactEmail="info@neckar.it" />
      </I18nProvider>,
    );

    expect(screen.getByRole("link", { name: "info@neckar.it" })).toHaveAttribute("href", "mailto:info@neckar.it");
  });

  it("omits the contact line when no address is configured", () => {
    render(
      <I18nProvider>
        <OnboardingRejectedScreen onLogout={vi.fn()} logoutPending={false} />
      </I18nProvider>,
    );

    expect(screen.queryByRole("link")).not.toBeInTheDocument();
  });
});
