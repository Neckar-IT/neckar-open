import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { SecondsOutput } from "./seconds";

describe("SecondsOutput", () => {
  it("should render a sub-second duration without converting it", () => {
    render(<SecondsOutput data-testid="seconds-output" value={0.05} />);
    expect(screen.getByTestId("seconds-output")).toHaveTextContent("0.05 s");
  });
});
