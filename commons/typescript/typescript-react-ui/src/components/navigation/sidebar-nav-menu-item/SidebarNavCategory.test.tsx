import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PencilRulerIcon } from "lucide-react";
import type { ReactElement } from "react";
import { describe, expect, it } from "vitest";
import { SidebarMenu, SidebarMenuSubItem, SidebarProvider } from "../../primitives/sidebar";
import { SidebarNavCategory } from "./SidebarNavCategory";

function CategoryInMenu({ containsCurrentPage }: { containsCurrentPage: boolean }): ReactElement {
  return (
    <SidebarProvider>
      <SidebarMenu>
        <SidebarNavCategory title="Planung" icon={PencilRulerIcon} containsCurrentPage={containsCurrentPage}>
          <SidebarMenuSubItem>Finanzierung</SidebarMenuSubItem>
        </SidebarNavCategory>
      </SidebarMenu>
    </SidebarProvider>
  );
}

describe("SidebarNavCategory", () => {
  it("shows the sub-items of the category that contains the current page", () => {
    render(<CategoryInMenu containsCurrentPage={true} />);

    expect(screen.getByText("Finanzierung")).toBeVisible();
  });

  it("hides the sub-items of a category without the current page", () => {
    render(<CategoryInMenu containsCurrentPage={false} />);

    expect(screen.queryByText("Finanzierung")).toBeNull();
  });

  it("opens a mounted category once it contains the current page", () => {
    const { rerender } = render(<CategoryInMenu containsCurrentPage={false} />);

    rerender(<CategoryInMenu containsCurrentPage={true} />);

    expect(screen.getByText("Finanzierung")).toBeVisible();
  });

  it("keeps a category open once it no longer contains the current page", () => {
    const { rerender } = render(<CategoryInMenu containsCurrentPage={true} />);

    rerender(<CategoryInMenu containsCurrentPage={false} />);

    expect(screen.getByText("Finanzierung")).toBeVisible();
  });

  it("renders the category as an item of the menu list", () => {
    render(<CategoryInMenu containsCurrentPage={false} />);

    expect(screen.getByRole("button", { name: "Planung" }).closest("li")?.parentElement?.tagName).toBe("UL");
  });

  it("opens and closes the category on click", async () => {
    const user = userEvent.setup();
    render(<CategoryInMenu containsCurrentPage={false} />);

    await user.click(screen.getByRole("button", { name: "Planung" }));
    expect(screen.getByText("Finanzierung")).toBeVisible();

    await user.click(screen.getByRole("button", { name: "Planung" }));
    expect(screen.queryByText("Finanzierung")).toBeNull();
  });
});
