/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { act, render, renderHook, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createStore, Provider } from "jotai";
import { describe, expect, it } from "vitest";
import { DialogRegistry } from "../overlays/entity-dialog";
import { useConfirmCascadingReset } from "./useConfirmCascadingReset";

/**
 * Tests
 */

describe("useConfirmCascadingReset", () => {
  function renderConfirm(confirmationStatus: ConfirmationStatus | undefined): () => Promise<boolean> {
    const store = createStore();
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <I18nProvider>
        <Provider store={store}>{children}</Provider>
      </I18nProvider>
    );
    render(<DialogRegistry />, { wrapper });
    return renderHook(
      () => useConfirmCascadingReset(confirmationStatus === undefined ? undefined : { confirmationStatus, usedBy: { counts: [] } }),
      { wrapper },
    ).result.current;
  }

  it("lets an entity not stored yet save without asking", async () => {
    const confirm = renderConfirm(undefined);

    await expect(confirm()).resolves.toBe(true);
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
  });

  it("lets a Draft save without asking", async () => {
    const confirm = renderConfirm(ConfirmationStatus.Draft);

    await expect(confirm()).resolves.toBe(true);
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
  });

  it("asks for a Confirmed entity and answers the decision", async () => {
    const user = userEvent.setup();
    const confirm = renderConfirm(ConfirmationStatus.Confirmed);

    let confirmed: Promise<boolean> = Promise.resolve(true);
    act(() => {
      confirmed = confirm();
    });
    await user.click(await screen.findByRole("button", { name: "Cancel" }));

    await expect(confirmed).resolves.toBe(false);
  });

  it("lets a Confirmed entity save when the user saves anyway", async () => {
    const user = userEvent.setup();
    const confirm = renderConfirm(ConfirmationStatus.Confirmed);

    let confirmed: Promise<boolean> = Promise.resolve(false);
    act(() => {
      confirmed = confirm();
    });
    await user.click(await screen.findByRole("button", { name: "Save anyway" }));

    await expect(confirmed).resolves.toBe(true);
  });
});
