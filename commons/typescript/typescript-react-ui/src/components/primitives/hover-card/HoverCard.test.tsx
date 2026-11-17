/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { HoverCard, HoverCardContent, HoverCardTrigger } from "./HoverCard";

/**
 * Tests
 */

describe("HoverCardContent", () => {
  it("renders its content in a portal, outside the trigger container", () => {
    const { container } = render(
      <HoverCard open>
        <HoverCardTrigger>Trigger</HoverCardTrigger>
        <HoverCardContent>
          <span data-testid="hover-content">Validation details</span>
        </HoverCardContent>
      </HoverCard>,
    );

    const content = screen.getByTestId("hover-content");

    // The content is rendered into the document...
    expect(content).toBeInTheDocument();
    // ...but escapes the render container via a portal, so ancestor overflow/positioning
    // contexts cannot clip or mis-position it (the cause of #2006).
    expect(container).not.toContainElement(content);
  });
});
