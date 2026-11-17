import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { EMail } from "./EMail";

describe("EMail", () => {
  it("should render the email output correctly", () => {
    render(<EMail value="test@email.com" />);

    const emailElement = screen.getByText("test@email.com");

    expect(emailElement).toBeInTheDocument();
    expect(emailElement).toHaveAttribute("href", "mailto:test@email.com");
  });
});
