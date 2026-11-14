/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { act, render, renderHook, screen } from "@testing-library/react";
import { createStore, Provider } from "jotai";
import { describe, expect, it, vi } from "vitest";
import { createEntityDialogAction } from "./createEntityDialogAction";
import { DialogRegistry } from "./DialogRegistry";
import type { EntityDialogRenderProps } from "./EntityDialogMount";

/**
 * Tests
 *
 * createEntityDialogAction registers one dialog per call and returns the hook of the action that opens it with the
 * given entity and done callback.
 */

function ArchiveDialog({ entity, onDone }: EntityDialogRenderProps<string>): React.ReactElement {
  return (
    <div>
      <p>{`Archive ${entity}`}</p>
      <button type="button" onClick={() => onDone?.()}>
        Done
      </button>
    </div>
  );
}

const translations = { en: { translation: { common: { archive: "Archive" } } } };

function renderDialogRegistry(): ({ children }: { children: React.ReactNode }) => React.ReactElement {
  const store = createStore();
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <I18nProvider languageResources={translations}>
      <Provider store={store}>{children}</Provider>
    </I18nProvider>
  );
  render(<DialogRegistry />, { wrapper });
  return wrapper;
}

describe("createEntityDialogAction", () => {
  it("returns an action with the given icon, the translation of the label key and the given variant", () => {
    const useArchiveAction = createEntityDialogAction<string>({
      dialog: ArchiveDialog,
      icon: <span>archive-icon</span>,
      labelKey: "common.archive",
      variant: "destructive",
      useAllowed: () => true,
    });
    const wrapper = renderDialogRegistry();

    const action = renderHook(() => useArchiveAction("Emma"), { wrapper }).result.current;

    render(<>{action.icon}</>);
    expect(screen.getByText("archive-icon")).toBeInTheDocument();
    expect(action.label).toBe("Archive");
    expect(action.variant).toBe("destructive");
    expect(action.hidden).toBe(false);
  });

  it("hides the action while useAllowed answers false", () => {
    const useArchiveAction = createEntityDialogAction<string>({
      dialog: ArchiveDialog,
      icon: null,
      labelKey: "common.archive",
      variant: "destructive",
      useAllowed: () => false,
    });
    const wrapper = renderDialogRegistry();

    const action = renderHook(() => useArchiveAction("Emma"), { wrapper }).result.current;

    expect(action.hidden).toBe(true);
  });

  it("opens the registered dialog with the entity and runs onDone when the dialog is done", () => {
    const useArchiveAction = createEntityDialogAction<string>({
      dialog: ArchiveDialog,
      icon: null,
      labelKey: "common.archive",
      variant: "destructive",
      useAllowed: () => true,
    });
    const wrapper = renderDialogRegistry();
    const onDone = vi.fn();
    const action = renderHook(() => useArchiveAction("Persuasion", onDone), { wrapper }).result.current;

    act(() => action.onTrigger());
    expect(screen.getByText("Archive Persuasion")).toBeInTheDocument();

    act(() => screen.getByRole("button", { name: "Done" }).click());
    expect(onDone).toHaveBeenCalledTimes(1);
  });

  it("registers an independent dialog per call", () => {
    const useArchiveAction = createEntityDialogAction<string>({
      dialog: ({ entity }) => <p>{`Archive ${entity}`}</p>,
      icon: null,
      labelKey: "common.archive",
      variant: "destructive",
      useAllowed: () => true,
    });
    const useUnarchiveAction = createEntityDialogAction<string>({
      dialog: ({ entity }) => <p>{`Unarchive ${entity}`}</p>,
      icon: null,
      labelKey: "common.unarchive",
      variant: "default",
      useAllowed: () => true,
    });
    const wrapper = renderDialogRegistry();
    const archive = renderHook(() => useArchiveAction("Emma"), { wrapper }).result.current;
    const unarchive = renderHook(() => useUnarchiveAction("Emma"), { wrapper }).result.current;

    act(() => archive.onTrigger());
    expect(screen.getByText("Archive Emma")).toBeInTheDocument();
    expect(screen.queryByText("Unarchive Emma")).not.toBeInTheDocument();

    act(() => unarchive.onTrigger());
    expect(screen.getByText("Unarchive Emma")).toBeInTheDocument();
  });
});
