import { act, render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { Popover, PopoverContent, PopoverTrigger } from "./Popover";

function ControlledPopover(
  props: Pick<React.ComponentProps<typeof PopoverContent>, "preventCloseOnClickOutside" | "preventCloseOnFocusOutside">,
) {
  const [open, setOpen] = useState(true);
  return (
    <>
      <button type="button">Outside</button>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger>Trigger</PopoverTrigger>
        <PopoverContent {...props}>Content</PopoverContent>
      </Popover>
    </>
  );
}

describe("PopoverContent", () => {
  it("closes on a click outside by default", async () => {
    render(<ControlledPopover />);

    await userEvent.click(screen.getByRole("button", { name: "Outside" }));

    expect(screen.queryByText("Content")).not.toBeInTheDocument();
  });

  it("stays open on a click outside with preventCloseOnClickOutside", async () => {
    render(<ControlledPopover preventCloseOnClickOutside={true} />);

    await userEvent.click(screen.getByRole("button", { name: "Outside" }));

    expect(screen.getByText("Content")).toBeInTheDocument();
  });

  it("still calls the caller's onPointerDownOutside", async () => {
    const onPointerDownOutside = vi.fn();
    render(
      <>
        <button type="button">Outside</button>
        <Popover open={true}>
          <PopoverTrigger>Trigger</PopoverTrigger>
          <PopoverContent preventCloseOnClickOutside={true} onPointerDownOutside={onPointerDownOutside}>
            Content
          </PopoverContent>
        </Popover>
      </>,
    );

    await userEvent.click(screen.getByRole("button", { name: "Outside" }));

    expect(onPointerDownOutside).toHaveBeenCalledOnce();
  });

  it("closes when the focus moves outside by default", () => {
    render(<ControlledPopover />);

    act(() => screen.getByRole("button", { name: "Outside" }).focus());

    expect(screen.queryByText("Content")).not.toBeInTheDocument();
  });

  it("stays open when the focus moves outside with preventCloseOnFocusOutside", () => {
    render(<ControlledPopover preventCloseOnFocusOutside={true} />);

    act(() => screen.getByRole("button", { name: "Outside" }).focus());

    expect(screen.getByText("Content")).toBeInTheDocument();
  });
});
