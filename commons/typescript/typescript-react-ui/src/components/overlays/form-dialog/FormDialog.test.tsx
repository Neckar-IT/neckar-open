/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { z } from "zod";
import { FormField } from "../../forms/form";
import { FormDialog } from "./FormDialog";

/**
 * Tests
 *
 * A call site may keep the dialog mounted and only toggle `open`, so every way of closing it has to
 * discard the edited values.
 */

const formSchema = z.object({ name: z.string() });

function renderDialog(onOpenChange = vi.fn()): void {
  render(
    <FormDialog
      formSchema={formSchema}
      defaultValues={{ name: "EG" }}
      textKeys={{ title: "Rename" }}
      onOpenChange={onOpenChange}
      onSubmit={vi.fn()}
    >
      <FormField name="name" render={({ field }) => <input aria-label="Name" {...field} />} />
    </FormDialog>,
  );
}

async function editName(): Promise<void> {
  const input = screen.getByLabelText("Name");
  await userEvent.clear(input);
  await userEvent.type(input, "OG");
  expect(input).toHaveValue("OG");
}

describe("FormDialog", () => {
  it("resets the form to the default values when the cancel button is clicked", async () => {
    renderDialog();
    await editName();

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(screen.getByLabelText("Name")).toHaveValue("EG");
  });

  it("resets the form to the default values when Escape is pressed", async () => {
    renderDialog();
    await editName();

    await userEvent.keyboard("{Escape}");

    expect(screen.getByLabelText("Name")).toHaveValue("EG");
  });
});
