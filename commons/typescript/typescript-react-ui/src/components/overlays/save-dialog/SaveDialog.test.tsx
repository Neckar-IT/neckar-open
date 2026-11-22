/**
 * Imports
 */

import { act, render, screen, tooltipTriggerOf } from "@test/test-utils";
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
  it("locks the submit button under an action lock and names the reason", async () => {
    render(
      <ActionLockProvider reason="Sie sind offline.">
        <SaveDialog textKeys={{ title: "Rechnung stornieren" }} onOpenChange={vi.fn()} onSubmit={vi.fn()}>
          <p>Body</p>
        </SaveDialog>
      </ActionLockProvider>,
    );
    const submit = screen.getByRole("button", { name: "Submit" });

    // Focus skips the hover delay, which the modal dialog in jsdom can stretch past the timeout of findByRole.
    act(() => tooltipTriggerOf(submit).focus());

    expect(submit).toBeDisabled();
    expect(await screen.findByRole("tooltip")).toHaveTextContent("Sie sind offline.");
  });

  it("submits on Enter", async () => {
    const onSubmit = vi.fn();
    render(
      <SaveDialog textKeys={{ title: "Rechnung stornieren" }} onOpenChange={vi.fn()} onSubmit={onSubmit}>
        <p>Body</p>
      </SaveDialog>,
    );

    await userEvent.keyboard("{Enter}");

    expect(onSubmit).toHaveBeenCalledOnce();
  });

  it("ignores Enter under an action lock", async () => {
    const onSubmit = vi.fn();
    render(
      <ActionLockProvider reason="Sie sind offline.">
        <SaveDialog textKeys={{ title: "Rechnung stornieren" }} onOpenChange={vi.fn()} onSubmit={onSubmit}>
          <p>Body</p>
        </SaveDialog>
      </ActionLockProvider>,
    );

    await userEvent.keyboard("{Enter}");

    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("disables the submit button and ignores Enter while the caller disables the submission", async () => {
    const onSubmit = vi.fn();
    render(
      <SaveDialog textKeys={{ title: "Rechnung stornieren" }} onOpenChange={vi.fn()} onSubmit={onSubmit} disabledSubmission={true}>
        <p>Body</p>
      </SaveDialog>,
    );

    await userEvent.keyboard("{Enter}");

    expect(screen.getByRole("button", { name: "Submit" })).toBeDisabled();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("ignores Enter while the submission is pending", async () => {
    const onSubmit = vi.fn();
    render(
      <SaveDialog textKeys={{ title: "Rechnung stornieren" }} onOpenChange={vi.fn()} onSubmit={onSubmit} isPending={true}>
        <p>Body</p>
      </SaveDialog>,
    );

    await userEvent.keyboard("{Enter}");

    expect(onSubmit).not.toHaveBeenCalled();
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
