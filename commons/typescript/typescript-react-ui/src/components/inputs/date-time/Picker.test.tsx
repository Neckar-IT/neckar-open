import { fireEvent, render, screen } from "@test/test-utils";
import { userEvent } from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { DateTimePicker } from "./Picker";

const june28At0930 = new Date(2024, 5, 28, 9, 30);

describe("DateTimePicker", () => {
  it("renders the placeholder when no value is set", () => {
    render(<DateTimePicker value={undefined} onChange={vi.fn()} />);

    expect(screen.getByText("Pick a Date")).toBeInTheDocument();
  });

  it("renders the formatted date-and-time when a value is set", () => {
    render(<DateTimePicker value={june28At0930} onChange={vi.fn()} />);

    expect(screen.getByText(/06\/28\/2024.*09:30/)).toBeInTheDocument();
  });

  it("collapses the trigger to the date alone when the time matches the default time-of-day", () => {
    const june28StartOfDay = new Date(2024, 5, 28, 0, 0, 0, 0);
    render(<DateTimePicker value={june28StartOfDay} onChange={vi.fn()} />);

    expect(screen.getByText("06/28/2024")).toBeInTheDocument();
    expect(screen.queryByText(/00:00/)).toBeNull();
  });

  it("opens the popover with calendar and time input when the trigger is clicked", async () => {
    const user = userEvent.setup();
    render(<DateTimePicker value={undefined} defaultMonth={june28At0930} onChange={vi.fn()} />);

    expect(screen.queryByText("June 2024")).toBeNull();
    await user.click(screen.getByText("Pick a Date"));
    expect(screen.getByText("June 2024")).toBeInTheDocument();
    // Time input lives next to the calendar, empty until a value is picked.
    expect(screen.getByDisplayValue("")).toBeInTheDocument();
  });

  it("renders the time-input wrapper without a redundant top border", () => {
    // The popover has a `<Separator>` directly above the time-input row; a `border-t`
    // class on the wrapper would render a second 1px line on top of the separator.
    render(<DateTimePicker value={june28At0930} defaultMonth={june28At0930} initiallyOpen={true} onChange={vi.fn()} />);

    const timeInput = screen.getByDisplayValue("09:30");
    const wrapper = timeInput.closest("div.p-3");
    expect(wrapper).not.toBeNull();
    expect(wrapper?.className).not.toMatch(/(?:^|\s)border-t(?:$|\s)/);
  });

  it("seeds the time at local midnight on first pick when defaultTimeOfDay is unset", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    const TestComponent = () => {
      const [value, setValue] = useState<Date | undefined>(undefined);
      return (
        <DateTimePicker
          value={value}
          defaultMonth={june28At0930}
          initiallyOpen={true}
          onChange={(next) => {
            setValue(next);
            onChange(next);
          }}
        />
      );
    };

    render(<TestComponent />);

    await user.click(screen.getByText("10"));
    expect(onChange).toHaveBeenCalledWith(new Date(2024, 5, 10, 0, 0, 0, 0));
  });

  it("seeds the time at end-of-day on first pick when defaultTimeOfDay is 'end'", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    const TestComponent = () => {
      const [value, setValue] = useState<Date | undefined>(undefined);
      return (
        <DateTimePicker
          value={value}
          defaultMonth={june28At0930}
          initiallyOpen={true}
          defaultTimeOfDay="end"
          onChange={(next) => {
            setValue(next);
            onChange(next);
          }}
        />
      );
    };

    render(<TestComponent />);

    await user.click(screen.getByText("10"));
    expect(onChange).toHaveBeenCalledWith(new Date(2024, 5, 10, 23, 59, 59, 999));
  });

  it("seeds the time at noon on first pick when defaultTimeOfDay is 'middle'", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    const TestComponent = () => {
      const [value, setValue] = useState<Date | undefined>(undefined);
      return (
        <DateTimePicker
          value={value}
          defaultMonth={june28At0930}
          initiallyOpen={true}
          defaultTimeOfDay="middle"
          onChange={(next) => {
            setValue(next);
            onChange(next);
          }}
        />
      );
    };

    render(<TestComponent />);

    await user.click(screen.getByText("10"));
    expect(onChange).toHaveBeenCalledWith(new Date(2024, 5, 10, 12, 0, 0, 0));
  });

  it("seeds the time from an explicit hours/minutes object on first pick", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    const TestComponent = () => {
      const [value, setValue] = useState<Date | undefined>(undefined);
      return (
        <DateTimePicker
          value={value}
          defaultMonth={june28At0930}
          initiallyOpen={true}
          defaultTimeOfDay={{ hours: 8, minutes: 0 }}
          onChange={(next) => {
            setValue(next);
            onChange(next);
          }}
        />
      );
    };

    render(<TestComponent />);

    await user.click(screen.getByText("10"));
    expect(onChange).toHaveBeenCalledWith(new Date(2024, 5, 10, 8, 0, 0, 0));
  });

  it("preserves the existing time when the user picks a new day", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    render(
      <DateTimePicker value={june28At0930} defaultMonth={june28At0930} initiallyOpen={true} defaultTimeOfDay="end" onChange={onChange} />,
    );

    // Pick the in-month '10' cell — even though defaultTimeOfDay is "end", the existing
    // 09:30 time portion must be preserved (the default only seeds the very first pick).
    const candidates = screen.getAllByText("10");
    const inMonthDay = candidates.find((el) => el.closest(".rdp-outside") == null);
    if (inMonthDay == null) {
      throw new Error("expected the in-month '10' cell to be present in the calendar grid");
    }
    await user.click(inMonthDay);
    expect(onChange).toHaveBeenCalledWith(new Date(2024, 5, 10, 9, 30, 0, 0));
  });

  it("updates only the time portion when the time input changes", () => {
    const onChange = vi.fn();

    render(<DateTimePicker value={june28At0930} defaultMonth={june28At0930} initiallyOpen={true} onChange={onChange} />);

    const timeInput = screen.getByDisplayValue("09:30");
    fireEvent.change(timeInput, { target: { value: "14:45" } });
    expect(onChange).toHaveBeenLastCalledWith(new Date(2024, 5, 28, 14, 45, 0, 0));
  });

  it("adopts a quick-select date+time verbatim, ignoring defaultTimeOfDay", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const tomorrowAt0900 = new Date(2024, 5, 29, 9, 0, 0, 0);

    render(
      <DateTimePicker
        value={undefined}
        defaultMonth={june28At0930}
        initiallyOpen={true}
        defaultTimeOfDay="end"
        quickSelects={[{ title: "Tomorrow 09:00", date: tomorrowAt0900 }]}
        onChange={onChange}
      />,
    );

    await user.click(screen.getByText("Tomorrow 09:00"));
    // Quick-select preserves the time portion of the supplied date — it does not seed
    // from defaultTimeOfDay.
    expect(onChange).toHaveBeenCalledWith(tomorrowAt0900);
  });

  it("renders the supplied trigger element instead of the default button", () => {
    render(<DateTimePicker value={undefined} onChange={vi.fn()} trigger={<button type="button">custom-trigger</button>} />);

    expect(screen.getByText("custom-trigger")).toBeInTheDocument();
    // The default trigger label must not appear when a custom trigger is supplied.
    expect(screen.queryByText("Pick a Date")).toBeNull();
  });

  it("clears the value when the in-popover Clear button is clicked", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    render(<DateTimePicker value={june28At0930} defaultMonth={june28At0930} initiallyOpen={true} onChange={onChange} />);

    await user.click(screen.getByText("Clear"));
    expect(onChange).toHaveBeenCalledWith(undefined);
  });

  it("renders an X-clear at the trigger when a value is set", () => {
    render(<DateTimePicker value={june28At0930} onChange={vi.fn()} />);

    // Empty popover, value set → trigger Button + ClearButton from InputWrapper = 2 buttons.
    expect(screen.getAllByRole("button")).toHaveLength(2);
  });

  it("does not render the trigger X-clear when no value is set", () => {
    render(<DateTimePicker value={undefined} onChange={vi.fn()} />);

    // Empty popover, no value → trigger Button only.
    expect(screen.getAllByRole("button")).toHaveLength(1);
  });

  it("clears the value when the trigger X-clear is clicked", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    render(<DateTimePicker value={june28At0930} onChange={onChange} />);

    // Trigger Button is index 0, the InputWrapper's X-clear is index 1 (popover stays closed
    // because the X-clear lives outside `PopoverTrigger`).
    const buttons = screen.getAllByRole("button");
    await user.click(buttons[1]!);
    expect(onChange).toHaveBeenCalledWith(undefined);
  });
});
