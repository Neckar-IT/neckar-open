import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { PhoneNumber } from "./PhoneNumber";

describe("PhoneNumber", () => {
  it("should render land line phone numbers correctly", () => {
    render(<PhoneNumber value="074739594960" />);
    expect(screen.getByText("07473 9594960"));
  });

  it("should render mobile phone numbers correctly", () => {
    render(<PhoneNumber value="017612345678" />);
    expect(screen.getByText("0176 12345678"));
  });
});
