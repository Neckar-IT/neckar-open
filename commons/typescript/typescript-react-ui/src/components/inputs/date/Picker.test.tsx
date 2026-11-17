/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { userEvent } from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { DatePicker } from "./Picker";

/**
 * Config
 */

const defaultMonth = new Date(2024, 5, 28);

/**
 * Tests
 */

describe("DatePicker", () => {
  it("should render a button and open the calendar", async () => {
    const user = userEvent.setup();
    render(<DatePicker defaultMonth={defaultMonth} value={undefined} onChange={vi.fn()} />);

    expect(screen.getByText("Pick a Date")).toBeInTheDocument();

    await user.click(screen.getByText("Pick a Date"));
    expect(screen.getByText("June 2024")).toBeInTheDocument();
  });

  it("should show the selected date", () => {
    render(<DatePicker defaultMonth={defaultMonth} value={new Date(2024, 5, 28)} onChange={vi.fn()} />);

    expect(screen.getByTestId("selected-date")).toHaveTextContent("Fri, 06/28/2024");
  });

  it("should select a date and close the popover", async () => {
    const user = userEvent.setup();

    const onChange = vi.fn();
    render(<DatePicker defaultMonth={defaultMonth} initiallyOpen={true} value={undefined} onChange={onChange} />);

    await user.click(screen.getByText("10"));
    expect(onChange).toHaveBeenCalledWith(new Date(2024, 5, 10));
    expect(screen.queryByText("June 2024")).toBeNull();
  });
});
