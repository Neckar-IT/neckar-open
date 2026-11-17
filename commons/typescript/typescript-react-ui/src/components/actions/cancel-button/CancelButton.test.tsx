import { fireEvent, render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import { CancelButton } from "./CancelButton";

describe("CancelButton", () => {
  it("prevents the mousedown default so it never steals focus from the active form field", () => {
    render(<CancelButton />);
    const prevented = !fireEvent.mouseDown(screen.getByRole("button"));
    expect(prevented).toBe(true);
  });

  it("still forwards a caller-supplied onMouseDown", () => {
    const onMouseDown = vi.fn();
    render(<CancelButton onMouseDown={onMouseDown} />);
    fireEvent.mouseDown(screen.getByRole("button"));
    expect(onMouseDown).toHaveBeenCalledTimes(1);
  });

  it("fires onClick on a regular click", () => {
    const onClick = vi.fn();
    render(<CancelButton onClick={onClick} />);
    fireEvent.click(screen.getByRole("button"));
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
