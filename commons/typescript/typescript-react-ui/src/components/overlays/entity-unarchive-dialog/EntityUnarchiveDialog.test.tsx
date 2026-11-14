/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterAll, beforeEach, describe, expect, it, vi } from "vitest";
import { EntityUnarchiveDialog } from "./EntityUnarchiveDialog";

const toastSuccessMock = vi.fn();
const withActionFailedToastMock = vi.fn((_actionLabel: string, mutation: Promise<unknown>) => mutation);

vi.mock("../../feedback", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  toast: { success: (...args: unknown[]) => toastSuccessMock(...args) },
  withActionFailedToast: (actionLabel: string, mutation: Promise<unknown>) => withActionFailedToastMock(actionLabel, mutation),
}));

// The mocked module stays in the module cache of a worker that does not isolate test files.
afterAll(() => {
  vi.doUnmock("../../feedback");
  vi.resetModules();
});

/**
 * Tests
 *
 * EntityUnarchiveDialog heads the confirmation with the translated title and the entity, runs `unarchive` on confirm and
 * closes, toasts and reports only after `unarchive` resolved.
 */

const translations = {
  en: {
    translation: {
      authors: {
        unarchive: { title: "Reactivate:", description: "The author becomes active again.", confirm: "Reactivate author" },
        messages: { unarchived: "Author reactivated" },
      },
    },
  },
};

const unarchive = vi.fn<() => Promise<unknown>>();
const onOpenChange = vi.fn<(open: boolean) => void>();
const onUnarchived = vi.fn<() => void>();

function renderEntityUnarchiveDialog(): void {
  render(
    <I18nProvider languageResources={translations}>
      <EntityUnarchiveDialog
        title="authors.unarchive.title"
        display={<span>Jane Austen</span>}
        description="authors.unarchive.description"
        confirmLabel="authors.unarchive.confirm"
        unarchivedMessage="authors.messages.unarchived"
        unarchive={unarchive}
        onOpenChange={onOpenChange}
        onUnarchived={onUnarchived}
      />
    </I18nProvider>,
  );
}

describe("EntityUnarchiveDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the translated title with the entity, the description and the confirm label", () => {
    renderEntityUnarchiveDialog();

    const heading = screen.getByRole("heading");
    expect(heading).toHaveTextContent("Reactivate:");
    expect(heading).toHaveTextContent("Jane Austen");
    expect(screen.getByRole("alertdialog")).toHaveTextContent("The author becomes active again.");
    expect(screen.getByRole("button", { name: "Reactivate author" })).toBeInTheDocument();
  });

  it("unarchives on confirm, then closes, shows the success toast and reports the unarchive", async () => {
    unarchive.mockResolvedValue(undefined);
    renderEntityUnarchiveDialog();

    await userEvent.click(screen.getByRole("button", { name: "Reactivate author" }));

    await waitFor(() => expect(onUnarchived).toHaveBeenCalledOnce());
    expect(unarchive).toHaveBeenCalledOnce();
    expect(withActionFailedToastMock).toHaveBeenCalledWith("Reactivate author", expect.any(Promise));
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(toastSuccessMock).toHaveBeenCalledWith("Author reactivated");
  });

  it("stays open and reports nothing while unarchive is pending", async () => {
    unarchive.mockReturnValue(new Promise(() => {}));
    renderEntityUnarchiveDialog();

    await userEvent.click(screen.getByRole("button", { name: "Reactivate author" }));

    await waitFor(() => expect(unarchive).toHaveBeenCalledOnce());
    expect(onOpenChange).not.toHaveBeenCalledWith(false);
    expect(onUnarchived).not.toHaveBeenCalled();
    expect(toastSuccessMock).not.toHaveBeenCalled();
  });

  it("closes on cancel without unarchiving", async () => {
    renderEntityUnarchiveDialog();

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(unarchive).not.toHaveBeenCalled();
  });
});
