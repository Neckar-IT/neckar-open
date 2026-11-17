/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ThemeProvider, type ThemeDefinition } from "../../../providers/theme";
import { ThemeSwitcher } from "./ThemeSwitcher";

/**
 * Test Setup
 */

const themes: ThemeDefinition[] = [
  { id: "neckar", label: "Neckar", colorScheme: "selectable" },
  { id: "terminal", label: "Terminal", colorScheme: "dark" },
];

function mockMatchMedia(): void {
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }));
}

function renderThemeSwitcher(availableThemes: ThemeDefinition[]): void {
  render(
    <ThemeProvider themes={availableThemes}>
      <ThemeSwitcher />
    </ThemeProvider>,
  );
}

/**
 * Tests
 */

describe("ThemeSwitcher", () => {
  beforeEach(() => {
    mockMatchMedia();
  });

  afterEach(() => {
    window.localStorage.clear();
    window.document.documentElement.removeAttribute("data-theme");
    window.document.documentElement.classList.remove("light", "dark");
  });

  it("should render nothing for a single fixed theme", () => {
    renderThemeSwitcher([{ id: "neckar", label: "Neckar", colorScheme: "light" }]);

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("should list all themes and the color modes", async () => {
    const user = userEvent.setup();
    renderThemeSwitcher(themes);

    await user.click(screen.getByRole("button"));

    expect(screen.getByRole("menuitemradio", { name: "Neckar" })).toBeInTheDocument();
    expect(screen.getByRole("menuitemradio", { name: "Terminal" })).toBeInTheDocument();
    expect(screen.getByRole("menuitemradio", { name: "Light" })).toBeInTheDocument();
    expect(screen.getByRole("menuitemradio", { name: "Dark" })).toBeInTheDocument();
    expect(screen.getByRole("menuitemradio", { name: "System" })).toBeInTheDocument();
  });

  it("should switch the theme", async () => {
    const user = userEvent.setup();
    renderThemeSwitcher(themes);

    await user.click(screen.getByRole("button"));
    await user.click(screen.getByRole("menuitemradio", { name: "Terminal" }));

    expect(window.document.documentElement.getAttribute("data-theme")).toBe("terminal");
    expect(window.document.documentElement.classList.contains("dark")).toBe(true);
  });

  it("should hide the color mode section for a fixed color scheme theme", async () => {
    window.localStorage.setItem("nit.theme", "terminal");
    const user = userEvent.setup();
    renderThemeSwitcher(themes);

    await user.click(screen.getByRole("button"));

    expect(screen.getByRole("menuitemradio", { name: "Terminal" })).toBeInTheDocument();
    expect(screen.queryByRole("menuitemradio", { name: "Light" })).not.toBeInTheDocument();
  });
});
