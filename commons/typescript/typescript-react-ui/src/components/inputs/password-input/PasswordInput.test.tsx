/**
 * Imports
 */

import { elementAt, fireEvent, render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import { PasswordInput } from "./PasswordInput";

/**
 * Tests
 *
 * A password input has no accessible "textbox" role, so the field is queried via the DOM directly.
 */

describe("PasswordInput", () => {
  const passwordField = (container: HTMLElement): HTMLInputElement => {
    const input = container.querySelector("input");
    if (input === null) throw new Error("password input not found");
    return input;
  };

  it("renders the value", () => {
    const { container } = render(<PasswordInput value="hunter2" onChange={() => {}} />);

    expect(passwordField(container)).toHaveValue("hunter2");
  });

  it("renders an empty field when the value is null", () => {
    const { container } = render(<PasswordInput value={null} onChange={() => {}} />);

    expect(passwordField(container)).toHaveValue("");
  });

  it("reports the typed value through onChange", () => {
    const onChange = vi.fn();
    const { container } = render(<PasswordInput value={null} onChange={onChange} />);

    fireEvent.change(passwordField(container), { target: { value: "secret" } });

    expect(onChange).toHaveBeenCalledExactlyOnceWith("secret");
  });

  it("reports null when cleared to an empty string", () => {
    const onChange = vi.fn();
    const { container } = render(<PasswordInput value="secret" onChange={onChange} />);

    fireEvent.change(passwordField(container), { target: { value: "" } });

    expect(onChange).toHaveBeenCalledExactlyOnceWith(null);
  });

  it("masks the input by default", () => {
    const { container } = render(<PasswordInput value="secret" onChange={() => {}} />);

    expect(passwordField(container)).toHaveAttribute("type", "password");
  });

  it("reveals and re-masks the input when the toggle is clicked", () => {
    // With value null the clear button is hidden, so the only button is the show/hide toggle.
    const { container } = render(<PasswordInput value={null} onChange={() => {}} />);
    const toggle = screen.getByRole("button");

    fireEvent.click(toggle);
    expect(passwordField(container)).toHaveAttribute("type", "text");

    fireEvent.click(toggle);
    expect(passwordField(container)).toHaveAttribute("type", "password");
  });

  it("hides the clear button while the value is empty", () => {
    render(<PasswordInput value={null} onChange={() => {}} />);

    // Only the show/hide toggle remains.
    expect(screen.getAllByRole("button")).toHaveLength(1);
  });

  it("reports null when the clear button is clicked", () => {
    const onChange = vi.fn();
    render(<PasswordInput value="secret" onChange={onChange} />);

    // With a value present the buttons are [show/hide toggle, clear]; the clear button is last.
    const allButtons = screen.getAllByRole("button");
    fireEvent.click(elementAt(allButtons, -1));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(null);
  });
});
