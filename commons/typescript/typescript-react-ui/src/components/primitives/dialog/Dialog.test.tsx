import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { Dialog, DialogBody, DialogContent, DialogDescription, DialogTitle } from "./Dialog";

function OpenDialog({ size }: { size?: "sm" | "md" | "lg" | "xl" | "full" }) {
  const [open, setOpen] = useState(true);
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent size={size}>
        <DialogTitle>Title</DialogTitle>
        <DialogDescription>Description</DialogDescription>
        <DialogBody>Body</DialogBody>
      </DialogContent>
    </Dialog>
  );
}

describe("DialogContent", () => {
  it("is anchored at the top with the sm width by default", () => {
    render(<OpenDialog />);

    const dialog = screen.getByRole("dialog");
    expect(dialog).toHaveClass("top-16", "translate-y-0", "sm:max-w-lg");
    expect(dialog).not.toHaveClass("top-1/2");
    expect(dialog).not.toHaveClass("sm:max-w-sm");
  });

  it.each([
    ["md", "sm:max-w-3xl"],
    ["lg", "sm:max-w-5xl"],
    ["xl", "sm:max-w-7xl"],
  ] as const)("sets the width of size %s", (size, widthClass) => {
    render(<OpenDialog size={size} />);

    expect(screen.getByRole("dialog")).toHaveClass(widthClass);
  });

  it("fills the viewport with size full", () => {
    render(<OpenDialog size="full" />);

    const dialog = screen.getByRole("dialog");
    expect(dialog).toHaveClass("inset-0", "max-h-full", "sm:max-w-full", "rounded-none");
    expect(dialog).not.toHaveClass("top-16");
    expect(dialog).not.toHaveClass("max-h-[calc(100vh-128px)]");
  });

  it("stays open on a click outside", async () => {
    render(<OpenDialog />);

    // The modal dialog sets `pointer-events: none` on the body; the check would reject the click before Radix sees it.
    await userEvent.setup({ pointerEventsCheck: 0 }).click(document.body);

    expect(screen.getByRole("dialog")).toBeInTheDocument();
  });
});
