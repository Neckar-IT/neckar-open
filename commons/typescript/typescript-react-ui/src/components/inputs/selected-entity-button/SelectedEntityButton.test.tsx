/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { SelectedEntityButton } from "./SelectedEntityButton";

/**
 * Tests
 */

describe("SelectedEntityButton", () => {
  it("calls onClick when the button is clicked", async () => {
    const onClick = vi.fn();
    render(
      <SelectedEntityButton hideClearButton onClear={vi.fn()} onClick={onClick}>
        Jonas Weber
      </SelectedEntityButton>,
    );

    await userEvent.click(screen.getByRole("button", { name: "Jonas Weber" }));

    expect(onClick).toHaveBeenCalledOnce();
  });

  it("shows the clear button only while an entity is selected and calls onClear", async () => {
    const onClear = vi.fn();
    const { rerender } = render(
      <SelectedEntityButton hideClearButton onClear={onClear}>
        Jonas Weber
      </SelectedEntityButton>,
    );
    expect(screen.getAllByRole("button")).toHaveLength(1);

    rerender(
      <SelectedEntityButton hideClearButton={false} onClear={onClear}>
        Jonas Weber
      </SelectedEntityButton>,
    );
    const buttons = screen.getAllByRole("button");
    expect(buttons).toHaveLength(2);
    await userEvent.click(buttons[1]!);

    expect(onClear).toHaveBeenCalledOnce();
  });
});
