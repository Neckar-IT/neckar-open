/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import type { UserDetails } from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type Keycloak from "keycloak-js";
import { describe, expect, it } from "vitest";
import { UserContextProvider } from "../context/UserContext";
import { KeycloakContext } from "../keycloak/KeycloakContext";
import { UserAvatar, UserMenu, UserNameEmail } from "./UserMenu";

/**
 * Fixture Data
 */

const user: UserDetails = {
  firstName: "Ada",
  lastName: "Lovelace",
  email: "ada@neckar.it",
  avatarUrl: null,
} as UserDetails;

// The menu only reaches for the logout call, so a stub of it stands in for the whole instance.
const keycloak = { logout: () => Promise.resolve() } as unknown as Keycloak;

function renderMenu() {
  return render(
    <I18nProvider>
      <KeycloakContext value={keycloak}>
        <UserContextProvider user={user}>
          <UserMenu />
        </UserContextProvider>
      </KeycloakContext>
    </I18nProvider>,
  );
}

/**
 * Tests
 */

describe("UserMenu", () => {
  it("resolves its own translations rather than showing the key", async () => {
    renderMenu();

    await userEvent.click(screen.getByRole("button"));

    expect(screen.getByRole("menuitem", { name: "Log out" })).toBeVisible();
    expect(screen.queryByText("userMenu.logout")).not.toBeInTheDocument();
  });
});

describe("UserAvatar", () => {
  it("falls back to the initials of the user's name", () => {
    render(
      <I18nProvider>
        <UserAvatar user={user} />
      </I18nProvider>,
    );

    expect(screen.getByText("AL")).toBeVisible();
  });
});

describe("UserNameEmail", () => {
  it("shows the full name above the email address", () => {
    render(<UserNameEmail user={user} />);

    expect(screen.getByText("Ada Lovelace")).toBeVisible();
    expect(screen.getByText("ada@neckar.it")).toBeVisible();
  });

  it("omits the email address for an account that carries none", () => {
    render(<UserNameEmail user={{ ...user, email: "" }} />);

    expect(screen.getByText("Ada Lovelace")).toBeVisible();
    expect(screen.queryByText("ada@neckar.it")).not.toBeInTheDocument();
  });

  it("shows the login name for an account without first and last name", () => {
    render(<UserNameEmail user={{ ...user, firstName: "", lastName: "", loginName: "ada" }} />);

    expect(screen.getByText("ada")).toBeVisible();
  });
});
