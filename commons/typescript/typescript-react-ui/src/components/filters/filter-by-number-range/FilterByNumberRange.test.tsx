import { fireEvent, render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { FilterByNumberRange } from "./FilterByNumberRange";

describe("FilterByNumberRange", () => {
  it("shows the label alone and no X while no bound is set", () => {
    render(<FilterByNumberRange label="Amount" numberKind="Integer" value={{}} onChange={vi.fn()} />);

    expect(screen.getByRole("button", { name: "Amount" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Remove Amount" })).not.toBeInTheDocument();
  });

  it.each([
    [{ min: 3, max: 9 }, "Amount: 3 – 9"],
    [{ min: 3 }, "Amount: from 3"],
    [{ max: 9 }, "Amount: until 9"],
    [{ min: 4, max: 4 }, "Amount: 4"],
  ])("names the range %o in the button", (value, name) => {
    render(<FilterByNumberRange label="Amount" numberKind="Integer" value={value} onChange={vi.fn()} />);

    expect(screen.getByRole("button", { name })).toBeInTheDocument();
  });

  it("shows an amount in cents in euro", () => {
    render(<FilterByNumberRange label="Total" numberKind="EuroCents" value={{ min: 6470 }} onChange={vi.fn()} />);

    expect(screen.getByRole("button", { name: "Total: from 64.70 €" })).toBeInTheDocument();
  });

  it("clears both bounds through its X", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<FilterByNumberRange label="Amount" numberKind="Integer" value={{ min: 3, max: 9 }} onChange={onChange} />);

    await user.click(screen.getByRole("button", { name: "Remove Amount" }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith({});
  });

  it("sets one bound and keeps the other in the panel form", () => {
    const onChange = vi.fn();
    render(<FilterByNumberRange form="Panel" label="Amount" numberKind="Integer" value={{ max: 9 }} onChange={onChange} />);

    const lower = screen.getByRole("textbox", { name: "Amount from" });
    fireEvent.change(lower, { target: { value: "2" } });
    fireEvent.blur(lower);

    expect(onChange).toHaveBeenLastCalledWith({ min: 2, max: 9 });
  });

  describe("Panel form", () => {
    it("stays neutral without an X while no bound is set", () => {
      render(<FilterByNumberRange form="Panel" label="Year" numberKind="Year" value={{}} onChange={vi.fn()} />);

      expect(screen.queryByRole("button", { name: "Clear" })).not.toBeInTheDocument();
      for (const field of screen.getAllByRole("textbox")) {
        expect(field.closest("[data-active]")).toHaveAttribute("data-active", "false");
      }
    });

    it("marks only the set bound and clears only that bound through its own X", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<FilterByNumberRange form="Panel" label="Year" numberKind="Year" value={{ min: 1900 }} onChange={onChange} />);

      const lower = screen.getByRole("textbox", { name: "Year from" });
      const upper = screen.getByRole("textbox", { name: "Year until" });
      expect(lower.closest("[data-active]")).toHaveAttribute("data-active", "true");
      expect(lower.closest("[data-active]")?.firstElementChild).toHaveClass("bg-primary/10", "border-primary/40", "text-primary");
      expect(upper.closest("[data-active]")).toHaveAttribute("data-active", "false");

      await user.click(screen.getByRole("button", { name: "Clear" }));

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ min: undefined });
    });

    it("gives each of two set bounds its own X", async () => {
      const user = userEvent.setup();
      const onChange = vi.fn();
      render(<FilterByNumberRange form="Panel" label="Year" numberKind="Year" value={{ min: 1900, max: 2000 }} onChange={onChange} />);

      await user.click(screen.getAllByRole("button", { name: "Clear" })[1] as HTMLElement);

      expect(onChange).toHaveBeenCalledExactlyOnceWith({ min: 1900, max: undefined });
    });
  });
});
