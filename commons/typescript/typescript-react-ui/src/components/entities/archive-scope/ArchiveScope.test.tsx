import { ArchiveFilter } from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ArchiveScope } from "./ArchiveScope";

describe("ArchiveScope", () => {
  it("offers the three states and marks the current one as pressed", () => {
    render(<ArchiveScope label="Archive" value={ArchiveFilter.Archived} onChange={vi.fn()} />);

    expect(screen.getByRole("radiogroup", { name: "Archive" })).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: "Active" })).toHaveAttribute("aria-checked", "false");
    expect(screen.getByRole("radio", { name: "Archived" })).toHaveAttribute("aria-checked", "true");
    expect(screen.getByRole("radio", { name: "All" })).toHaveAttribute("aria-checked", "false");
  });

  it.each([
    [ArchiveFilter.Archived, "Archived"],
    [ArchiveFilter.All, "All"],
  ])("reports %s when its option is picked", async (filter, name) => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<ArchiveScope label="Archive" value={ArchiveFilter.OnlyActive} onChange={onChange} />);

    await user.click(screen.getByRole("radio", { name }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(filter);
  });

  it("keeps the selected option when it is pressed again", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<ArchiveScope label="Archive" value={ArchiveFilter.OnlyActive} onChange={onChange} />);

    await user.click(screen.getByRole("radio", { name: "Active" }));

    expect(onChange).not.toHaveBeenCalled();
  });

  it.each([
    [ArchiveFilter.OnlyActive, "false"],
    [ArchiveFilter.Archived, "true"],
    [ArchiveFilter.All, "true"],
  ])("sets data-active for %s to %s", (value, active) => {
    render(<ArchiveScope label="Archive" value={value} onChange={vi.fn()} />);

    const group = screen.getByRole("radiogroup", { name: "Archive" });
    expect(group).toHaveAttribute("data-active", active);
    expect(group).not.toHaveClass("bg-primary/10");
    const markedItems = screen.getAllByRole("radio").filter((item) => item.classList.contains("bg-primary/10"));
    expect(markedItems.map((item) => item.getAttribute("aria-checked"))).toEqual(active === "true" ? ["true"] : []);
  });
});
