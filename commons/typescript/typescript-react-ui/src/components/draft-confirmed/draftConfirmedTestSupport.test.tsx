/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CascadingDraftResetWarningDialog } from "./CascadingDraftResetWarningDialog";
import { saveDespiteCascadingReset } from "./draftConfirmedTestSupport";

/**
 * Tests
 */

describe("saveDespiteCascadingReset", () => {
  it("submits the German dialog", async () => {
    const onSubmit = vi.fn();
    render(
      <I18nProvider options={{ lng: "de" }}>
        <CascadingDraftResetWarningDialog usedByCounts={[]} onOpenChange={vi.fn()} onSubmit={onSubmit} />
      </I18nProvider>,
    );
    await screen.findByRole("alertdialog");

    saveDespiteCascadingReset();

    expect(onSubmit).toHaveBeenCalledTimes(1);
  });
});
