/**
 * Imports
 */

import { render } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { detailsValueOf, formattedDateTimeOf } from "./dataDisplayTestSupport";
import { EntityDetailsList } from "./entity-details-list";
import { FormattedDateTime } from "./formatted-date-time";

/**
 * Tests
 */

describe("detailsValueOf", () => {
  it("returns the value of the row the label names", () => {
    render(
      <EntityDetailsList
        rows={[
          { label: "Title", value: "Pride and Prejudice" },
          { label: "Author", value: "Jane Austen" },
        ]}
      />,
    );

    expect(detailsValueOf("Author")).toBe("Jane Austen");
  });

  it("returns null for a label without a value element", () => {
    render(<span>Title</span>);

    expect(detailsValueOf("Title")).toBeNull();
  });
});

describe("formattedDateTimeOf", () => {
  it("returns the text FormattedDateTime renders and unmounts it", () => {
    const text = formattedDateTimeOf("2026-04-01T08:30:00Z");

    expect(document.body.textContent).toBe("");
    const { container } = render(<FormattedDateTime date="2026-04-01T08:30:00Z" />);
    expect(text).toBe(container.textContent);
  });
});
