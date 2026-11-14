/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import { Delayed } from "./Delayed";

/**
 * Tests
 */

afterEach(() => {
  window.testMode = undefined;
});

describe("Delayed", () => {
  it("should render the children after the delay", async () => {
    render(<Delayed delay={500}>Content</Delayed>);

    expect(screen.queryByText("Content")).not.toBeInTheDocument();
    // findByText polls past the component's own 500 ms timer and the re-render that follows it —
    // a second timer of the same length would race them.
    expect(await screen.findByText("Content")).toBeInTheDocument();
  });

  it("should render the children without the delay in test mode", () => {
    window.testMode = true;

    render(<Delayed delay={500}>Content</Delayed>);

    expect(screen.getByText("Content")).toBeInTheDocument();
  });
});
