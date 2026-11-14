/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { TextList } from "./TextList";

/**
 * Tests
 *
 * TextList edits a `string[]` in place: one input per entry, a remove button beside each, an add
 * button below. The position is the identity — two entries may hold the same text.
 */

const addButton = () => screen.getByRole("button", { name: "Add entry" });
const removeButtons = () => screen.getAllByRole("button", { name: "Remove entry" });

describe("TextList", () => {
  it("shows one input per entry", () => {
    render(<TextList value={["first", "second"]} onChange={vi.fn()} />);

    expect(screen.getByDisplayValue("first")).toBeInTheDocument();
    expect(screen.getByDisplayValue("second")).toBeInTheDocument();
  });

  it("appends an empty entry when the add button is used", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TextList value={["first"]} onChange={onChange} />);

    await user.click(addButton());

    expect(onChange).toHaveBeenCalledExactlyOnceWith(["first", ""]);
  });

  it("starts the first entry on an empty list", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TextList value={null} onChange={onChange} />);

    await user.click(addButton());

    expect(onChange).toHaveBeenCalledExactlyOnceWith([""]);
  });

  it("removes the entry its own button belongs to", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TextList value={["first", "second", "third"]} onChange={onChange} />);

    await user.click(removeButtons()[1]!);

    expect(onChange).toHaveBeenCalledExactlyOnceWith(["first", "third"]);
  });

  it("removes by position where two entries hold the same text", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TextList value={["same", "same"]} onChange={onChange} />);

    await user.click(removeButtons()[0]!);

    expect(onChange).toHaveBeenCalledExactlyOnceWith(["same"]);
  });

  it("writes what is typed back into the entry it was typed in", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TextList value={["first", ""]} onChange={onChange} />);

    await user.type(screen.getAllByRole("textbox")[1]!, "x");

    expect(onChange).toHaveBeenCalledExactlyOnceWith(["first", "x"]);
  });

  it("keeps an entry that was typed empty", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TextList value={["a"]} onChange={onChange} />);

    await user.clear(screen.getAllByRole("textbox")[0]!);

    expect(onChange).toHaveBeenCalledExactlyOnceWith([""]);
  });

  it("disables the add button once the list holds as many entries as it may", () => {
    render(<TextList value={["a", "b", "c"]} onChange={vi.fn()} maxEntries={3} />);

    expect(addButton()).toBeDisabled();
  });

  it("leaves the add button usable below the maximum", () => {
    render(<TextList value={["a", "b"]} onChange={vi.fn()} maxEntries={3} />);

    expect(addButton()).toBeEnabled();
  });
});
