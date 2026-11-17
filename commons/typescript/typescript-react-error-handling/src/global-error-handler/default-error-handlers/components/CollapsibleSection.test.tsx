/**
 * Imports
 */

import { fireEvent, render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { CollapsibleSection } from "./CollapsibleSection";

/**
 * Tests
 */

describe("CollapsibleSection", () => {
  it("keeps the content collapsed until the heading is clicked", () => {
    render(
      <CollapsibleSection heading="Stack Trace">
        <span>at java.base/java.lang.Thread.run</span>
      </CollapsibleSection>,
    );
    expect(screen.queryByText(/Thread.run/)).not.toBeInTheDocument();

    fireEvent.click(screen.getByText("Stack Trace"));
    expect(screen.getByText(/Thread.run/)).toBeInTheDocument();
  });

  it("shows the content right away when initially open", () => {
    render(
      <CollapsibleSection heading="Stack Trace" initiallyOpen={true}>
        <span>at java.base/java.lang.Thread.run</span>
      </CollapsibleSection>,
    );
    expect(screen.getByText(/Thread.run/)).toBeInTheDocument();
  });

  it("spans the full width and sets no width cap of its own", () => {
    const { container } = render(
      <CollapsibleSection heading="Request-Details" initiallyOpen={true}>
        <span>GET /api/pv/wallbox-types/views/table-entries</span>
      </CollapsibleSection>,
    );

    const section = container.firstElementChild;
    expect(section).toHaveClass("w-full");
    expect(section?.className).not.toMatch(/max-w-/);
  });
});
