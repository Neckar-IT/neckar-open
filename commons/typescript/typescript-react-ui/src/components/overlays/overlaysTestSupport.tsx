import { render } from "@testing-library/react";
import { createStore, Provider } from "jotai";
import { DialogRegistry } from "./entity-dialog/DialogRegistry";

/** Renders with a store of its own, so no dialog of an earlier test is still open. */
export function renderWithDialogs(ui: React.ReactElement): void {
  render(
    <Provider store={createStore()}>
      {ui}
      <DialogRegistry />
    </Provider>,
  );
}
