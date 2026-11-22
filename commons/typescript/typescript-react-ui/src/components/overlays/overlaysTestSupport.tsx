import { NowProvider, nowMockedValue } from "@neckar-hub/typescript-react-utils/providers";
import { render } from "@testing-library/react";
import { createStore, Provider } from "jotai";
import { DialogRegistry } from "./entity-dialog/DialogRegistry";

/**
 * Renders with a store of its own, so no dialog of an earlier test is still open,
 * and with the fixed `nowMockedValue` that `UnknownOutcomeAlert` reads.
 */
export function renderWithDialogs(ui: React.ReactElement): void {
  render(
    <NowProvider value={nowMockedValue}>
      <Provider store={createStore()}>
        {ui}
        <DialogRegistry />
      </Provider>
    </NowProvider>,
  );
}
