/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ActionLockProvider } from "../../actions/ActionLockProvider";
import { SaveDialog } from "./SaveDialog";

/**
 * Tests
 *
 * The title is a mandatory prop, so no dialog can fall back to a generic heading. The description is
 * optional; when it is left out, the dialog must not keep a dangling aria-describedby reference.
 * The test i18n environment resolves to English (`en.json`).
 */

describe("SaveDialog", () => {
  it("locks the submit button under an action lock", () => {
    render(
      <ActionLockProvider reason="Sie sind offline.">
        <SaveDialog textKeys={{ title: "Rechnung stornieren" }} onOpenChange={vi.fn()} onSubmit={vi.fn()}>
          <p>Body</p>
        </SaveDialog>
      </ActionLockProvider>,
    );

    expect(screen.getByRole("button", { name: "Submit" })).toBeDisabled();
  });

  it("heads the dialog with the given title key", () => {
    render(
      <SaveDialog textKeys={{ title: "Rechnung stornieren" }} onOpenChange={vi.fn()} onSubmit={vi.fn()}>
        <p>Body</p>
      </SaveDialog>,
    );

    expect(screen.getByRole("heading", { name: "Rechnung stornieren" })).toBeInTheDocument();
  });

  it("renders the description below the heading when a description key is given", () => {
    render(
      <SaveDialog
        textKeys={{ title: "Rechnung stornieren", description: "destructiveConfirmationDialog.description" }}
        onOpenChange={vi.fn()}
        onSubmit={vi.fn()}
      >
        <p>Body</p>
      </SaveDialog>,
    );

    expect(screen.getByRole("dialog")).toHaveAccessibleDescription("This action cannot be undone.");
  });

  it("leaves the dialog without an accessible description when no description key is given", () => {
    render(
      <SaveDialog textKeys={{ title: "Rechnung stornieren" }} onOpenChange={vi.fn()} onSubmit={vi.fn()}>
        <p>Body</p>
      </SaveDialog>,
    );

    expect(screen.getByRole("dialog")).not.toHaveAccessibleDescription();
  });

  it("labels the buttons with the shared cancel and submit defaults", () => {
    render(
      <SaveDialog textKeys={{ title: "Rechnung stornieren" }} onOpenChange={vi.fn()} onSubmit={vi.fn()}>
        <p>Body</p>
      </SaveDialog>,
    );

    expect(screen.getByRole("button", { name: "Cancel" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Submit" })).toBeInTheDocument();
  });

  it("calls onClose once and onCancel once when the cancel button is clicked", async () => {
    const onClose = vi.fn();
    const onCancel = vi.fn();
    render(
      <SaveDialog
        textKeys={{ title: "Rechnung stornieren" }}
        onOpenChange={vi.fn()}
        onSubmit={vi.fn()}
        onClose={onClose}
        onCancel={onCancel}
      >
        <p>Body</p>
      </SaveDialog>,
    );

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("calls onClose but not onCancel when Escape is pressed", async () => {
    const onClose = vi.fn();
    const onCancel = vi.fn();
    render(
      <SaveDialog
        textKeys={{ title: "Rechnung stornieren" }}
        onOpenChange={vi.fn()}
        onSubmit={vi.fn()}
        onClose={onClose}
        onCancel={onCancel}
      >
        <p>Body</p>
      </SaveDialog>,
    );

    await userEvent.keyboard("{Escape}");

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onCancel).not.toHaveBeenCalled();
  });
});
