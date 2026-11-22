import { fireEvent, screen } from "@testing-library/react";
import de from "../../localization/de.json";

/** Clicks the German save button of the open `CascadingDraftResetWarningDialog`. */
export function saveDespiteCascadingReset(): void {
  fireEvent.click(screen.getByRole("button", { name: de.confirmation.cascadingReset.saveAnyway }));
}
