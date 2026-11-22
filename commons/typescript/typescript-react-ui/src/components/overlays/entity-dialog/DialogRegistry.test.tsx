/**
 * Imports
 */

import { act, render, renderHook, screen } from "@test/test-utils";
import { createStore, Provider } from "jotai";
import { describe, expect, it, vi } from "vitest";
import { DialogRegistry } from "./DialogRegistry";
import { createEntityDialogAtom, useOpenEntityDialog } from "./entityDialogAtom";

/**
 * Tests
 *
 * DialogRegistry renders the dialog of every atom created through createEntityDialogAtom, also of one created
 * after its first render.
 */

describe("DialogRegistry", () => {
  it("shows the dialog of an atom created after the registry rendered", () => {
    const store = createStore();
    render(
      <Provider store={store}>
        <DialogRegistry />
      </Provider>,
    );

    act(() => {
      const dialogAtom = createEntityDialogAtom<string>(({ entity }) => <p>{`Archive ${entity}`}</p>);
      store.set(dialogAtom, { type: "Visible", entity: "Emma" });
    });

    expect(screen.getByText("Archive Emma")).toBeInTheDocument();
  });

  it("opens a dialog through useOpenEntityDialog and runs onDone when the dialog is done", () => {
    const store = createStore();
    const dialogAtom = createEntityDialogAtom<string>(({ entity, onDone }) => (
      <button type="button" onClick={() => onDone?.()}>{`Confirm ${entity}`}</button>
    ));
    render(
      <Provider store={store}>
        <DialogRegistry />
      </Provider>,
    );
    const { result } = renderHook(() => useOpenEntityDialog(dialogAtom), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });
    const onDone = vi.fn();

    act(() => result.current("Persuasion", onDone));
    act(() => screen.getByRole("button", { name: "Confirm Persuasion" }).click());

    expect(onDone).toHaveBeenCalledTimes(1);
  });
});
