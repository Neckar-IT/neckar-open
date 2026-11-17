import { fireEvent, render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import { Slider } from "./Slider";

describe("Slider", () => {
  it("renders one thumb when neither value nor defaultValue is set", () => {
    render(<Slider max={10} />);

    expect(screen.getAllByRole("slider")).toHaveLength(1);
  });

  it("shows the controlled number value", () => {
    render(<Slider max={10} value={7} />);

    expect(screen.getByRole("slider")).toHaveAttribute("aria-valuenow", "7");
  });

  it("reports a changed value as a single number", () => {
    const onValueChange = vi.fn<(value: number) => void>();
    render(<Slider defaultValue={3} max={10} onValueChange={onValueChange} />);

    fireEvent.keyDown(screen.getByRole("slider"), { key: "ArrowRight" });

    expect(onValueChange).toHaveBeenCalledWith(4);
  });

  it("starts an uncontrolled slider at min", () => {
    render(<Slider max={10} min={2} />);

    expect(screen.getByRole("slider")).toHaveAttribute("aria-valuenow", "2");
  });

  it("reports a committed value as a single number", () => {
    const onValueCommit = vi.fn<(value: number) => void>();
    render(<Slider defaultValue={3} max={10} onValueCommit={onValueCommit} />);

    fireEvent.keyDown(screen.getByRole("slider"), { key: "ArrowRight" });

    expect(onValueCommit).toHaveBeenCalledWith(4);
  });
});
