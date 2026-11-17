/**
 * Imports
 */

import { fireEvent, render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import { InputWrapper } from "./InputWrapper";

/**
 * Tests
 */

describe("InputWrapper", () => {
  it("renders its children", () => {
    render(
      <InputWrapper onClear={() => {}}>
        <input aria-label="wrapped" />
      </InputWrapper>,
    );

    expect(screen.getByLabelText("wrapped")).toBeInTheDocument();
  });

  it("renders the clear button by default", () => {
    render(
      <InputWrapper onClear={() => {}}>
        <input aria-label="wrapped" />
      </InputWrapper>,
    );

    expect(screen.getByRole("button")).toBeInTheDocument();
  });

  it("hides the clear button when hideClearButton is set", () => {
    render(
      <InputWrapper onClear={() => {}} hideClearButton>
        <input aria-label="wrapped" />
      </InputWrapper>,
    );

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("invokes onClear when the clear button is clicked", () => {
    const onClear = vi.fn();
    render(
      <InputWrapper onClear={onClear}>
        <input aria-label="wrapped" />
      </InputWrapper>,
    );

    fireEvent.click(screen.getByRole("button"));

    expect(onClear).toHaveBeenCalledTimes(1);
  });

  it("disables the clear button when disabled", () => {
    render(
      <InputWrapper onClear={() => {}} disabled>
        <input aria-label="wrapped" />
      </InputWrapper>,
    );

    expect(screen.getByRole("button")).toBeDisabled();
  });

  it("renders a left add-on", () => {
    render(
      <InputWrapper onClear={() => {}} addOnLeft={<span>https://</span>}>
        <input aria-label="wrapped" />
      </InputWrapper>,
    );

    expect(screen.getByText("https://")).toBeInTheDocument();
  });

  it("replaces the clear button with a right add-on when one is provided", () => {
    render(
      <InputWrapper onClear={() => {}} addOnRight={<span>.com</span>}>
        <input aria-label="wrapped" />
      </InputWrapper>,
    );

    expect(screen.getByText(".com")).toBeInTheDocument();
    // The right add-on takes the slot the clear button would otherwise occupy.
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("does not call onClear from the right add-on slot", () => {
    const onClear = vi.fn();
    render(
      <InputWrapper onClear={onClear} addOnRight={<span>.com</span>}>
        <input aria-label="wrapped" />
      </InputWrapper>,
    );

    expect(onClear).not.toHaveBeenCalled();
  });

  it("applies the invalid styling to the wrapper", () => {
    const { container } = render(
      <InputWrapper onClear={() => {}} isInvalid>
        <input aria-label="wrapped" />
      </InputWrapper>,
    );

    expect(container.querySelector(".border-destructive")).toBeInTheDocument();
  });
});
