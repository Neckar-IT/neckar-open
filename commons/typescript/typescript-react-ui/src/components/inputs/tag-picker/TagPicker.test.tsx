/**
 * Imports
 */

import type { Tag } from "@neckar-hub/typescript-utils/openapi-types";
import { elementAt, render, screen, within } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { TagPicker } from "./TagPicker";

/**
 * Tests
 *
 * TagPicker is a multi-select over tags. Selecting an unselected tag adds it; selecting a selected
 * tag (in the list or via its badge) removes it. Removing the last tag reports `null`, not `[]`.
 */

const red: Tag = { id: "1", name: "Red" };
const blue: Tag = { id: "2", name: "Blue" };
const green: Tag = { id: "3", name: "Green" };
const options = [red, blue, green];

/** Opens the popover by clicking the trigger's chevron (always the last button). */
const openPopover = async (user: ReturnType<typeof userEvent.setup>) => {
  const buttons = screen.getAllByRole("button");
  await user.click(elementAt(buttons, -1));
};

describe("TagPicker", () => {
  it("shows the placeholder when nothing is selected", () => {
    render(<TagPicker options={options} value={null} onChange={vi.fn()} />);

    expect(screen.getByText("Select Tags")).toBeInTheDocument();
  });

  it("shows each selected tag as a badge", () => {
    render(<TagPicker options={options} value={[red]} onChange={vi.fn()} />);

    expect(screen.getByText("Red")).toBeInTheDocument();
  });

  it("adds a tag to the selection when its option is picked", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TagPicker options={options} value={[red]} onChange={onChange} />);

    await openPopover(user);
    await user.click(screen.getByRole("option", { name: "Blue" }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith([red, blue]);
  });

  it("removes a tag from the selection when its selected option is picked", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TagPicker options={options} value={[red, blue]} onChange={onChange} />);

    await openPopover(user);
    await user.click(screen.getByRole("option", { name: "Red" }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith([blue]);
  });

  it("reports null when the last selected tag is removed", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TagPicker options={options} value={[red]} onChange={onChange} />);

    await openPopover(user);
    await user.click(screen.getByRole("option", { name: "Red" }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(null);
  });

  it("removes a tag via its badge remove button", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TagPicker options={options} value={[red, blue]} onChange={onChange} />);

    // Scope to the "Red" badge and click its remove (X) button.
    const redBadge = screen.getByText("Red").parentElement;
    if (redBadge === null) throw new Error("Red badge not found");
    await user.click(within(redBadge).getByRole("button"));

    expect(onChange).toHaveBeenCalledExactlyOnceWith([blue]);
  });
});
