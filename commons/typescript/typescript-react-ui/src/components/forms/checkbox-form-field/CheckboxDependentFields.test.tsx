import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { CheckboxDependentFields } from "./CheckboxDependentFields";

describe("CheckboxDependentFields", () => {
  it("indents its children to the checkbox label", () => {
    render(
      <CheckboxDependentFields>
        <span>gated field</span>
      </CheckboxDependentFields>,
    );

    // h-4 checkbox + gap-2 to its label = pl-6
    expect(screen.getByText("gated field").parentElement).toHaveClass("pl-6");
  });

  it("keeps the vertical rhythm of the surrounding form", () => {
    render(
      <CheckboxDependentFields>
        <span>gated field</span>
      </CheckboxDependentFields>,
    );

    expect(screen.getByText("gated field").parentElement).toHaveClass("flex", "flex-col", "gap-4");
  });

  it("merges a custom className", () => {
    render(
      <CheckboxDependentFields className="custom-class">
        <span>gated field</span>
      </CheckboxDependentFields>,
    );

    expect(screen.getByText("gated field").parentElement).toHaveClass("custom-class");
  });
});
