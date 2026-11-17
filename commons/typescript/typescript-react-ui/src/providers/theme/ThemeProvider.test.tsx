/**
 * Imports
 */

import { act, render, screen } from "@test/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { ThemeDefinition } from "./themeDefinition";
import { ThemeProvider } from "./ThemeProvider";
import { useTheme } from "./useTheme";

/**
 * Test Setup
 */

const themes: ThemeDefinition[] = [
  { id: "neckar", label: "Neckar", colorScheme: "selectable" },
  { id: "terminal", label: "Terminal", colorScheme: "dark" },
  { id: "paper", label: "Paper", colorScheme: "light" },
];

function mockMatchMedia(prefersDark: boolean): void {
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: query === "(prefers-color-scheme: dark)" ? prefersDark : false,
    media: query,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }));
}

function ThemeInfo(): React.ReactElement {
  const { theme, setThemeId, colorMode, setColorMode, resolvedColorScheme } = useTheme();
  return (
    <div>
      <span data-testid="theme-id">{theme.id}</span>
      <span data-testid="color-mode">{colorMode}</span>
      <span data-testid="resolved-color-scheme">{resolvedColorScheme}</span>
      <button data-testid="switch-to-terminal" onClick={() => setThemeId("terminal")}>
        terminal
      </button>
      <button data-testid="switch-to-dark" onClick={() => setColorMode("dark")}>
        dark
      </button>
    </div>
  );
}

/**
 * Tests
 */

describe("ThemeProvider", () => {
  beforeEach(() => {
    mockMatchMedia(false);
  });

  afterEach(() => {
    window.localStorage.clear();
    window.document.documentElement.removeAttribute("data-theme");
    window.document.documentElement.classList.remove("light", "dark");
  });

  it("should apply the default theme to the html element", () => {
    render(
      <ThemeProvider themes={themes}>
        <ThemeInfo />
      </ThemeProvider>,
    );

    expect(screen.getByTestId("theme-id")).toHaveTextContent("neckar");
    expect(window.document.documentElement.getAttribute("data-theme")).toBe("neckar");
    expect(window.document.documentElement.classList.contains("light")).toBe(true);
  });

  it("should respect the defaultThemeId", () => {
    render(
      <ThemeProvider themes={themes} defaultThemeId="paper">
        <ThemeInfo />
      </ThemeProvider>,
    );

    expect(window.document.documentElement.getAttribute("data-theme")).toBe("paper");
  });

  it("should switch the theme and persist it", () => {
    render(
      <ThemeProvider themes={themes}>
        <ThemeInfo />
      </ThemeProvider>,
    );

    act(() => {
      screen.getByTestId("switch-to-terminal").click();
    });

    expect(window.document.documentElement.getAttribute("data-theme")).toBe("terminal");
    expect(window.localStorage.getItem("nit.theme")).toBe("terminal");
  });

  it("should force the dark class for a fixed dark theme", () => {
    render(
      <ThemeProvider themes={themes} defaultThemeId="terminal">
        <ThemeInfo />
      </ThemeProvider>,
    );

    expect(screen.getByTestId("resolved-color-scheme")).toHaveTextContent("dark");
    expect(window.document.documentElement.classList.contains("dark")).toBe(true);
    expect(window.document.documentElement.classList.contains("light")).toBe(false);
  });

  it("should apply the user color mode for a selectable theme", () => {
    render(
      <ThemeProvider themes={themes}>
        <ThemeInfo />
      </ThemeProvider>,
    );

    act(() => {
      screen.getByTestId("switch-to-dark").click();
    });

    expect(window.document.documentElement.classList.contains("dark")).toBe(true);
    expect(window.localStorage.getItem("nit.color-mode")).toBe("dark");
  });

  it("should follow the system color scheme in system mode", () => {
    mockMatchMedia(true);

    render(
      <ThemeProvider themes={themes}>
        <ThemeInfo />
      </ThemeProvider>,
    );

    expect(screen.getByTestId("color-mode")).toHaveTextContent("system");
    expect(screen.getByTestId("resolved-color-scheme")).toHaveTextContent("dark");
  });

  it("should persist the resolved color scheme for the pre-React inline script", () => {
    render(
      <ThemeProvider themes={themes} defaultThemeId="terminal">
        <ThemeInfo />
      </ThemeProvider>,
    );

    expect(window.localStorage.getItem("nit.resolved-color-scheme")).toBe("dark");
  });

  it("should restore the persisted theme", () => {
    window.localStorage.setItem("nit.theme", "paper");

    render(
      <ThemeProvider themes={themes}>
        <ThemeInfo />
      </ThemeProvider>,
    );

    expect(window.document.documentElement.getAttribute("data-theme")).toBe("paper");
  });

  it("should fall back to the default theme for an unknown persisted theme id", () => {
    window.localStorage.setItem("nit.theme", "no-longer-existing-theme");

    render(
      <ThemeProvider themes={themes}>
        <ThemeInfo />
      </ThemeProvider>,
    );

    expect(window.document.documentElement.getAttribute("data-theme")).toBe("neckar");
  });
});
