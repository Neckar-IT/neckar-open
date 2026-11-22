import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { ArchiveIcon } from "lucide-react";
import { describe, expect, it } from "vitest";
import { TooltipProvider } from "../../primitives/tooltip";
import { StatusBadge, type StatusBadgeTone } from "./StatusBadge";

const toneClasses: Record<StatusBadgeTone, string> = {
  muted: "text-muted-foreground",
  info: "text-primary",
  success: "text-state-success",
  warning: "text-state-warning",
  danger: "text-state-error",
};

describe("StatusBadge", () => {
  it.each(Object.entries(toneClasses))("colours the %s tone with %s", (tone, toneClass) => {
    render(<StatusBadge tone={tone as StatusBadgeTone} icon={<ArchiveIcon />} label="Archived" data-testid="badge" />);

    expect(screen.getByTestId("badge")).toHaveClass(toneClass);
  });

  it("shows the label as tooltip and no text in the icon variant", () => {
    render(<StatusBadge tone="muted" icon={<ArchiveIcon />} label="Archived" data-testid="badge" />);

    expect(screen.getByTestId("badge")).toHaveAttribute("title", "Archived");
    expect(screen.queryByText("Archived")).not.toBeInTheDocument();
  });

  it("shows the label as text in the with-text variant", () => {
    render(<StatusBadge tone="muted" icon={<ArchiveIcon />} label="Archived" variant="with-text" />);

    expect(screen.getByText("Archived")).toBeInTheDocument();
  });

  it("swaps the icon for a spinner while loading", () => {
    render(<StatusBadge tone="info" icon={<ArchiveIcon data-testid="icon" />} label="Published" loading data-testid="badge" />);

    expect(screen.queryByTestId("icon")).not.toBeInTheDocument();
    expect(screen.getByTestId("badge")).toHaveClass("opacity-60");
    expect(screen.getByTestId("badge").querySelector(".animate-spin")).not.toBeNull();
  });

  it("lets the className override the tone colour", () => {
    render(<StatusBadge tone="muted" icon={<ArchiveIcon />} label="Draft" className="text-draft" data-testid="badge" />);

    expect(screen.getByTestId("badge")).toHaveClass("text-draft");
    expect(screen.getByTestId("badge")).not.toHaveClass("text-muted-foreground");
  });

  it("shows the hint as tooltip on keyboard focus", async () => {
    render(
      <TooltipProvider>
        <StatusBadge tone="warning" icon={<ArchiveIcon />} label="Hanging" hint="The run has no finished event." data-testid="badge" />
      </TooltipProvider>,
    );

    await userEvent.tab();

    expect(await screen.findByRole("tooltip")).toHaveTextContent("The run has no finished event.");
    expect(screen.getByTestId("badge")).not.toHaveAttribute("title");
    expect(screen.getByTestId("badge")).toHaveAttribute("data-slot", "badge");
  });

  it("links the address in a new tab and shows the label as tooltip without hint", async () => {
    render(
      <TooltipProvider>
        <StatusBadge tone="danger" icon={<ArchiveIcon />} label="Failed" href="https://git.example/job/1" />
      </TooltipProvider>,
    );

    const link = screen.getByRole("link", { name: "Failed" });
    expect(link).toHaveAttribute("href", "https://git.example/job/1");
    expect(link).toHaveAttribute("target", "_blank");
    await userEvent.tab();
    expect(await screen.findByRole("tooltip")).toHaveTextContent("Failed");
  });

  it("links nothing without href", () => {
    render(
      <TooltipProvider>
        <StatusBadge tone="muted" icon={<ArchiveIcon />} label="Unrecorded" hint="Explained" />
      </TooltipProvider>,
    );

    expect(screen.queryByRole("link")).not.toBeInTheDocument();
  });
});
