import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./Select";

function renderOpenSelect(position?: "popper" | "item-aligned") {
  render(
    <Select open={true} value="a">
      <SelectTrigger>
        <SelectValue />
      </SelectTrigger>
      <SelectContent position={position}>
        <SelectItem value="a">A</SelectItem>
      </SelectContent>
    </Select>,
  );
  return screen.getByRole("listbox");
}

describe("SelectContent", () => {
  it("opens below the trigger by default", () => {
    expect(renderOpenSelect()).toHaveAttribute("data-align-trigger", "false");
  });

  it("aligns the list over the trigger when the caller asks for item-aligned", () => {
    expect(renderOpenSelect("item-aligned")).toHaveAttribute("data-align-trigger", "true");
  });
});
