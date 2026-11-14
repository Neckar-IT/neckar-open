import { render } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { FormattedDateTime } from "./FormattedDateTime";

describe("FormattedDateTime", () => {
  it("formats a wire-shaped Instant string", () => {
    const { container } = render(<FormattedDateTime date="2026-04-01T08:30:00Z" />);
    const text = container.textContent ?? "";
    expect(text).toMatch(/2026/);
    expect(text).toMatch(/04|4\b|Apr/);
  });

  it("formats a Date instance", () => {
    const { container } = render(<FormattedDateTime date={new Date(Date.UTC(2026, 3, 1, 8, 30))} />);
    expect(container.textContent ?? "").toMatch(/2026/);
  });

  it("renders nothing when date is null or undefined", () => {
    const nullResult = render(<FormattedDateTime date={null} />);
    expect(nullResult.container.textContent).toBe("");

    const undefinedResult = render(<FormattedDateTime date={undefined} />);
    expect(undefinedResult.container.textContent).toBe("");
  });
});
