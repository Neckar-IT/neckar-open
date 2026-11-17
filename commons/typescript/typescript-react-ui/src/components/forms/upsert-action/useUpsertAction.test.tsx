/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { createStore, Provider } from "jotai";
import { useForm } from "react-hook-form";
import { afterAll, beforeEach, describe, expect, it, vi } from "vitest";
import { DialogRegistry } from "../../overlays/entity-dialog";
import {
  type OtherBadResponse,
  type ServerErrorMapping,
  type UpsertMessages,
  useUpsertAction,
  type UseUpsertActionOptions,
} from "./useUpsertAction";

const toastSuccessMock = vi.fn();
const toastErrorMock = vi.fn();
const resetQueriesMock = vi.fn();

vi.mock("../../feedback", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  toast: { error: (...args: unknown[]) => toastErrorMock(...args), success: (...args: unknown[]) => toastSuccessMock(...args) },
  withActionFailedToast: (_actionLabel: string, mutation: Promise<unknown>) => mutation,
}));

// The mocked module stays in the module cache of a worker that does not isolate test files.
afterAll(() => {
  vi.doUnmock("../../feedback");
  vi.resetModules();
});
vi.mock("@neckar-hub/typescript-react-query", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  resetQueriesExceptPrevented: () => resetQueriesMock(),
  useQueryClient: () => ({}),
}));

/**
 * Tests
 *
 * useUpsertAction sends the form values with the version the form was loaded at, answers the shared
 * responses itself and hands every other bad response to the caller.
 */

interface PublisherValues {
  name: string;
}

interface PublisherRequest {
  name: string;
  baseVersion: number | null;
}

type PublisherResponse =
  | { type: "good"; data: { id: string; currentVersion: number } }
  | { type: "bad/VersionConflict"; baseVersion: number | null; actualVersion: number }
  | { type: "bad/NotFound" }
  | { type: "bad/NameAlreadyExists" };

const mutation = vi.fn<(request: PublisherRequest) => Promise<PublisherResponse>>();
const onSaved = vi.fn();
const mapServerErrors = vi.fn<(response: OtherBadResponse<PublisherResponse>) => void>();

type HarnessOptions = Partial<UseUpsertActionOptions<PublisherValues, PublisherRequest, PublisherResponse>>;

function Harness(options: HarnessOptions): React.ReactElement {
  const form = useForm<PublisherValues>({ defaultValues: { name: "Penguin Books" } });
  const action = useUpsertAction<PublisherValues, PublisherRequest, PublisherResponse>({
    mode: "update",
    form,
    currentVersion: 3,
    mutation,
    toRequest: (values, baseVersion) => ({ ...values, baseVersion }),
    messages: { saved: "Publisher saved", alreadyCreated: "Publisher already created", notFound: "Publisher not found" },
    onSaved,
    mapServerErrors,
    ...options,
  });
  return (
    <button type="button" onClick={action.onTrigger}>
      {action.label}
    </button>
  );
}

/** Renders the action; the returned function renders it again with other options, as a refetch does. */
function renderAction(options: HarnessOptions = {}): (nextOptions: HarnessOptions) => void {
  const store = createStore();
  const ui = (harnessOptions: HarnessOptions) => (
    <I18nProvider>
      <Provider store={store}>
        <Harness {...harnessOptions} />
        <DialogRegistry />
      </Provider>
    </I18nProvider>
  );
  const { rerender } = render(ui(options));
  return (nextOptions) => rerender(ui(nextOptions));
}

async function save(): Promise<void> {
  fireEvent.click(await screen.findByRole("button", { name: "Save" }));
}

async function decideVersionConflict(name: "Cancel" | "Discard and reload" | "Save anyway"): Promise<void> {
  fireEvent.click(await screen.findByRole("button", { name }));
  await waitFor(() => expect(screen.queryByRole("alertdialog")).toBeNull());
}

const conflict = (baseVersion: number | null, actualVersion: number): PublisherResponse => ({
  type: "bad/VersionConflict",
  baseVersion,
  actualVersion,
});

describe("useUpsertAction", () => {
  beforeEach(() => {
    mutation.mockReset();
    onSaved.mockReset();
    mapServerErrors.mockReset();
    toastSuccessMock.mockReset();
    toastErrorMock.mockReset();
    resetQueriesMock.mockReset();
  });

  it("toasts a save, hands the saved record over and builds the next save on its version", async () => {
    mutation.mockResolvedValue({ type: "good", data: { id: "p-1", currentVersion: 4 } });
    renderAction();

    await save();
    await waitFor(() => expect(onSaved).toHaveBeenCalledWith({ id: "p-1", currentVersion: 4 }));
    await save();

    await waitFor(() => expect(mutation).toHaveBeenCalledTimes(2));
    expect(toastSuccessMock).toHaveBeenCalledWith("Publisher saved");
    expect(mutation.mock.calls.map(([request]) => request.baseVersion)).toEqual([3, 4]);
  });

  it("sends no version on a creation, and toasts an id a creation took meanwhile", async () => {
    mutation.mockResolvedValue(conflict(null, 1));
    renderAction({ mode: "new" });

    await save();

    await waitFor(() => expect(toastErrorMock).toHaveBeenCalledWith("Publisher already created"));
    expect(mutation.mock.calls[0]?.[0]).toEqual({ name: "Penguin Books", baseVersion: null });
    expect(screen.queryByRole("alertdialog")).toBeNull();
  });

  it("sends no version for an entity without one", async () => {
    mutation.mockResolvedValue({ type: "good", data: { id: "p-1", currentVersion: 4 } });
    renderAction({ mode: undefined, currentVersion: undefined });

    await save();

    await waitFor(() => expect(mutation).toHaveBeenCalledTimes(1));
    expect(mutation.mock.calls[0]?.[0].baseVersion).toBeNull();
  });

  it("keeps the version the form was loaded at when a refetch hands a newer one", async () => {
    mutation.mockResolvedValue({ type: "good", data: { id: "p-1", currentVersion: 6 } });
    const renderAgain = renderAction({ currentVersion: 3 });

    renderAgain({ currentVersion: 5 });
    await save();

    await waitFor(() => expect(mutation).toHaveBeenCalledTimes(1));
    expect(mutation.mock.calls[0]?.[0].baseVersion).toBe(3);
  });

  it("writes over the stored version when the user saves anyway", async () => {
    mutation.mockResolvedValueOnce(conflict(3, 9)).mockResolvedValueOnce({ type: "good", data: { id: "p-1", currentVersion: 10 } });
    renderAction();

    await save();
    await decideVersionConflict("Save anyway");

    await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
    expect(mutation.mock.calls.map(([request]) => request.baseVersion)).toEqual([3, 9]);
  });

  it("asks confirmBeforeSave once when the user saves anyway", async () => {
    const confirmBeforeSave = vi.fn(async () => true);
    mutation.mockResolvedValueOnce(conflict(3, 9)).mockResolvedValueOnce({ type: "good", data: { id: "p-1", currentVersion: 10 } });
    renderAction({ confirmBeforeSave });

    await save();
    await decideVersionConflict("Save anyway");

    await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
    expect(confirmBeforeSave).toHaveBeenCalledTimes(1);
  });

  it("resets the queries once when the user reloads", async () => {
    mutation.mockResolvedValue(conflict(3, 9));
    renderAction();

    await save();
    await decideVersionConflict("Discard and reload");

    await waitFor(() => expect(resetQueriesMock).toHaveBeenCalledTimes(1));
    expect(mutation).toHaveBeenCalledTimes(1);
  });

  it("sends nothing when the user cancels, and asks again on the next save", async () => {
    mutation.mockResolvedValue(conflict(3, 9));
    renderAction();

    await save();
    await decideVersionConflict("Cancel");
    await save();
    await decideVersionConflict("Cancel");

    expect(mutation.mock.calls.map(([request]) => request.baseVersion)).toEqual([3, 3]);
    expect(resetQueriesMock).not.toHaveBeenCalled();
  });

  it("toasts a record that is gone", async () => {
    mutation.mockResolvedValue({ type: "bad/NotFound" });
    renderAction();

    await save();

    await waitFor(() => expect(toastErrorMock).toHaveBeenCalledWith("Publisher not found"));
    expect(mapServerErrors).not.toHaveBeenCalled();
  });

  it("hands every other bad response to mapServerErrors", async () => {
    mutation.mockResolvedValue({ type: "bad/NameAlreadyExists" });
    renderAction();

    await save();

    await waitFor(() => expect(mapServerErrors).toHaveBeenCalledWith({ type: "bad/NameAlreadyExists" }));
  });

  it("sends nothing when confirmBeforeSave answers false", async () => {
    const confirmBeforeSave = vi.fn(async () => false);
    renderAction({ confirmBeforeSave });

    await save();

    await waitFor(() => expect(confirmBeforeSave).toHaveBeenCalledWith({ name: "Penguin Books" }));
    expect(mutation).not.toHaveBeenCalled();
  });

  it("requires the text and the mapping of every answer the endpoint sends, and accepts a mapping of a good-only endpoint", () => {
    // @ts-expect-error The endpoint answers bad/NotFound, so the notFound text is required.
    const withoutNotFound: UpsertMessages<PublisherResponse> = { saved: "Publisher saved", alreadyCreated: "Publisher already created" };
    // @ts-expect-error The endpoint answers bad/NameAlreadyExists, so mapServerErrors is required.
    const withoutMapping: ServerErrorMapping<PublisherResponse> = {};
    const onlySaved: UpsertMessages<{ type: "good"; data: { id: string } }> = { saved: "Publisher saved" };
    const goodOnlyWithoutMapping: ServerErrorMapping<{ type: "good"; data: { id: string } }> = {};
    const goodOnlyWithMapping: ServerErrorMapping<{ type: "good"; data: { id: string } }> = { mapServerErrors: (_response: never) => {} };

    expect([withoutNotFound, withoutMapping, onlySaved, goodOnlyWithoutMapping, goodOnlyWithMapping]).toHaveLength(5);
  });
});
