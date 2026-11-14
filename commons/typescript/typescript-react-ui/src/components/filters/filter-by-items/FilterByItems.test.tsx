/**
 * Imports
 */

import { elementAt, render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { FilterByItems } from "./FilterByItems";

/**
 * Tests
 *
 * FilterByItems is a popover filter over a fixed option list, in single- or multiple-select mode.
 */

const options = [
  { value: "a", label: "Active" },
  { value: "d", label: "Done" },
  { value: "p", label: "Pending" },
];

/** Opens the popover by clicking the trigger (always the first button). */
const openPopover = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(elementAt(screen.getAllByRole("button"), 0));
};

describe("FilterByItems", () => {
  describe("single mode", () => {
    it("shows the placeholder when nothing is selected", () => {
      render(<FilterByItems mode="single" value={undefined} onChange={vi.fn()} options={options} placeholder="Status" />);

      expect(screen.getByText("Status")).toBeInTheDocument();
    });

    it("shows the selected option's label in the trigger", () => {
      render(<FilterByItems mode="single" value="a" onChange={vi.fn()} options={options} placeholder="Status" />);

      expect(screen.getByText("Active")).toBeInTheDocument();
    });

    it("reports the picked value and is selectable from the popover", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<FilterByItems mode="single" value={undefined} onChange={onChange} options={options} placeholder="Status" />);

      await openPopover(user);
      await user.click(screen.getByRole("option", { name: "Done" }));

      expect(onChange).toHaveBeenCalledExactlyOnceWith("d");
    });

    it("reports undefined when the inline clear button is clicked", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<FilterByItems mode="single" value="a" onChange={onChange} options={options} placeholder="Status" />);

      // With a value present the buttons are [trigger, clear]; the clear button is last.
      const buttons = screen.getAllByRole("button");
      await user.click(elementAt(buttons, -1));

      expect(onChange).toHaveBeenCalledExactlyOnceWith(undefined);
    });

    it("disables the trigger when there are no options", () => {
      render(<FilterByItems mode="single" value={undefined} onChange={vi.fn()} options={[]} placeholder="Status" />);

      expect(screen.getByRole("button")).toBeDisabled();
    });
  });

  describe("multiple mode", () => {
    it("adds a value to the selection when an unselected option is picked", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<FilterByItems mode="multiple" value={["a"]} onChange={onChange} options={options} placeholder="Status" />);

      await openPopover(user);
      await user.click(screen.getByRole("option", { name: "Done" }));

      expect(onChange).toHaveBeenCalledExactlyOnceWith(["a", "d"]);
    });

    it("removes a value from the selection when a selected option is picked", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<FilterByItems mode="multiple" value={["a", "d"]} onChange={onChange} options={options} placeholder="Status" />);

      await openPopover(user);
      await user.click(screen.getByRole("option", { name: "Active" }));

      expect(onChange).toHaveBeenCalledExactlyOnceWith(["d"]);
    });

    it("marks a selected option by its checkbox alone, without the check mark of a single choice", async () => {
      const user = userEvent.setup();
      render(<FilterByItems mode="multiple" value={["a"]} onChange={vi.fn()} options={options} placeholder="Status" />);

      await openPopover(user);

      const option = screen.getByRole("option", { name: "Active" });
      expect(option).toHaveAttribute("aria-checked", "true");
      expect(option).not.toHaveAttribute("data-checked");
    });

    it("names up to two selected options in the button and counts three", () => {
      const { rerender } = render(
        <FilterByItems mode="multiple" value={["a", "d"]} onChange={vi.fn()} options={options} placeholder="Status" />,
      );
      expect(screen.getByRole("button", { name: "Status: Active, Done" })).toBeInTheDocument();

      rerender(<FilterByItems mode="multiple" value={["a", "d", "p"]} onChange={vi.fn()} options={options} placeholder="Status" />);
      expect(screen.getByRole("button", { name: "Status: 3 selected" })).toBeInTheDocument();
    });

    it("shows the options as checkboxes in the panel form and unchecks one", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<FilterByItems form="Panel" mode="multiple" value={["a"]} onChange={onChange} options={options} placeholder="Status" />);

      expect(screen.getByRole("group", { name: "Status" })).toBeInTheDocument();
      expect(screen.getByRole("checkbox", { name: "Active" })).toBeChecked();
      expect(screen.getByRole("checkbox", { name: "Done" })).not.toBeChecked();

      await user.click(screen.getByRole("checkbox", { name: "Active" }));

      expect(onChange).toHaveBeenCalledExactlyOnceWith(undefined);
    });

    it("adds a checked option to the selection in the panel form", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<FilterByItems form="Panel" mode="multiple" value={["a"]} onChange={onChange} options={options} placeholder="Status" />);

      await user.click(screen.getByRole("checkbox", { name: "Done" }));

      expect(onChange).toHaveBeenCalledExactlyOnceWith(["a", "d"]);
    });
  });

  describe("single mode in the panel form", () => {
    it("shows the options as a radio group below each other and reports the picked one", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<FilterByItems form="Panel" mode="single" value="a" onChange={onChange} options={options} placeholder="Status" />);

      expect(screen.getByRole("radiogroup", { name: "Status" })).toBeInTheDocument();
      expect(screen.getByRole("radio", { name: "Active" })).toBeChecked();

      await user.click(screen.getByRole("radio", { name: "Done" }));

      expect(onChange).toHaveBeenCalledExactlyOnceWith("d");
    });

    it("shows a custom option component beside its radio", () => {
      const customOptions = options.map((option) => ({ ...option, popoverLabel: <strong>{`Badge ${option.label}`}</strong> }));
      render(<FilterByItems form="Panel" mode="single" value="a" onChange={vi.fn()} options={customOptions} placeholder="Status" />);

      expect(screen.getByRole("radio", { name: "Badge Active" })).toBeChecked();
      expect(screen.getByText("Badge Done")).toBeVisible();
    });

    it("has no frame around the group", () => {
      const { container } = render(
        <FilterByItems form="Panel" mode="single" value="a" onChange={vi.fn()} options={options} placeholder="Status" />,
      );

      expect(container.querySelector("[data-active]")).not.toBeInTheDocument();
      expect(screen.getByRole("radiogroup", { name: "Status" })).not.toHaveClass("border");
    });
  });
});
