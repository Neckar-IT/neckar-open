/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { EntitySelectDialog } from "./EntitySelectDialog";

/**
 * Tests
 *
 * EntitySelectDialog heads the dialog with the translated title, renders the select table as body and closes on the
 * cancel button and on Escape.
 */

const translations = { en: { translation: { authors: { selectDialog: { title: "Select author" } } } } };

function renderEntitySelectDialog(onOpenChange: (open: boolean) => void): void {
  render(
    <I18nProvider languageResources={translations}>
      <EntitySelectDialog title="authors.selectDialog.title" onOpenChange={onOpenChange}>
        <p>Author table</p>
      </EntitySelectDialog>
    </I18nProvider>,
  );
}

describe("EntitySelectDialog", () => {
  it("heads the dialog with the translated title and renders the children in its body", () => {
    renderEntitySelectDialog(vi.fn());

    const dialog = screen.getByRole("dialog");
    expect(screen.getByRole("heading", { name: "Select author" })).toBeInTheDocument();
    expect(dialog).toHaveTextContent("Author table");
  });

  it("closes when the cancel button is clicked", async () => {
    const onOpenChange = vi.fn();
    renderEntitySelectDialog(onOpenChange);

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("closes on Escape", async () => {
    const onOpenChange = vi.fn();
    renderEntitySelectDialog(onOpenChange);

    await userEvent.keyboard("{Escape}");

    expect(onOpenChange).toHaveBeenCalledWith(false);
  });
});
