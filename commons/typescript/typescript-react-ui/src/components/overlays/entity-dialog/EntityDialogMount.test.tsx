/**
 * Imports
 */

import { act, render, screen } from "@test/test-utils";
import { createStore, Provider } from "jotai";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { createEntityDialogAtom } from "./entityDialogAtom";
import { EntityDialogMount, type EntityDialogRenderProps } from "./EntityDialogMount";

/**
 * Tests
 *
 * EntityDialogMount renders the dialog of its atom while it is visible and hides it when the dialog closes.
 */

function TestDialog({ entity, onOpenChange, onDone }: EntityDialogRenderProps<string>): React.ReactElement {
  return (
    <div>
      <p>{`Dialog for ${entity}`}</p>
      <button type="button" onClick={() => onOpenChange(false)}>
        Close
      </button>
      <button type="button" onClick={() => onDone?.()}>
        Done
      </button>
    </div>
  );
}

describe("EntityDialogMount", () => {
  it("renders nothing while the dialog is hidden", () => {
    const dialogAtom = createEntityDialogAtom(TestDialog);
    render(
      <Provider store={createStore()}>
        <EntityDialogMount atom={dialogAtom} render={TestDialog} />
      </Provider>,
    );

    expect(screen.queryByText(/Dialog for/)).not.toBeInTheDocument();
  });

  it("renders the visible entity, hands over onDone and hides on close", () => {
    const store = createStore();
    const dialogAtom = createEntityDialogAtom(TestDialog);
    const onDone = vi.fn();
    render(
      <Provider store={store}>
        <EntityDialogMount atom={dialogAtom} render={TestDialog} />
      </Provider>,
    );

    act(() => store.set(dialogAtom, { type: "Visible", entity: "Pride and Prejudice", onDone }));
    expect(screen.getByText("Dialog for Pride and Prejudice")).toBeInTheDocument();

    act(() => screen.getByRole("button", { name: "Done" }).click());
    expect(onDone).toHaveBeenCalledTimes(1);

    act(() => screen.getByRole("button", { name: "Close" }).click());
    expect(screen.queryByText(/Dialog for/)).not.toBeInTheDocument();
    expect(store.get(dialogAtom)).toEqual({ type: "Hidden" });
  });

  it("renders a dialog that calls a hook, also after it was hidden", () => {
    const store = createStore();
    const dialogAtom = createEntityDialogAtom(TestDialog);
    function CountingDialog({ entity }: EntityDialogRenderProps<string>): React.ReactElement {
      const [count] = useState(1);
      return <p>{`${entity} ${count}`}</p>;
    }
    render(
      <Provider store={store}>
        <EntityDialogMount atom={dialogAtom} render={CountingDialog} />
      </Provider>,
    );

    act(() => store.set(dialogAtom, { type: "Visible", entity: "Emma" }));
    act(() => store.set(dialogAtom, { type: "Hidden" }));
    act(() => store.set(dialogAtom, { type: "Visible", entity: "Persuasion" }));

    expect(screen.getByText("Persuasion 1")).toBeInTheDocument();
  });
});
