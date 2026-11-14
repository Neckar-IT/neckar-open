/**
 * Imports
 */

import { act, render, renderHook, screen } from "@test/test-utils";
import { createStore, Provider } from "jotai";
import { describe, expect, it } from "vitest";
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

  it("opens a dialog through useOpenEntityDialog", () => {
    const store = createStore();
    const dialogAtom = createEntityDialogAtom<string>(({ entity }) => <p>{`Confirm ${entity}`}</p>);
    render(
      <Provider store={store}>
        <DialogRegistry />
      </Provider>,
    );
    const { result } = renderHook(() => useOpenEntityDialog(dialogAtom), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    act(() => result.current("Persuasion"));

    expect(screen.getByText("Confirm Persuasion")).toBeInTheDocument();
  });
});
