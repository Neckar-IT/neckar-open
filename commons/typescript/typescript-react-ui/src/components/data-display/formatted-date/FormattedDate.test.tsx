import { render } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { FormattedDate } from "./FormattedDate";

describe("FormattedDate", () => {
  it("formats a wire-shaped LocalDate string", () => {
    const { container } = render(<FormattedDate date="2026-04-01" />);
    const text = container.textContent ?? "";
    expect(text).toMatch(/2026/);
    expect(text).toMatch(/04|4\b|Apr/);
  });

  it("formats a Date instance", () => {
    const { container } = render(<FormattedDate date={new Date(Date.UTC(2026, 3, 1))} />);
    expect(container.textContent ?? "").toMatch(/2026/);
  });

  it("renders nothing when date is null or undefined", () => {
    const nullResult = render(<FormattedDate date={null} />);
    expect(nullResult.container.textContent).toBe("");

    const undefinedResult = render(<FormattedDate date={undefined} />);
    expect(undefinedResult.container.textContent).toBe("");
  });
});
