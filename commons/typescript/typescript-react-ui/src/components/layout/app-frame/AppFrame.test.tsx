import { render, screen, waitFor } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { BookOpenIcon } from "lucide-react";
import { afterEach, describe, expect, it } from "vitest";
import { SidebarProvider, useSidebar } from "../../primitives/sidebar";
import { AppMain } from "./AppFrame";

// The sidebar's `useIsMobile` compares `window.innerWidth` with its breakpoint of 768 px; jsdom starts at 1024 px.
const desktopWidth = window.innerWidth;

function setViewportWidth(width: number): void {
  Object.defineProperty(window, "innerWidth", { configurable: true, writable: true, value: width });
}

function MobileSidebarState(): React.ReactElement {
  const { openMobile } = useSidebar();
  return <output aria-label="mobile sidebar">{openMobile ? "open" : "closed"}</output>;
}

function renderAppMain(): void {
  render(
    <SidebarProvider>
      <AppMain appName="Patterns" brandIcon={BookOpenIcon} breadcrumbs={<nav aria-label="breadcrumb">Books</nav>}>
        <h1>Books</h1>
      </AppMain>
      <MobileSidebarState />
    </SidebarProvider>,
  );
}

afterEach(() => {
  setViewportWidth(desktopWidth);
});

describe("AppMain", () => {
  it("renders the sticky h-12 header with breadcrumbs from tablet width", () => {
    renderAppMain();

    const header = screen.getByRole("banner");
    expect(header).toHaveClass("h-12", "sticky");
    expect(screen.getByRole("navigation", { name: "breadcrumb" })).toBeVisible();
    expect(screen.queryByText("Patterns")).toBeNull();
  });

  it("renders the brand with the app name on mobile", () => {
    setViewportWidth(390);

    renderAppMain();

    expect(screen.getByText("Patterns")).toBeVisible();
    expect(screen.getByRole("banner")).not.toHaveClass("h-12");
    expect(screen.queryByRole("navigation", { name: "breadcrumb" })).toBeNull();
  });

  it("opens the mobile sidebar with the menu toggle", async () => {
    setViewportWidth(390);
    renderAppMain();

    await userEvent.click(screen.getByRole("button", { name: "Toggle menu" }));

    expect(screen.getByRole("status", { name: "mobile sidebar" })).toHaveTextContent("open");
  });

  it("appends the app name to the document title", async () => {
    renderAppMain();

    await waitFor(() => {
      expect(document.title).toContain("Patterns");
    });
    expect(document.title).toContain("Books");
  });
});
