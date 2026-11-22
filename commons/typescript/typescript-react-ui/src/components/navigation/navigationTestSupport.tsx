import { MemoryRouter } from "@neckar-hub/typescript-react-router";
import { render, type RenderResult, screen } from "@testing-library/react";
import { vi } from "vitest";
import { SidebarProvider } from "../primitives/sidebar";

/**
 * Renders a sidebar navigation at [path] inside the `SidebarProvider` it needs; jsdom lacks the
 * `matchMedia` the provider reads to tell a mobile viewport.
 */
export function renderSidebarNavAt(path: string, nav: React.ReactElement): RenderResult {
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }));
  return render(
    <MemoryRouter initialEntries={[path]}>
      <SidebarProvider>{nav}</SidebarProvider>
    </MemoryRouter>,
  );
}

export function isSidebarNavEntryActive(name: string): boolean {
  return screen.getByRole("link", { name }).getAttribute("data-active") === "true";
}
