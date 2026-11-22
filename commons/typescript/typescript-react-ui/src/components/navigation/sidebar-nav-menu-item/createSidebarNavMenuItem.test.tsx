import { createLink, createUrlGenerator, MemoryRouter, type NoParams } from "@neckar-hub/typescript-react-router";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SidebarMenu, SidebarProvider } from "../../primitives/sidebar";
import { createSidebarNavMenuItem } from "./createSidebarNavMenuItem";

type Routes = { "/projects": NoParams; "/reports": NoParams };

const SidebarNavMenuItem = createSidebarNavMenuItem<Routes>({ Link: createLink<Routes>(), generateUrl: createUrlGenerator<Routes>() });

function renderSidebar(pathname: string, projectsOverride?: boolean): void {
  render(
    <MemoryRouter initialEntries={[pathname]}>
      <SidebarProvider>
        <SidebarMenu>
          <SidebarNavMenuItem to="/projects" title="Projects" isActive={projectsOverride} />
          <SidebarNavMenuItem to="/reports" title="Reports" />
        </SidebarMenu>
      </SidebarProvider>
    </MemoryRouter>,
  );
}

function ariaCurrent(name: string): string | null {
  return screen.getByRole("link", { name }).getAttribute("aria-current");
}

describe("createSidebarNavMenuItem", () => {
  it("marks the item of the current location as the current page", () => {
    renderSidebar("/reports");

    expect([ariaCurrent("Projects"), ariaCurrent("Reports")]).toEqual([null, "page"]);
  });

  it("marks an item activated by the override as the current section", () => {
    renderSidebar("/electro/projects/1", true);

    expect(ariaCurrent("Projects")).toBe("true");
  });

  it("marks an item activated by the override on its own page as the current page", () => {
    renderSidebar("/projects", true);

    expect(ariaCurrent("Projects")).toBe("page");
  });
});
