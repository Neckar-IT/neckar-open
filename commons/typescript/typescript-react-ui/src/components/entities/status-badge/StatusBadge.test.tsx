import { render, screen } from "@test/test-utils";
import { ArchiveIcon } from "lucide-react";
import { describe, expect, it } from "vitest";
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
});
