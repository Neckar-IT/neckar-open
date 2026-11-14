import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { AddConditionsLabel } from "./AddConditionsLabel";

describe("AddConditionsLabel", () => {
  it("renders the localized add-now call to action", () => {
    render(<AddConditionsLabel />);

    expect(screen.getByText(/add now/i)).toBeInTheDocument();
  });

  it("merges a custom className", () => {
    render(<AddConditionsLabel className="custom-class" />);

    expect(screen.getByText(/add now/i)).toHaveClass("custom-class");
  });
});
