/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { userEvent } from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PickerShell } from "./PickerShell";

/**
 * Tests
 */

describe("PickerShell", () => {
  it("closes the popover after a quick select and calls its handler", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(
      <PickerShell
        hasValue={false}
        initiallyOpen={true}
        quickSelects={[{ title: "Today", onSelect }]}
        triggerLabel="Pick"
        clearSelectionLabel="Clear"
        onClear={vi.fn()}
      >
        <div>calendar</div>
      </PickerShell>,
    );

    await user.click(screen.getByText("Today"));

    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(screen.queryByText("calendar")).toBeNull();
  });

  it("closes the popover after clear and calls onClear", async () => {
    const user = userEvent.setup();
    const onClear = vi.fn();
    render(
      <PickerShell hasValue={true} initiallyOpen={true} triggerLabel="Pick" clearSelectionLabel="Clear" onClear={onClear}>
        <div>calendar</div>
      </PickerShell>,
    );

    await user.click(screen.getByText("Clear"));

    expect(onClear).toHaveBeenCalledTimes(1);
    expect(screen.queryByText("calendar")).toBeNull();
  });

  it("offers the close function to a children function", async () => {
    const user = userEvent.setup();
    render(
      <PickerShell hasValue={false} initiallyOpen={true} triggerLabel="Pick" clearSelectionLabel="Clear" onClear={vi.fn()}>
        {(close) => <button onClick={close}>pick day</button>}
      </PickerShell>,
    );

    await user.click(screen.getByText("pick day"));

    expect(screen.queryByText("pick day")).toBeNull();
  });

  it("disables the trigger when readOnly", () => {
    render(
      <PickerShell hasValue={false} readOnly={true} triggerLabel="Pick" clearSelectionLabel="Clear" onClear={vi.fn()}>
        <div>calendar</div>
      </PickerShell>,
    );

    expect(screen.getByRole("button", { name: "Pick" })).toBeDisabled();
  });

  it("names the trigger with ariaLabel", () => {
    render(
      <PickerShell ariaLabel="Start date" hasValue={false} triggerLabel="Pick" clearSelectionLabel="Clear" onClear={vi.fn()}>
        <div>calendar</div>
      </PickerShell>,
    );

    expect(screen.getByRole("button", { name: "Start date" })).toBeInTheDocument();
  });

  it("appends a set value to the ariaLabel", () => {
    render(
      <PickerShell ariaLabel="Start date" hasValue={true} triggerLabel="06/28/2024" clearSelectionLabel="Clear" onClear={vi.fn()}>
        <div>calendar</div>
      </PickerShell>,
    );

    expect(screen.getByRole("button", { name: "Start date: 06/28/2024" })).toBeInTheDocument();
  });

  it("shows the trigger X-clear only when a value is set", () => {
    const { rerender } = render(
      <PickerShell hasValue={false} triggerLabel="Pick" clearSelectionLabel="Clear" onClear={vi.fn()}>
        <div>calendar</div>
      </PickerShell>,
    );
    expect(screen.getAllByRole("button")).toHaveLength(1);

    rerender(
      <PickerShell hasValue={true} triggerLabel="Pick" clearSelectionLabel="Clear" onClear={vi.fn()}>
        <div>calendar</div>
      </PickerShell>,
    );
    expect(screen.getAllByRole("button")).toHaveLength(2);
  });
});
