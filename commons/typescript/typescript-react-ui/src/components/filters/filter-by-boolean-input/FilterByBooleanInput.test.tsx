import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { FilterByBooleanInput } from "./FilterByBooleanInput";

describe("FilterByBooleanInput", () => {
  it.each(["Inline", "Panel"] as const)("toggles the value and marks it pressed in the %s form", async (form) => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<FilterByBooleanInput form={form} label="Hanging only" value={true} onChange={onChange} />);

    const toggle = screen.getByRole("button", { name: "Hanging only" });
    expect(toggle).toHaveAttribute("aria-pressed", "true");
    await user.click(toggle);

    expect(onChange).toHaveBeenCalledExactlyOnceWith(false);
  });
});
