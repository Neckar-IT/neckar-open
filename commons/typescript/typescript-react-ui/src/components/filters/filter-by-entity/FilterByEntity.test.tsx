/**
 * Imports
 */

import { render, screen, waitFor } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { FilterByEntity } from "./FilterByEntity";

/**
 * Tests
 *
 * FilterByEntity in the panel is a field-like button that opens the select dialog of the entity.
 */

describe("FilterByEntity", () => {
  it("names the field in the panel by label and value while the value is a React node", () => {
    render(
      <FilterByEntity
        form="Panel"
        label="Author"
        active
        selected={<strong>Jonas Weber</strong>}
        onClear={vi.fn()}
        renderSelectDialog={() => null}
      />,
    );

    expect(screen.getByRole("button", { name: "Author: Jonas Weber" })).toHaveAttribute("aria-haspopup", "dialog");
  });

  it("shows the placeholder in the muted color and no clear button without a selection", () => {
    render(
      <FilterByEntity form="Panel" label="Author" active={false} selected={undefined} onClear={vi.fn()} renderSelectDialog={() => null} />,
    );

    expect(screen.getAllByRole("button")).toHaveLength(1);
    expect(screen.getByText("Select")).toHaveClass("text-muted-foreground");
  });

  it("clears the selection through the clear button", async () => {
    const onClear = vi.fn();
    render(<FilterByEntity form="Panel" label="Author" active selected="Jonas Weber" onClear={onClear} renderSelectDialog={() => null} />);

    await userEvent.click(screen.getAllByRole("button")[1]!);

    expect(onClear).toHaveBeenCalledOnce();
  });

  it("returns the focus to the button when the dialog closes", async () => {
    const user = userEvent.setup();
    function Harness(): React.ReactElement {
      const [selected, setSelected] = useState<string | undefined>(undefined);
      return (
        <FilterByEntity
          form="Panel"
          label="Author"
          active={selected !== undefined}
          selected={selected}
          onClear={() => setSelected(undefined)}
          renderSelectDialog={(onOpenChange) => <button onClick={() => onOpenChange(false)}>Close dialog</button>}
        />
      );
    }
    render(<Harness />);

    const trigger = screen.getByRole("button", { name: /Author/ });
    await user.click(trigger);
    await user.click(screen.getByRole("button", { name: "Close dialog" }));

    await waitFor(() => expect(trigger).toHaveFocus());
  });

  it.each([
    [true, "Jonas Weber"],
    [false, undefined],
  ])("marks the panel field as an active filter button while active is %s", (active, selected) => {
    render(
      <FilterByEntity form="Panel" label="Author" active={active} selected={selected} onClear={vi.fn()} renderSelectDialog={() => null} />,
    );

    const field = screen.getByRole("button", { name: /Author/ }).parentElement?.parentElement;
    if (active) {
      expect(field).toHaveClass("bg-primary/10", "border-primary/40", "text-primary");
    } else {
      expect(field).not.toHaveClass("bg-primary/10");
    }
  });
});
