/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import type { Step } from "./MultiStepControl";
import { MultiStepList } from "./MultiStepList";

/**
 * Tests
 *
 * The list renders one numbered item per step and marks the active and disabled steps via styling.
 */

const steps: Step[] = [
  { title: "Alpha", component: null },
  { title: "Beta", component: null },
  { title: "Gamma", component: null, disabled: true },
];

describe("MultiStepList", () => {
  it("renders a numbered item with the title for each step", () => {
    render(<MultiStepList steps={steps} currentStepIndex={0} />);

    expect(screen.getByText("1")).toBeInTheDocument();
    expect(screen.getByText("Alpha")).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("Beta")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.getByText("Gamma")).toBeInTheDocument();
  });

  it("highlights the title of the active step", () => {
    render(<MultiStepList steps={steps} currentStepIndex={1} />);

    expect(screen.getByText("Beta")).toHaveClass("text-primary", "font-semibold");
    expect(screen.getByText("Alpha")).not.toHaveClass("text-primary");
  });

  it("mutes the title of a disabled step", () => {
    render(<MultiStepList steps={steps} currentStepIndex={0} />);

    expect(screen.getByText("Gamma")).toHaveClass("text-muted-foreground");
  });
});
