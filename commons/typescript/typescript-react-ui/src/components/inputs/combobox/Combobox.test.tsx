/**
 * Imports
 */

import { fireEvent, render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Combobox } from "./Combobox";

/**
 * Tests
 *
 * Combobox is an externally-filtered single-select. It does not filter `items` itself
 * (`shouldFilter={false}`) — typing only forwards the search text via `onFilterChange`. Selection
 * toggles: picking the already-selected item deselects it.
 */

const items = ["Apple", "Banana"];
const renderItem = (item: string) => item;

const baseProps = {
  items,
  filter: "",
  onFilterChange: vi.fn(),
  itemRenderer: renderItem,
};

describe("Combobox (flat variant)", () => {
  it("renders each item as an option", () => {
    render(<Combobox {...baseProps} variant="flat" selectedItem={undefined} setSelectedItem={vi.fn()} />);

    expect(screen.getByRole("option", { name: "Apple" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Banana" })).toBeInTheDocument();
  });

  it("selects an item when its option is clicked", async () => {
    const user = userEvent.setup();
    const setSelectedItem = vi.fn();
    render(<Combobox {...baseProps} variant="flat" selectedItem={undefined} setSelectedItem={setSelectedItem} />);

    await user.click(screen.getByRole("option", { name: "Banana" }));

    expect(setSelectedItem).toHaveBeenCalledExactlyOnceWith("Banana");
  });

  it("deselects when the already-selected item is clicked again", async () => {
    const user = userEvent.setup();
    const setSelectedItem = vi.fn();
    render(<Combobox {...baseProps} variant="flat" selectedItem="Apple" setSelectedItem={setSelectedItem} />);

    await user.click(screen.getByRole("option", { name: "Apple" }));

    expect(setSelectedItem).toHaveBeenCalledExactlyOnceWith(undefined);
  });

  it("clears the selection via the deselect button", async () => {
    const user = userEvent.setup();
    const setSelectedItem = vi.fn();
    render(<Combobox {...baseProps} variant="flat" selectedItem="Apple" setSelectedItem={setSelectedItem} />);

    // With a selection the only native button is the deselect bar at the top.
    await user.click(screen.getByRole("button"));

    expect(setSelectedItem).toHaveBeenCalledExactlyOnceWith(undefined);
  });

  it("forwards the search text via onFilterChange", () => {
    const onFilterChange = vi.fn();
    render(<Combobox {...baseProps} onFilterChange={onFilterChange} variant="flat" selectedItem={undefined} setSelectedItem={vi.fn()} />);

    fireEvent.change(screen.getByPlaceholderText("Filter Items ..."), { target: { value: "ban" } });

    expect(onFilterChange).toHaveBeenCalledWith("ban");
  });

  it("shows the empty state when there are no items", () => {
    render(<Combobox {...baseProps} items={[]} variant="flat" selectedItem={undefined} setSelectedItem={vi.fn()} />);

    expect(screen.getByText("No Items found.")).toBeInTheDocument();
  });
});

describe("Combobox (popover variant)", () => {
  it("shows the placeholder and selects an item after opening", async () => {
    const user = userEvent.setup();
    const setSelectedItem = vi.fn();
    render(<Combobox {...baseProps} variant="popover" selectedItem={undefined} setSelectedItem={setSelectedItem} />);

    const trigger = screen.getByRole("combobox");
    expect(trigger).toHaveTextContent("Search Items ...");

    await user.click(trigger);
    await user.click(screen.getByRole("option", { name: "Banana" }));

    expect(setSelectedItem).toHaveBeenCalledExactlyOnceWith("Banana");
  });
});
