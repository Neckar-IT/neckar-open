/**
 * Imports
 */

import { render, screen, waitFor } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ActionLockProvider } from "../../actions/ActionLockProvider";
import { toast } from "../../feedback";
import { ConfirmationDialog } from "./ConfirmationDialog";
import { DestructiveConfirmationDialog } from "./DestructiveConfirmationDialog";

const toastErrorMock = vi.fn();
beforeEach(() => {
  toastErrorMock.mockReset();
  vi.spyOn(toast, "error").mockImplementation(toastErrorMock);
});

/**
 * Tests
 *
 * title and description resolve in the order: direct prop → textKeys entry → embedded default.
 * The test i18n environment resolves to English (`en.json`).
 */

describe("ConfirmationDialog", () => {
  it("renders the direct title and description props verbatim, ignoring textKeys", () => {
    render(
      <ConfirmationDialog
        title="Kunde löschen?"
        description="Der Kunde wird entfernt."
        textKeys={{ title: "multiStep.finish", description: "destructiveConfirmationDialog.description" }}
        onConfirm={vi.fn()}
        onOpenChange={vi.fn()}
      />,
    );

    expect(screen.getByText("Kunde löschen?")).toBeInTheDocument();
    expect(screen.getByText("Der Kunde wird entfernt.")).toBeInTheDocument();
    expect(screen.queryByText("Save")).not.toBeInTheDocument();
  });

  it("renders children outside the description", () => {
    render(
      <ConfirmationDialog title="Delete?" description="Short text." onConfirm={vi.fn()} onOpenChange={vi.fn()}>
        <ul>
          <li>3 books</li>
        </ul>
      </ConfirmationDialog>,
    );

    expect(screen.getByRole("alertdialog")).toHaveAccessibleDescription("Short text.");
    expect(screen.getByText("3 books").closest("[data-slot='alert-dialog-description']")).toBeNull();
  });

  it("falls back to the textKeys title and description when no direct props are given", () => {
    render(
      <ConfirmationDialog
        textKeys={{ title: "multiStep.finish", description: "destructiveConfirmationDialog.description" }}
        onConfirm={vi.fn()}
        onOpenChange={vi.fn()}
      />,
    );

    expect(screen.getByText("Save")).toBeInTheDocument();
    expect(screen.getByText("This action cannot be undone.")).toBeInTheDocument();
  });

  it("falls back to the confirmationDialog.title default and renders no description when nothing is provided", () => {
    render(<ConfirmationDialog onConfirm={vi.fn()} onOpenChange={vi.fn()} />);

    expect(screen.getByText("Confirm action")).toBeInTheDocument();
    expect(screen.getByRole("alertdialog")).not.toHaveAccessibleDescription();
  });

  it("renders the neutral confirm label and the default cancel label", () => {
    render(<ConfirmationDialog title="Confirm action" onConfirm={vi.fn()} onOpenChange={vi.fn()} />);

    expect(screen.getByRole("button", { name: "Confirm" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Cancel" })).toBeInTheDocument();
  });

  it("locks the confirm button inside an ActionLockProvider", () => {
    const onConfirm = vi.fn();
    render(
      <ActionLockProvider reason="Offline">
        <ConfirmationDialog title="Archivieren?" onConfirm={onConfirm} onOpenChange={vi.fn()} />
      </ActionLockProvider>,
    );
    const confirm = screen.getByRole("button", { name: "Confirm" });

    confirm.click();

    expect(confirm).toBeDisabled();
    expect(onConfirm).not.toHaveBeenCalled();
  });
});

describe("DestructiveConfirmationDialog", () => {
  it("renders the destructive confirm label and the default destructive description", () => {
    render(<DestructiveConfirmationDialog title="Eintrag löschen?" onConfirm={vi.fn()} onOpenChange={vi.fn()} />);

    expect(screen.getByText("This action cannot be undone.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Delete" })).toBeInTheDocument();
  });

  it("lets a direct description prop override the default destructive description", () => {
    render(
      <DestructiveConfirmationDialog
        title="Eintrag löschen?"
        description="Alle Verknüpfungen gehen verloren."
        onConfirm={vi.fn()}
        onOpenChange={vi.fn()}
      />,
    );

    expect(screen.getByText("Alle Verknüpfungen gehen verloren.")).toBeInTheDocument();
    expect(screen.queryByText("This action cannot be undone.")).not.toBeInTheDocument();
  });

  it("shows the failure toast named after the confirm label when onConfirm rejects", async () => {
    render(
      <DestructiveConfirmationDialog
        title="Ausgabe löschen?"
        textKeys={{ confirm: "Ausgabe löschen" }}
        onConfirm={() => Promise.reject(new Error("delete failed"))}
        onOpenChange={vi.fn()}
      />,
    );

    await userEvent.click(screen.getByRole("button", { name: "Ausgabe löschen" }));

    await waitFor(() => expect(toastErrorMock).toHaveBeenCalledOnce());
    render(toastErrorMock.mock.calls[0]?.[0] as ReactElement);
    expect(screen.getByText("Ausgabe löschen failed.")).toBeInTheDocument();
  });
});
