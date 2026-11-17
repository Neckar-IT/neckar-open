import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { ToggleGroup, ToggleGroupItem } from "./ToggleGroup";

function renderGroup(spacing?: number) {
  render(
    <ToggleGroup type="single" aria-label="Formatierung" spacing={spacing}>
      <ToggleGroupItem value="bold">B</ToggleGroupItem>
      <ToggleGroupItem value="italic">I</ToggleGroupItem>
    </ToggleGroup>,
  );
  return screen.getByRole("radiogroup", { name: "Formatierung" });
}

describe("ToggleGroup", () => {
  it("joins its items without spacing by default", () => {
    expect(renderGroup()).toHaveAttribute("data-spacing", "0");
  });

  it("spaces the items apart when the caller sets spacing", () => {
    expect(renderGroup(2)).toHaveAttribute("data-spacing", "2");
  });
});
