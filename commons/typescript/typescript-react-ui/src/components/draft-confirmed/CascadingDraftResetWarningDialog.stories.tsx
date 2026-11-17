/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Button } from "../primitives/button";
import { CascadingDraftResetWarningDialog } from "./CascadingDraftResetWarningDialog";

/**
 * Meta Information
 */

const meta = {
  title: "Draft confirmed / CascadingDraftResetWarningDialog",
} satisfies Meta;

export default meta;

/**
 * Story Resources
 */

const languageResources = {
  en: { translation: { entities: { Book: "{{count}} book", Book_other: "{{count}} books", Series: "{{count}} series" } } },
  de: { translation: { entities: { Book: "{{count}} Buch", Book_other: "{{count}} Bücher", Series: "{{count}} Serie" } } },
};

/**
 * Stories
 */

function CascadingDraftResetWarningDialogStoryBody({
  usedByCounts,
}: {
  usedByCounts: { type: string; activeConfirmedCount: number }[];
}): React.ReactElement {
  const [open, setOpen] = useState(true);

  return (
    <I18nProvider languageResources={languageResources}>
      <Button onClick={() => setOpen(true)}>Save</Button>

      {open ? (
        <CascadingDraftResetWarningDialog usedByCounts={usedByCounts} onOpenChange={setOpen} onSubmit={() => setOpen(false)} />
      ) : null}
    </I18nProvider>
  );
}

/**
 * Two entity types with confirmed records: the list renders below the description, outside it.
 */
export const CascadingDraftResetWarningDialogStory: StoryObj<typeof meta> = {
  name: "CascadingDraftResetWarningDialog",
  render: () => (
    <CascadingDraftResetWarningDialogStoryBody
      usedByCounts={[
        { type: "Book", activeConfirmedCount: 3 },
        { type: "Series", activeConfirmedCount: 1 },
      ]}
    />
  ),
};

/**
 * No entity has confirmed records: the dialog shows the description only.
 */
export const CascadingDraftResetWarningDialogWithoutListStory: StoryObj<typeof meta> = {
  name: "CascadingDraftResetWarningDialog (no list)",
  render: () => <CascadingDraftResetWarningDialogStoryBody usedByCounts={[]} />,
};
