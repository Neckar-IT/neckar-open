/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { SelectFromList } from "./SelectFromList";

/**
 * Tests
 *
 * SelectFromList binds a typed object list to the primitive Select, matching the selected value by
 * deep equality and mapping option clicks back to the corresponding object.
 */

interface Option {
  name: string;
  group: number;
}

const options: Option[] = [
  { name: "Alpha", group: 1 },
  { name: "Beta", group: 2 },
];

const renderItem = (item: Option) => item.name;

describe("SelectFromList", () => {
  it("calls onChange with the selected object when an option is picked", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<SelectFromList options={options} itemRenderer={renderItem} value={null} onChange={onChange} />);

    await user.click(screen.getByRole("combobox"));
    await user.click(screen.getByRole("option", { name: "Beta" }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith({ name: "Beta", group: 2 });
  });

  it("marks the option that deep-equals the value as selected, even for a different object instance", async () => {
    const user = userEvent.setup();
    // A structurally equal but distinct instance must still resolve to the matching option.
    const equalButDistinct: Option = { name: "Beta", group: 2 };
    render(<SelectFromList options={options} itemRenderer={renderItem} value={equalButDistinct} onChange={vi.fn()} />);

    await user.click(screen.getByRole("combobox"));

    expect(screen.getByRole("option", { name: "Beta" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("option", { name: "Alpha" })).toHaveAttribute("aria-selected", "false");
  });

  it("selects no option when the value is not present in the options", async () => {
    const user = userEvent.setup();
    render(<SelectFromList options={options} itemRenderer={renderItem} value={{ name: "Gamma", group: 3 }} onChange={vi.fn()} />);

    await user.click(screen.getByRole("combobox"));

    expect(screen.getByRole("option", { name: "Alpha" })).toHaveAttribute("aria-selected", "false");
    expect(screen.getByRole("option", { name: "Beta" })).toHaveAttribute("aria-selected", "false");
  });

  it("reports null when the clear button is clicked", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<SelectFromList options={options} itemRenderer={renderItem} value={options[0]} onChange={onChange} />);

    // The trigger has role combobox, so the only button is the clear button.
    await user.click(screen.getByRole("button"));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(null);
  });
});
