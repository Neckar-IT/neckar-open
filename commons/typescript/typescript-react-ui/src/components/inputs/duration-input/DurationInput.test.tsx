/**
 * Imports
 */

import { elementAt, fireEvent, render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import { DurationInput } from "./DurationInput";

/**
 * Tests
 *
 * The value is stored in milliseconds and split into a minutes and a seconds input.
 */

describe("DurationInput", () => {
  // [minutes input, seconds input]
  const inputs = () => screen.getAllByRole("spinbutton");
  // [decrement button, increment button]
  const buttons = () => screen.getAllByRole("button");

  it("derives minutes and seconds from the millisecond value", () => {
    render(<DurationInput value={125_000} onChange={() => {}} />); // 2:05

    expect(inputs()[0]).toHaveValue(2);
    expect(inputs()[1]).toHaveValue(5);
  });

  it("recomputes the value in milliseconds when the minutes change", () => {
    const onChange = vi.fn();
    render(<DurationInput value={5_000} onChange={onChange} />); // 0:05

    fireEvent.change(elementAt(inputs(), 0), { target: { value: "2" } });

    expect(onChange).toHaveBeenCalledExactlyOnceWith((2 * 60 + 5) * 1000);
  });

  it("recomputes the value in milliseconds when the seconds change", () => {
    const onChange = vi.fn();
    render(<DurationInput value={120_000} onChange={onChange} />); // 2:00

    fireEvent.change(elementAt(inputs(), 1), { target: { value: "30" } });

    expect(onChange).toHaveBeenCalledExactlyOnceWith((2 * 60 + 30) * 1000);
  });

  it("treats empty minutes as zero minutes, keeping the seconds", () => {
    const onChange = vi.fn();
    render(<DurationInput value={125_000} onChange={onChange} />); // 2:05

    fireEvent.change(elementAt(inputs(), 0), { target: { value: "" } });

    expect(onChange).toHaveBeenCalledExactlyOnceWith(5 * 1000);
  });

  it("treats empty seconds as zero seconds, keeping the minutes", () => {
    const onChange = vi.fn();
    render(<DurationInput value={125_000} onChange={onChange} />); // 2:05

    fireEvent.change(elementAt(inputs(), 1), { target: { value: "" } });

    expect(onChange).toHaveBeenCalledExactlyOnceWith(2 * 60 * 1000);
  });

  it("ignores a seconds value of 60 or more", () => {
    const onChange = vi.fn();
    render(<DurationInput value={120_000} onChange={onChange} />); // 2:00

    fireEvent.change(elementAt(inputs(), 1), { target: { value: "75" } });

    expect(onChange).not.toHaveBeenCalled();
  });

  it("increments by the step when the increment button is clicked", () => {
    const onChange = vi.fn();
    render(<DurationInput value={5_000} onChange={onChange} step={1_000} />);

    fireEvent.click(elementAt(buttons(), 1));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(6_000);
  });

  it("clamps the increment to max", () => {
    const onChange = vi.fn();
    render(<DurationInput value={10_000} onChange={onChange} step={1_000} max={10_000} />);

    fireEvent.click(elementAt(buttons(), 1));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(10_000);
  });

  it("clamps the decrement to min", () => {
    const onChange = vi.fn();
    render(<DurationInput value={0} onChange={onChange} step={1_000} min={0} />);

    fireEvent.click(elementAt(buttons(), 0));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(0);
  });

  it("increments via the ArrowUp key", () => {
    const onChange = vi.fn();
    render(<DurationInput value={5_000} onChange={onChange} step={1_000} />);

    fireEvent.keyDown(elementAt(inputs(), 0), { key: "ArrowUp" });

    expect(onChange).toHaveBeenCalledExactlyOnceWith(6_000);
  });

  it("decrements via the ArrowDown key", () => {
    const onChange = vi.fn();
    render(<DurationInput value={5_000} onChange={onChange} step={1_000} />);

    fireEvent.keyDown(elementAt(inputs(), 1), { key: "ArrowDown" });

    expect(onChange).toHaveBeenCalledExactlyOnceWith(4_000);
  });
});
