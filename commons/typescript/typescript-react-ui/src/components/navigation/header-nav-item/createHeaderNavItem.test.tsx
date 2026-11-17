import { createLink, createUrlGenerator, MemoryRouter, type NoParams } from "@neckar-hub/typescript-react-router";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { createHeaderNavItem } from "./createHeaderNavItem";

type Routes = { "/": NoParams; "/settings": NoParams; "/reports": NoParams };

const HeaderNavItem = createHeaderNavItem<Routes>({ Link: createLink<Routes>(), generateUrl: createUrlGenerator<Routes>() });

function renderNavigation(pathname: string, settingsEnd = false): void {
  render(
    <MemoryRouter initialEntries={[pathname]}>
      <nav>
        <HeaderNavItem to="/" title="Home" />
        <HeaderNavItem to="/settings" title="Settings" end={settingsEnd} />
        <HeaderNavItem to="/reports" title="Reports" />
      </nav>
    </MemoryRouter>,
  );
}

function currentLinks(): string[] {
  return screen.queryAllByRole("link", { current: "page" }).map((link) => link.textContent ?? "");
}

describe("createHeaderNavItem", () => {
  it("marks the item whose path the current location continues", () => {
    renderNavigation("/settings/profile");

    expect(currentLinks()).toEqual(["Settings"]);
  });

  it("marks no item below its path when end is set", () => {
    renderNavigation("/settings/profile", true);

    expect(currentLinks()).toEqual([]);
  });

  it("highlights the current item and mutes every other item", () => {
    renderNavigation("/");

    const home = screen.getByRole("link", { name: "Home" });
    expect(home).toHaveClass("text-foreground", "font-semibold");
    expect(home).not.toHaveClass("text-muted-foreground");
    for (const name of ["Settings", "Reports"]) {
      const other = screen.getByRole("link", { name });
      expect(other).toHaveClass("text-muted-foreground");
      expect(other).not.toHaveClass("text-foreground");
      expect(other).not.toHaveClass("font-semibold");
    }
  });
});
