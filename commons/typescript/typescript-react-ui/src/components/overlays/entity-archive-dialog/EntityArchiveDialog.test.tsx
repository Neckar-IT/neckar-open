/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterAll, beforeEach, describe, expect, it, vi } from "vitest";
import { type ArchiveOutcome, EntityArchiveDialog } from "./EntityArchiveDialog";

const toastSuccessMock = vi.fn();
const warnAboutActiveUsageMock = vi.fn();
const withActionFailedToastMock = vi.fn((_actionLabel: string, mutation: Promise<unknown>) => mutation);

vi.mock("../../feedback", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  toast: { success: (...args: unknown[]) => toastSuccessMock(...args) },
  warnAboutActiveUsage: (...args: unknown[]) => warnAboutActiveUsageMock(...args),
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
 * EntityArchiveDialog heads the confirmation with the translated title and the entity, shows the usage below the
 * description, runs `archive` on confirm and closes, toasts, warns about active usage and reports only after `archive`
 * resolved.
 */

const translations = {
  en: {
    translation: {
      authors: {
        archive: { title: "Archive:", description: "The author is hidden from new books.", confirm: "Archive author" },
        messages: { archived: "Author archived" },
      },
    },
  },
};

const archive = vi.fn<() => Promise<ArchiveOutcome>>();
const onOpenChange = vi.fn<(open: boolean) => void>();
const onArchived = vi.fn<() => void>();

function renderEntityArchiveDialog(): void {
  render(
    <I18nProvider languageResources={translations}>
      <EntityArchiveDialog
        title="authors.archive.title"
        display={<span>Jane Austen</span>}
        description="authors.archive.description"
        usage={<div>Used by 3 active books</div>}
        confirmLabel="authors.archive.confirm"
        archivedMessage="authors.messages.archived"
        archive={archive}
        onOpenChange={onOpenChange}
        onArchived={onArchived}
      />
    </I18nProvider>,
  );
}

describe("EntityArchiveDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the translated title with the entity, the description, the usage outside the description and the confirm label", () => {
    renderEntityArchiveDialog();

    const heading = screen.getByRole("heading");
    expect(heading).toHaveTextContent("Archive:");
    expect(heading).toHaveTextContent("Jane Austen");
    expect(screen.getByRole("alertdialog")).toHaveAccessibleDescription("The author is hidden from new books.");
    expect(screen.getByText("Used by 3 active books")).toBeInTheDocument();
    expect(screen.getByText("Used by 3 active books").closest("[data-slot='alert-dialog-description']")).toBeNull();
    expect(screen.getByRole("button", { name: "Archive author" })).toBeInTheDocument();
  });

  it("archives on confirm, then closes, shows the success toast, warns about the active usage and reports the archive", async () => {
    const usedBy = { counts: [{ activeCount: 3 }] };
    archive.mockResolvedValue({ type: "good", usedBy });
    renderEntityArchiveDialog();

    await userEvent.click(screen.getByRole("button", { name: "Archive author" }));

    await waitFor(() => expect(onArchived).toHaveBeenCalledOnce());
    expect(archive).toHaveBeenCalledOnce();
    expect(withActionFailedToastMock).toHaveBeenCalledWith("Archive author", expect.any(Promise));
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(toastSuccessMock).toHaveBeenCalledWith("Author archived");
    expect(warnAboutActiveUsageMock).toHaveBeenCalledWith(usedBy);
  });

  it("closes and reports an answer that is not good without the usage warning", async () => {
    archive.mockResolvedValue({ type: "bad/AlreadyArchived" });
    renderEntityArchiveDialog();

    await userEvent.click(screen.getByRole("button", { name: "Archive author" }));

    await waitFor(() => expect(onArchived).toHaveBeenCalledOnce());
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(toastSuccessMock).toHaveBeenCalledWith("Author archived");
    expect(warnAboutActiveUsageMock).not.toHaveBeenCalled();
  });

  it("stays open and reports nothing while archive is pending", async () => {
    archive.mockReturnValue(new Promise(() => {}));
    renderEntityArchiveDialog();

    await userEvent.click(screen.getByRole("button", { name: "Archive author" }));

    await waitFor(() => expect(archive).toHaveBeenCalledOnce());
    expect(onOpenChange).not.toHaveBeenCalledWith(false);
    expect(onArchived).not.toHaveBeenCalled();
    expect(toastSuccessMock).not.toHaveBeenCalled();
    expect(warnAboutActiveUsageMock).not.toHaveBeenCalled();
  });

  it("closes on cancel without archiving", async () => {
    renderEntityArchiveDialog();

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(archive).not.toHaveBeenCalled();
  });
});
