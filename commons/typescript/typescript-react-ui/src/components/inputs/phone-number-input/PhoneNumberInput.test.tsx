/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PhoneNumberInput } from "./PhoneNumberInput";

/**
 * Tests
 *
 * An empty input is `null` in the form state, on change and on blur alike.
 */

describe("PhoneNumberInput", () => {
  it("emits null when an empty input loses focus", async () => {
    const onChange = vi.fn();
    render(<PhoneNumberInput value={null} onChange={onChange} />);

    await userEvent.click(screen.getByRole("textbox"));
    await userEvent.tab();

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith(null);
  });

  it("emits null when an input of only whitespace loses focus", async () => {
    const onChange = vi.fn();
    render(<PhoneNumberInput value="   " onChange={onChange} />);

    await userEvent.click(screen.getByRole("textbox"));
    await userEvent.tab();

    expect(onChange).toHaveBeenCalledWith(null);
  });

  it("emits the formatted number when a filled input loses focus", async () => {
    const onChange = vi.fn();
    render(<PhoneNumberInput value="+49 7473 12345" onChange={onChange} />);

    await userEvent.click(screen.getByRole("textbox"));
    await userEvent.tab();

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange.mock.calls[0]?.[0]).toEqual(expect.stringContaining("7473"));
  });
});
