/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { act, render, screen } from "@testing-library/react";
import type { ReactElement } from "react";
import { beforeEach, describe, expect, it } from "vitest";
import { PendingInvitationTokenKey } from "../invitation/pendingInvitationToken";
import { OnboardingGate, type OnboardingRegistrationProps } from "./OnboardingGate";

/** The props of the last rendered registration page — the test triggers its outcomes through them. */
let registrationProps: OnboardingRegistrationProps | undefined;

function renderRegistration(props: OnboardingRegistrationProps): ReactElement {
  registrationProps = props;
  return <div>{props.joinsInvitingTenant ? "Join page" : "Signup page"}</div>;
}

function renderGate(selfSignupAllowed: boolean): void {
  render(
    <I18nProvider>
      <OnboardingGate
        selfSignupAllowed={selfSignupAllowed}
        renderRegistration={renderRegistration}
        onRegistered={() => {}}
        onLogout={() => {}}
        logoutPending={false}
      />
    </I18nProvider>,
  );
}

/**
 * Tests
 */

beforeEach(() => {
  window.sessionStorage.clear();
  registrationProps = undefined;
});

describe("OnboardingGate without a pending invitation", () => {
  it("renders the product's registration page when self-signup is allowed", () => {
    renderGate(true);

    expect(screen.getByText("Signup page")).toBeVisible();
  });

  it("shows the access-denied screen when self-signup is rejected", () => {
    renderGate(false);

    expect(screen.getByRole("button", { name: "Sign out" })).toBeVisible();
    expect(screen.queryByText("Signup page")).toBeNull();
  });

  it("shows the access-denied screen once the registration answers bad/SelfSignupRejected", () => {
    renderGate(true);

    act(() => registrationProps?.onSelfSignupRejected());

    expect(screen.getByRole("button", { name: "Sign out" })).toBeVisible();
  });
});

describe("OnboardingGate with a pending invitation", () => {
  beforeEach(() => {
    window.sessionStorage.setItem(PendingInvitationTokenKey, "invitation-token-1");
  });

  it("renders the product's registration page as a join confirmation, even when self-signup is rejected", () => {
    renderGate(false);

    expect(screen.getByText("Join page")).toBeVisible();
  });

  it("drops a token the registration answers with bad/InvitationNotAcceptable and falls back to self-signup", () => {
    renderGate(true);

    act(() => registrationProps?.onInvitationNotAcceptable());

    expect(screen.getByText("Signup page")).toBeVisible();
    expect(window.sessionStorage.getItem(PendingInvitationTokenKey)).toBeNull();
  });

  it("shows the access-denied screen when the token is dead and self-signup is rejected", () => {
    renderGate(false);

    act(() => registrationProps?.onInvitationNotAcceptable());

    expect(screen.getByRole("button", { name: "Sign out" })).toBeVisible();
  });
});
