import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "./Tooltip";

function renderTooltip(tabIndex?: number) {
  render(
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger tabIndex={tabIndex}>Info</TooltipTrigger>
        <TooltipContent>Erklärung</TooltipContent>
      </Tooltip>
    </TooltipProvider>,
  );
  return screen.getByRole("button", { name: "Info" });
}

describe("TooltipTrigger", () => {
  it("stays outside the tab order by default", () => {
    expect(renderTooltip()).toHaveAttribute("tabindex", "-1");
  });

  it("joins the tab order when the caller passes a tabIndex", () => {
    expect(renderTooltip(0)).toHaveAttribute("tabindex", "0");
  });
});
