/**
 * Imports
 */

import { elementAt, render, screen, waitFor } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { afterAll, beforeEach, describe, expect, it, vi } from "vitest";
import de from "../../localization/de.json";
import en from "../../localization/en.json";
import { ConfirmEntityDialog, type ConfirmEntityResponse } from "./ConfirmEntityDialog";

const toastErrorMock = vi.fn();
const resetQueriesMock = vi.fn();

vi.mock("../feedback", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  toast: { success: vi.fn(), error: (...args: unknown[]) => toastErrorMock(...args) },
}));

vi.mock("@neckar-hub/typescript-react-query", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  resetQueriesExceptPrevented: () => resetQueriesMock(),
  useQueryClient: () => ({}),
}));

// The mocked module stays in the module cache of a worker that does not isolate test files.
afterAll(() => {
  vi.doUnmock("../feedback");
  vi.doUnmock("@neckar-hub/typescript-react-query");
  vi.resetModules();
});

/**
 * Tests
 */

const onOpenChange = vi.fn<(open: boolean) => void>();

const messages = { confirmed: "Confirmed", validationFailed: "Validation failed", notFoundDetails: "Not found" };

async function confirmWith(response: ConfirmEntityResponse, versionConflict?: string): Promise<void> {
  render(
    <ConfirmEntityDialog
      onOpenChange={onOpenChange}
      onConfirm={() => Promise.resolve(response)}
      isPending={false}
      messages={{ ...messages, versionConflict }}
    />,
  );
  await userEvent.click(elementAt(screen.getAllByRole("button"), -1));
}

describe("ConfirmEntityDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("toasts the version-conflict message of the caller, reloads the queries and closes", async () => {
    await confirmWith({ type: "bad/VersionConflict" }, "Author changed meanwhile");

    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
    expect(toastErrorMock).toHaveBeenCalledWith("Author changed meanwhile");
    expect(resetQueriesMock).toHaveBeenCalledOnce();
  });

  it("leaves the reset of the queries to the query client on a confirmation that succeeds", async () => {
    await confirmWith({ type: "good" });

    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
    expect(resetQueriesMock).not.toHaveBeenCalled();
  });

  it("toasts the embedded version-conflict text when the caller passes none", async () => {
    await confirmWith({ type: "bad/VersionConflict" });

    await waitFor(() => expect(toastErrorMock).toHaveBeenCalledOnce());
    expect([de.confirmation.versionConflict, en.confirmation.versionConflict]).toContain(toastErrorMock.mock.calls[0]?.[0]);
  });
});
