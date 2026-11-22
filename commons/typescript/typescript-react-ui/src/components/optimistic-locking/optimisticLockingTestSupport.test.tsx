/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { render } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { decideVersionConflict } from "./optimisticLockingTestSupport";
import { VersionConflictDialog, type VersionConflictDecision } from "./VersionConflictDialog";

/**
 * Tests
 */

function DecidingDialog({ onDecision }: { onDecision: (decision: VersionConflictDecision) => void }): React.ReactElement {
  const [open, setOpen] = useState(true);
  return (
    <I18nProvider options={{ lng: "de" }}>
      {open ? (
        <VersionConflictDialog
          onDecision={(decision) => {
            onDecision(decision);
            setOpen(false);
          }}
        />
      ) : null}
    </I18nProvider>
  );
}

describe("decideVersionConflict", () => {
  it.each<VersionConflictDecision>(["Cancel", "Reload", "SaveAnyway"])("answers the German dialog with %s", async (decision) => {
    const onDecision = vi.fn();
    render(<DecidingDialog onDecision={onDecision} />);

    await decideVersionConflict(decision);

    expect(onDecision).toHaveBeenCalledWith(decision);
  });
});
