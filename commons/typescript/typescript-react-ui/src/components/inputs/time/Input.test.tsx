/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { TimeInput } from "./Input";

/**
 * Test Component
 */

function TestComponent(props: { includeSeconds?: boolean }) {
  const includeSeconds = props.includeSeconds ?? false;

  const [value, setValue] = useState<string | null>("12:34:56");
  const [submittedValue, setSubmittedValue] = useState<string | null>("");

  return (
    <>
      <TimeInput data-testid="input" includeSeconds={includeSeconds} value={value} onChange={setValue} />

      <button
        onClick={() => {
          setSubmittedValue(value);
        }}
      >
        Submit
      </button>

      <pre data-testid="submitted-value">{submittedValue}</pre>
    </>
  );
}

/**
 * Tests
 */

describe("TimeInput with seconds", () => {
  it("should render the time input withthe current value excluding the seconds", () => {
    render(<TestComponent />);

    expect(screen.getByTestId("input")).toHaveValue("12:34");
  });

  it("should modify the value on user input", async () => {
    const user = userEvent.setup();

    render(<TestComponent />);

    const inputElement = screen.getByTestId("input");
    const buttonElement = screen.getByText(/submit/i);

    expect(screen.getByTestId("submitted-value")).toHaveTextContent("");

    await user.type(inputElement, "10:00", {
      initialSelectionStart: 0,
      initialSelectionEnd: 5,
    });
    await user.click(buttonElement);

    expect(screen.getByTestId("submitted-value")).toHaveTextContent("10:00:00");
  });
});

describe("TimeInput with seconds", () => {
  it("should render the time input with the current value excluding the seconds", () => {
    render(<TestComponent includeSeconds />);

    expect(screen.getByTestId("input")).toHaveValue("12:34:56");
  });

  it("should modify the value on user input", async () => {
    const user = userEvent.setup();

    render(<TestComponent />);

    const inputElement = screen.getByTestId("input");
    const buttonElement = screen.getByText(/submit/i);

    expect(screen.getByTestId("submitted-value")).toHaveTextContent("");

    await user.type(inputElement, "10:00:00", {
      initialSelectionStart: 0,
      initialSelectionEnd: 8,
    });
    await user.click(buttonElement);

    expect(screen.getByTestId("submitted-value")).toHaveTextContent("10:00:00");
  });
});
