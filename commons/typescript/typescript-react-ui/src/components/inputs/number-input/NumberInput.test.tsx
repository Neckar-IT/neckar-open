/**
 * Imports
 */

import { fireEvent, render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import { NumberInput } from "./NumberInput";

/**
 * Tests
 *
 * The increment/decrement/clear logic is locale-independent and is asserted via the numeric
 * `onChange` arguments. The locale-dependent display string is intentionally not asserted.
 */

describe("NumberInput", () => {
  const decrementButton = () => screen.getByRole("button", { name: "Decrease" });
  const incrementButton = () => screen.getByRole("button", { name: "Increase" });

  it("places the clear button after the increment button", () => {
    render(<NumberInput value={5} onChange={vi.fn()} showButtons />);

    expect(screen.getAllByRole("button").map((button) => button.getAttribute("aria-label"))).toEqual(["Decrease", "Increase", "Clear"]);
  });

  it("places addOnRight after the increment button", () => {
    render(<NumberInput value={5} onChange={vi.fn()} showButtons addOnRight={<button type="button" aria-label="Extra" />} />);

    expect(screen.getAllByRole("button").map((button) => button.getAttribute("aria-label"))).toEqual(["Decrease", "Increase", "Extra"]);
  });

  it("places addOnLeft before the decrement button", () => {
    render(<NumberInput value={5} onChange={vi.fn()} showButtons addOnLeft={<button type="button" aria-label="Extra" />} />);

    expect(screen.getAllByRole("button").map((button) => button.getAttribute("aria-label"))).toEqual([
      "Extra",
      "Decrease",
      "Increase",
      "Clear",
    ]);
  });

  it("increments by the step", () => {
    const onChange = vi.fn();
    render(<NumberInput value={5} onChange={onChange} step={1} showButtons />);

    fireEvent.click(incrementButton());

    expect(onChange).toHaveBeenCalledExactlyOnceWith(6);
  });

  it("decrements by the step", () => {
    const onChange = vi.fn();
    render(<NumberInput value={5} onChange={onChange} step={1} showButtons />);

    fireEvent.click(decrementButton());

    expect(onChange).toHaveBeenCalledExactlyOnceWith(4);
  });

  it("clamps the increment to max", () => {
    const onChange = vi.fn();
    render(<NumberInput value={10} onChange={onChange} step={1} max={10} showButtons />);

    fireEvent.click(incrementButton());

    expect(onChange).toHaveBeenCalledExactlyOnceWith(10);
  });

  it("clamps the decrement to min", () => {
    const onChange = vi.fn();
    render(<NumberInput value={0} onChange={onChange} step={1} min={0} showButtons />);

    fireEvent.click(decrementButton());

    expect(onChange).toHaveBeenCalledExactlyOnceWith(0);
  });

  it("increments from an undefined value to max", () => {
    const onChange = vi.fn();
    render(<NumberInput value={undefined} onChange={onChange} max={10} showButtons />);

    fireEvent.click(incrementButton());

    expect(onChange).toHaveBeenCalledExactlyOnceWith(10);
  });

  it("increments from an undefined value to 0 when no max is set", () => {
    const onChange = vi.fn();
    render(<NumberInput value={undefined} onChange={onChange} showButtons />);

    fireEvent.click(incrementButton());

    expect(onChange).toHaveBeenCalledExactlyOnceWith(0);
  });

  it("decrements from an undefined value to min", () => {
    const onChange = vi.fn();
    render(<NumberInput value={undefined} onChange={onChange} min={2} showButtons />);

    fireEvent.click(decrementButton());

    expect(onChange).toHaveBeenCalledExactlyOnceWith(2);
  });

  it("increments via the ArrowUp key", () => {
    const onChange = vi.fn();
    render(<NumberInput value={5} onChange={onChange} step={1} />);

    fireEvent.keyUp(screen.getByRole("textbox"), { key: "ArrowUp" });

    expect(onChange).toHaveBeenCalledExactlyOnceWith(6);
  });

  it("decrements via the ArrowDown key", () => {
    const onChange = vi.fn();
    render(<NumberInput value={5} onChange={onChange} step={1} />);

    fireEvent.keyUp(screen.getByRole("textbox"), { key: "ArrowDown" });

    expect(onChange).toHaveBeenCalledExactlyOnceWith(4);
  });

  it("reports undefined when the clear button is clicked", () => {
    const onChange = vi.fn();
    render(<NumberInput value={5} onChange={onChange} />);

    fireEvent.click(screen.getByRole("button", { name: "Clear" }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(undefined);
  });
});
