/**
 * Imports
 */

import {
  AdjustableValueAdjustedDoubleWithTypeType,
  AdjustableValueAutoDoubleWithTypeType,
  AdjustableValueManualDoubleWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { elementAt, fireEvent, render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { AdjustableValueNumberInput } from "./AdjustableValueNumberInput";

/**
 * Tests
 *
 * The numeric counterpart to AdjustableValueTextInput: `Auto` shows the calculated number in a
 * disabled input, `Adjusted` is an editable override, `Manual` is an editable number with no
 * calculated counterpart. NumberInput reports its value on blur.
 */

const { Adjusted } = AdjustableValueAdjustedDoubleWithTypeType;
const { Auto } = AdjustableValueAutoDoubleWithTypeType;
const { Manual } = AdjustableValueManualDoubleWithTypeType;

describe("AdjustableValueNumberInput", () => {
  describe("Auto mode", () => {
    it("disables the input", () => {
      render(<AdjustableValueNumberInput value={{ type: Auto, value: 1234 }} onChange={vi.fn()} />);

      expect(screen.getByRole("textbox")).toBeDisabled();
    });

    it("switches to Adjusted mode when the toggle is clicked, seeding the calculated value", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<AdjustableValueNumberInput value={{ type: Auto, value: 1234 }} onChange={onChange} />);

      await user.click(screen.getByRole("button"));

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Adjusted, value: 1234, calculated: 1234 });
    });
  });

  describe("Adjusted mode", () => {
    const adjusted = { type: Adjusted, value: 42, calculated: 99 } as const;

    it("enables the input", () => {
      render(<AdjustableValueNumberInput value={adjusted} onChange={vi.fn()} />);

      expect(screen.getByRole("textbox")).toBeEnabled();
    });

    it("reports an Adjusted value carrying the calculated value when the edited number is committed", () => {
      const onChange = vi.fn();
      render(<AdjustableValueNumberInput value={adjusted} onChange={onChange} />);

      // NumberInput commits the parsed value on blur.
      const input = screen.getByRole("textbox");
      fireEvent.change(input, { target: { value: "50" } });
      fireEvent.blur(input);

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Adjusted, value: 50, calculated: 99 });
    });

    it("reverts to Auto mode using the calculated value when the toggle is clicked", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<AdjustableValueNumberInput value={adjusted} onChange={onChange} />);

      // In Adjusted mode the buttons are [info popover, mode toggle]; the toggle is last.
      const buttons = screen.getAllByRole("button");
      await user.click(elementAt(buttons, -1));

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Auto, value: 99 });
    });

    it("reveals the translated label and the calculated value in the info popover", async () => {
      const user = userEvent.setup();
      render(<AdjustableValueNumberInput value={adjusted} onChange={vi.fn()} />);

      await user.click(elementAt(screen.getAllByRole("button"), 0));

      expect(screen.getByText("Calculated Value:")).toBeInTheDocument();
      expect(screen.getByText("99")).toBeInTheDocument();
    });

    it("converts the calculated value in the popover the way the input converts the effective one", async () => {
      const user = userEvent.setup();
      // A euro field: both numbers are cents on the wire, euros on screen.
      render(
        <AdjustableValueNumberInput
          value={{ type: Adjusted, value: 12345, calculated: 10000 }}
          onChange={vi.fn()}
          unit="€"
          conversionFactor={0.01}
          fractionDigits={2}
        />,
      );
      expect(screen.getByRole("textbox")).toHaveValue("123.45");

      await user.click(elementAt(screen.getAllByRole("button"), 0));

      expect(screen.getByText("100.00 €")).toBeInTheDocument();
    });

    it("formats the calculated value with the input's grouping and unit position", async () => {
      const user = userEvent.setup();
      render(
        <AdjustableValueNumberInput
          value={{ type: Adjusted, value: 2024, calculated: 1998 }}
          onChange={vi.fn()}
          unit="€"
          unitPosition="prefix"
          useGrouping={false}
        />,
      );
      expect(screen.getByRole("textbox")).toHaveValue("2024");

      await user.click(elementAt(screen.getAllByRole("button"), 0));

      expect(screen.getByText("€ 1998")).toBeInTheDocument();
    });
  });

  describe("Manual mode", () => {
    const manual = { type: Manual, value: 42 } as const;

    it("enables the input", () => {
      render(<AdjustableValueNumberInput value={manual} onChange={vi.fn()} />);

      expect(screen.getByRole("textbox")).toBeEnabled();
    });

    it("offers a mode toggle but no info popover, as there is no calculated value", () => {
      const { container } = render(<AdjustableValueNumberInput value={manual} onChange={vi.fn()} />);

      // The toggle turns the override off rather than resetting it, so it reads as undo without a popover.
      expect(container.querySelector(".lucide-undo")).not.toBeNull();
      expect(container.querySelector(".lucide-pencil")).toBeNull();
      expect(container.querySelector(".lucide-info")).toBeNull();
    });

    it("stays Manual when the edited number is committed", () => {
      const onChange = vi.fn();
      render(<AdjustableValueNumberInput value={manual} onChange={onChange} />);

      const input = screen.getByRole("textbox");
      fireEvent.change(input, { target: { value: "50" } });
      fireEvent.blur(input);

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Manual, value: 50 });
    });

    it("drops the value entirely when the toggle is clicked", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<AdjustableValueNumberInput value={manual} onChange={onChange} />);

      // A manual value discloses nothing, so the mode toggle is the only button.
      await user.click(screen.getByRole("button"));

      expect(onChange).toHaveBeenCalledExactlyOnceWith(null);
    });

    it("replaces NumberInput's clear button rather than sitting beside it", () => {
      const { container } = render(<AdjustableValueNumberInput value={manual} onChange={vi.fn()} />);

      // The toggle is the add-on, and an add-on is what NumberInput renders instead of its clear
      // button — otherwise an X would sit where the sibling fields show a mode toggle.
      expect(container.querySelector(".lucide-x")).toBeNull();
    });
  });

  /**
   * The state an optional adjustable field starts in — nothing stored and nothing calculated. Before
   * this the component read `value.value` unconditionally and threw on the first render of any form
   * holding such a field.
   */
  describe("No value", () => {
    it("shows an empty disabled input", () => {
      render(<AdjustableValueNumberInput value={null} onChange={vi.fn()} />);

      const input = screen.getByRole("textbox");
      expect(input).toHaveValue("");
      expect(input).toBeDisabled();
    });

    it("switches to Manual mode when the toggle is clicked, as there is nothing to adjust against", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<AdjustableValueNumberInput value={null} onChange={onChange} />);

      // Nothing is stored and nothing is disclosed, so the mode toggle is the only button.
      await user.click(screen.getByRole("button"));

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Manual, value: null });
    });

    it("offers no info popover, as there is no calculated value", () => {
      const { container } = render(<AdjustableValueNumberInput value={null} onChange={vi.fn()} />);

      expect(container.querySelector(".lucide-pencil")).not.toBeNull();
      expect(container.querySelector(".lucide-info")).toBeNull();
    });
  });
});
