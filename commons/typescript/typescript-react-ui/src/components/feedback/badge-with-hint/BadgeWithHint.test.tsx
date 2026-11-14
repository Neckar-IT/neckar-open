import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { TooltipProvider } from "../../primitives/tooltip";
import { BadgeWithHint } from "./BadgeWithHint";

describe("BadgeWithHint", () => {
  it("explains itself on hover", async () => {
    render(
      <TooltipProvider>
        <BadgeWithHint hint="The run has no finished event.">Hanging</BadgeWithHint>
      </TooltipProvider>,
    );

    await userEvent.hover(screen.getByText("Hanging"));

    expect(await screen.findByRole("tooltip")).toHaveTextContent("The run has no finished event.");
    expect(screen.getByText("Hanging")).toHaveAttribute("data-slot", "badge");
  });

  it("opens the hint on keyboard focus of a badge without link", async () => {
    render(
      <TooltipProvider>
        <BadgeWithHint hint="Explained">Plain</BadgeWithHint>
      </TooltipProvider>,
    );

    await userEvent.tab();

    expect(await screen.findByRole("tooltip")).toHaveTextContent("Explained");
  });

  it("links the address and keeps the hint on the link", async () => {
    render(
      <TooltipProvider>
        <BadgeWithHint hint="Explained" href="https://git.example/job/1">
          Linked
        </BadgeWithHint>
      </TooltipProvider>,
    );

    expect(screen.getByRole("link")).toHaveAttribute("href", "https://git.example/job/1");
    await userEvent.tab();
    expect(await screen.findByRole("tooltip")).toHaveTextContent("Explained");
  });
});
