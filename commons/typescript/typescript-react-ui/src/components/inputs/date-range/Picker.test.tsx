/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { userEvent } from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { type DateRange, DateRangePicker } from "./Picker";

/**
 * Config
 */

const defaultMonth = new Date(2024, 5, 28);

/**
 * Tests
 */

describe("DateRangePicker", () => {
  it("should render the component and open the popover", async () => {
    const user = userEvent.setup();
    render(<DateRangePicker defaultMonth={defaultMonth} value={undefined} onChange={vi.fn()} />);

    expect(screen.getByText("Pick a Period")).toBeInTheDocument();
    expect(screen.queryByText("June 2024")).toBeNull();

    await user.click(screen.getByText("Pick a Period"));

    expect(screen.getByText("June 2024")).toBeInTheDocument();
  });

  it("should show the selected date range", () => {
    render(
      <DateRangePicker defaultMonth={defaultMonth} value={{ from: new Date(2024, 5, 28), to: new Date(2024, 5, 30) }} onChange={vi.fn()} />,
    );

    expect(screen.getByTestId("selected-date-range")).toHaveTextContent("06/28/2024 - 06/30/2024");
  });

  it("should set the value to the beginning and end of a day if only one date was selected", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<DateRangePicker defaultMonth={defaultMonth} initiallyOpen={true} value={undefined} onChange={onChange} />);

    await user.click(screen.getByText("10"));

    expect(onChange).toHaveBeenCalledWith({
      from: new Date(2024, 5, 10, 0, 0, 0),
      to: new Date(2024, 5, 10, 23, 59, 59, 999),
    });
  });

  it("should set the value to the selected dates (end of day for the 'to' date)", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    const TestComponent = () => {
      const [value, setValue] = useState<DateRange | undefined>(undefined);

      return (
        <DateRangePicker
          defaultMonth={defaultMonth}
          initiallyOpen={true}
          value={value}
          onChange={(value) => {
            setValue(value);
            onChange(value);
          }}
        />
      );
    };

    render(<TestComponent />);

    await user.click(screen.getByText("10"));
    await user.click(screen.getByText("12"));

    expect(onChange.mock.calls).toEqual([
      [
        {
          from: new Date(2024, 5, 10, 0, 0, 0),
          to: new Date(2024, 5, 10, 23, 59, 59, 999),
        },
      ],
      [
        {
          from: new Date(2024, 5, 10, 0, 0, 0),
          to: new Date(2024, 5, 12, 23, 59, 59, 999),
        },
      ],
    ]);
  });

  it("should set the defined periods with the quick selects", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <DateRangePicker
        initiallyOpen={true}
        quickSelects={[{ title: "2024, Q1", from: new Date(2024, 0, 1), to: new Date(2024, 2, 31) }]}
        value={undefined}
        onChange={onChange}
      />,
    );

    await user.click(screen.getByText("2024, Q1"));

    expect(onChange).toHaveBeenCalledWith({
      from: new Date(2024, 0, 1, 0, 0, 0),
      to: new Date(2024, 2, 31, 23, 59, 59, 999),
    });
  });
});
