/**
 * Imports
 */

import {
  AdjustableValueAdjustedStringWithTypeType,
  AdjustableValueAutoStringWithTypeType,
  AdjustableValueManualStringWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { elementAt, render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { AdjustableValueSelect } from "./AdjustableValueSelect";

/**
 * Tests
 *
 * An adjustable select value is `Auto` (the calculated value, select disabled), `Adjusted` (a user
 * override of a calculated value, select editable), `Manual` (a user override without any calculated
 * counterpart, select editable) or `null` (inherited with nothing to inherit, select disabled). The
 * right-hand toggle flips the override on and off: `Auto ↔ Adjusted` while a calculated value exists,
 * `null ↔ Manual` while it does not.
 */

const { Adjusted } = AdjustableValueAdjustedStringWithTypeType;
const { Auto } = AdjustableValueAutoStringWithTypeType;
const { Manual } = AdjustableValueManualStringWithTypeType;

const options = ["silver", "black"];
const renderItem = (item: string) => item;

describe("AdjustableValueSelect", () => {
  describe("Auto mode", () => {
    it("shows the auto value in a disabled select", () => {
      render(
        <AdjustableValueSelect options={options} itemRenderer={renderItem} value={{ type: Auto, value: "silver" }} onChange={vi.fn()} />,
      );

      const select = screen.getByRole("combobox");
      expect(select).toHaveTextContent("silver");
      expect(select).toBeDisabled();
    });

    it("switches to Adjusted mode when the toggle is clicked, seeding the calculated value", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(
        <AdjustableValueSelect options={options} itemRenderer={renderItem} value={{ type: Auto, value: "silver" }} onChange={onChange} />,
      );

      // In Auto mode the only button is the mode toggle.
      await user.click(screen.getByRole("button"));

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Adjusted, value: "silver", calculated: "silver" });
    });
  });

  describe("Adjusted mode", () => {
    const adjusted = { type: Adjusted, value: "black", calculated: "silver" } as const;

    it("shows the override value in an editable select", () => {
      render(<AdjustableValueSelect options={options} itemRenderer={renderItem} value={adjusted} onChange={vi.fn()} />);

      const select = screen.getByRole("combobox");
      expect(select).toHaveTextContent("black");
      expect(select).toBeEnabled();
    });

    it("reports an Adjusted value carrying the calculated value when another option is picked", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      // A third option distinct from both the override ("black") and the calculated value ("silver"),
      // so the assertion fails an implementation that wrongly writes the picked value as `calculated`.
      render(
        <AdjustableValueSelect options={["silver", "black", "bronze"]} itemRenderer={renderItem} value={adjusted} onChange={onChange} />,
      );

      await user.click(screen.getByRole("combobox"));
      await user.click(screen.getByRole("option", { name: "bronze" }));

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Adjusted, value: "bronze", calculated: "silver" });
    });

    it("reverts to Auto mode using the calculated value when the toggle is clicked", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<AdjustableValueSelect options={options} itemRenderer={renderItem} value={adjusted} onChange={onChange} />);

      // In Adjusted mode the buttons are [info popover, mode toggle]; the toggle is last.
      await user.click(elementAt(screen.getAllByRole("button"), -1));

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Auto, value: "silver" });
    });

    it("reveals the calculated value in the info popover", async () => {
      const user = userEvent.setup();
      render(<AdjustableValueSelect options={options} itemRenderer={renderItem} value={adjusted} onChange={vi.fn()} />);

      // The info popover is the first button (the add-on on the left).
      await user.click(elementAt(screen.getAllByRole("button"), 0));

      expect(screen.getByText("Calculated Value:")).toBeInTheDocument();
    });
  });

  describe("Manual mode", () => {
    const manual = { type: Manual, value: "black" } as const;

    it("shows the manual value in an editable select without an info popover", () => {
      const { container } = render(<AdjustableValueSelect options={options} itemRenderer={renderItem} value={manual} onChange={vi.fn()} />);

      expect(screen.getByRole("combobox")).toBeEnabled();
      expect(container.querySelector(".lucide-info")).toBeNull();
    });

    it("stays Manual when another option is picked", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<AdjustableValueSelect options={options} itemRenderer={renderItem} value={manual} onChange={onChange} />);

      await user.click(screen.getByRole("combobox"));
      await user.click(screen.getByRole("option", { name: "silver" }));

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Manual, value: "silver" });
    });

    it("reverts to the inherited null state when the toggle is clicked", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<AdjustableValueSelect options={options} itemRenderer={renderItem} value={manual} onChange={onChange} />);

      await user.click(screen.getByRole("button"));

      expect(onChange).toHaveBeenCalledExactlyOnceWith(null);
    });
  });

  describe("Inherited null state", () => {
    it("shows a disabled select with no value", () => {
      render(<AdjustableValueSelect options={options} itemRenderer={renderItem} value={null} onChange={vi.fn()} />);

      expect(screen.getByRole("combobox")).toBeDisabled();
    });

    it("switches to an empty Manual override when the toggle is clicked", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<AdjustableValueSelect options={options} itemRenderer={renderItem} value={null} onChange={onChange} />);

      await user.click(screen.getByRole("button"));

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ type: Manual, value: null });
    });
  });
});
