/**
 * Imports
 */

import {
  AdjustableValueAdjustedStringWithTypeType,
  AdjustableValueAutoStringWithTypeType,
  AdjustableValueManualStringWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { elementAt, fireEvent, render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { AdjustableValueTextInput } from "./AdjustableValueTextInput";

/**
 * Tests
 *
 * An adjustable value is `Auto` (the calculated value, input disabled), `Adjusted` (a user override
 * of a calculated value, input editable), `Manual` (a user value without any calculated counterpart,
 * input editable) or `null` (nothing stored and nothing calculated, input disabled). The right-hand
 * toggle switches the override on and off along whichever axis has a calculated counterpart:
 * `Auto ↔ Adjusted` while one exists, `null ↔ Manual` while it does not.
 */

const { Adjusted } = AdjustableValueAdjustedStringWithTypeType;
const { Auto } = AdjustableValueAutoStringWithTypeType;
const { Manual } = AdjustableValueManualStringWithTypeType;

describe("AdjustableValueTextInput", () => {
  describe("Auto mode", () => {
    it("shows the auto value in a disabled input", () => {
      render(<AdjustableValueTextInput value={{ type: Auto, value: "auto-value" }} onChange={vi.fn()} />);

      const input = screen.getByRole("textbox");
      expect(input).toHaveValue("auto-value");
      expect(input).toBeDisabled();
    });

    it("switches to Adjusted mode when the toggle is clicked, seeding the calculated value", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<AdjustableValueTextInput value={{ type: Auto, value: "auto-value" }} onChange={onChange} />);

      // In Auto mode the only button is the mode toggle.
      await user.click(screen.getByRole("button"));

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Adjusted, value: "auto-value", calculated: "auto-value" });
    });
  });

  describe("Adjusted mode", () => {
    const adjusted = { type: Adjusted, value: "override", calculated: "computed" } as const;

    it("shows the override value in an editable input", () => {
      render(<AdjustableValueTextInput value={adjusted} onChange={vi.fn()} />);

      const input = screen.getByRole("textbox");
      expect(input).toHaveValue("override");
      expect(input).toBeEnabled();
    });

    it("reports an Adjusted value carrying the calculated value when the text changes", () => {
      const onChange = vi.fn();
      render(<AdjustableValueTextInput value={adjusted} onChange={onChange} />);

      fireEvent.change(screen.getByRole("textbox"), { target: { value: "edited" } });

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Adjusted, value: "edited", calculated: "computed" });
    });

    it("reports a null override when the text is cleared", () => {
      const onChange = vi.fn();
      render(<AdjustableValueTextInput value={adjusted} onChange={onChange} />);

      fireEvent.change(screen.getByRole("textbox"), { target: { value: "" } });

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Adjusted, value: null, calculated: "computed" });
    });

    it("reverts to Auto mode using the calculated value when the toggle is clicked", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<AdjustableValueTextInput value={adjusted} onChange={onChange} />);

      // In Adjusted mode the buttons are [info popover, mode toggle]; the toggle is last.
      const buttons = screen.getAllByRole("button");
      await user.click(elementAt(buttons, -1));

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Auto, value: "computed" });
    });

    it("reveals the calculated value in the info popover", async () => {
      const user = userEvent.setup();
      render(<AdjustableValueTextInput value={adjusted} onChange={vi.fn()} />);

      // The info popover is the first button (the add-on on the left).
      await user.click(elementAt(screen.getAllByRole("button"), 0));

      expect(screen.getByText("Calculated Value:")).toBeInTheDocument();
      expect(screen.getByText("computed")).toBeInTheDocument();
    });
  });

  describe("Manual mode", () => {
    const manual = { type: Manual, value: "typed-by-hand" } as const;

    it("shows the manual value in an editable input", () => {
      render(<AdjustableValueTextInput value={manual} onChange={vi.fn()} />);

      const input = screen.getByRole("textbox");
      expect(input).toHaveValue("typed-by-hand");
      expect(input).toBeEnabled();
    });

    it("offers a mode toggle but no info popover, as there is no calculated value", () => {
      const { container } = render(<AdjustableValueTextInput value={manual} onChange={vi.fn()} />);

      // The toggle turns the override off rather than resetting it, so it reads as undo without a popover.
      expect(container.querySelector(".lucide-undo")).not.toBeNull();
      expect(container.querySelector(".lucide-pencil")).toBeNull();
      expect(container.querySelector(".lucide-info")).toBeNull();
    });

    it("stays Manual when the text changes", () => {
      const onChange = vi.fn();
      render(<AdjustableValueTextInput value={manual} onChange={onChange} />);

      fireEvent.change(screen.getByRole("textbox"), { target: { value: "edited" } });

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Manual, value: "edited" });
    });

    it("drops the value entirely when the toggle is clicked", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<AdjustableValueTextInput value={manual} onChange={onChange} />);

      // A manual value discloses nothing, so the mode toggle is the only button.
      await user.click(screen.getByRole("button"));

      expect(onChange).toHaveBeenCalledExactlyOnceWith(null);
    });
  });

  describe("No value", () => {
    it("shows an empty disabled input", () => {
      render(<AdjustableValueTextInput value={null} onChange={vi.fn()} />);

      const input = screen.getByRole("textbox");
      expect(input).toHaveValue("");
      expect(input).toBeDisabled();
    });

    it("switches to Manual mode when the toggle is clicked, as there is nothing to adjust against", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<AdjustableValueTextInput value={null} onChange={onChange} />);

      // Nothing is stored and nothing is disclosed, so the mode toggle is the only button.
      await user.click(screen.getByRole("button"));

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Manual, value: null });
    });

    it("offers no info popover, as there is no calculated value", () => {
      const { container } = render(<AdjustableValueTextInput value={null} onChange={vi.fn()} />);

      expect(container.querySelector(".lucide-pencil")).not.toBeNull();
      expect(container.querySelector(".lucide-info")).toBeNull();
    });
  });
});
