/**
 * Imports
 */

import { fireEvent, render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import { TextInput } from "./TextInput";

/**
 * Tests
 */

describe("TextInput", () => {
  it("renders the initial value in the input", () => {
    render(<TextInput value="hello" onChange={() => {}} />);

    expect(screen.getByRole("textbox")).toHaveValue("hello");
  });

  it("renders an empty input when value is null", () => {
    render(<TextInput value={null} onChange={() => {}} />);

    expect(screen.getByRole("textbox")).toHaveValue("");
  });

  it("reports the typed value through onChange", () => {
    const onChange = vi.fn();
    render(<TextInput value={null} onChange={onChange} />);

    fireEvent.change(screen.getByRole("textbox"), { target: { value: "abc" } });

    expect(onChange).toHaveBeenCalledExactlyOnceWith("abc");
  });

  it("reports null when the input is cleared to an empty string", () => {
    const onChange = vi.fn();
    render(<TextInput value="abc" onChange={onChange} />);

    fireEvent.change(screen.getByRole("textbox"), { target: { value: "" } });

    expect(onChange).toHaveBeenCalledExactlyOnceWith(null);
  });

  it("defaults the input type to text", () => {
    render(<TextInput value={null} onChange={() => {}} />);

    expect(screen.getByRole("textbox")).toHaveAttribute("type", "text");
  });

  it("applies the email input type", () => {
    render(<TextInput value={null} onChange={() => {}} type="email" />);

    expect(screen.getByRole("textbox")).toHaveAttribute("type", "email");
  });

  it("applies the url input type", () => {
    render(<TextInput value={null} onChange={() => {}} type="url" />);

    expect(screen.getByRole("textbox")).toHaveAttribute("type", "url");
  });

  it("hides the clear button while the value is null", () => {
    render(<TextInput value={null} onChange={() => {}} />);

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("renders the clear button once the input has a value", () => {
    render(<TextInput value="hello" onChange={() => {}} />);

    expect(screen.getByRole("button")).toBeInTheDocument();
  });

  it("reports null when the clear button is clicked", () => {
    const onChange = vi.fn();
    render(<TextInput value="hello" onChange={onChange} />);

    fireEvent.click(screen.getByRole("button"));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(null);
  });

  it("disables the input when disabled", () => {
    render(<TextInput value={null} onChange={() => {}} disabled />);

    expect(screen.getByRole("textbox")).toBeDisabled();
  });

  it("forwards remaining input props such as placeholder", () => {
    render(<TextInput value={null} onChange={() => {}} placeholder="Type here…" />);

    expect(screen.getByPlaceholderText("Type here…")).toBeInTheDocument();
  });
});
