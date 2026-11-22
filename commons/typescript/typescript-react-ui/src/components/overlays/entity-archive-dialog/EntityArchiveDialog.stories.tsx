/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Button } from "../../primitives/button";
import { EntityArchiveDialog } from "./EntityArchiveDialog";
import { EntityArchiveUsage } from "./EntityArchiveUsage";

const meta = {
  title: "Overlays / EntityArchiveDialog",
  component: EntityArchiveDialog,
} satisfies Meta;

export default meta;

/**
 * Story Resources
 */

const languageResources = {
  en: {
    translation: {
      authors: {
        archive: {
          title: "Archive:",
          description: "The author is hidden from new books.",
          confirm: "Archive author",
          activeUsage_Book_one: "One active book refers to this author.",
          activeUsage_Book_other: "{{count}} active books refer to this author.",
          archivedUsage_Book_one: "One archived book holds a reference as well.",
          archivedUsage_Book_other: "{{count}} archived books hold a reference as well.",
        },
        messages: { archived: "Author archived", archiveFailed: "Author could not be archived" },
      },
    },
  },
  de: {
    translation: {
      authors: {
        archive: {
          title: "Archivieren:",
          description: "Der Autor wird für neue Bücher ausgeblendet.",
          confirm: "Autor archivieren",
          activeUsage_Book_one: "Ein aktives Buch verweist auf diesen Autor.",
          activeUsage_Book_other: "{{count}} aktive Bücher verweisen auf diesen Autor.",
          archivedUsage_Book_one: "Ein archiviertes Buch enthält ebenfalls eine Referenz.",
          archivedUsage_Book_other: "{{count}} archivierte Bücher enthalten ebenfalls eine Referenz.",
        },
        messages: { archived: "Autor archiviert", archiveFailed: "Autor konnte nicht archiviert werden" },
      },
    },
  },
};

/**
 * Stories
 */

function EntityArchiveDialogStoryBody({ usage }: { usage?: React.ReactNode }): React.ReactElement {
  const [open, setOpen] = useState(true);

  return (
    <I18nProvider languageResources={languageResources}>
      <Button onClick={() => setOpen(true)}>Archive</Button>

      {open ? (
        <EntityArchiveDialog
          title="authors.archive.title"
          display={<span>Jane Austen</span>}
          description="authors.archive.description"
          usage={usage}
          confirmLabel="authors.archive.confirm"
          messages={{ archived: "authors.messages.archived", failed: "authors.messages.archiveFailed" }}
          archive={() => Promise.resolve({ type: "good", usedBy: { counts: [] } })}
          onOpenChange={setOpen}
        />
      ) : null}
    </I18nProvider>
  );
}

export const EntityArchiveDialogStory: StoryObj = {
  name: "EntityArchiveDialog",
  render: () => <EntityArchiveDialogStoryBody />,
};

/**
 * The `usage` block (here a list of dependent records) renders below the description, outside it.
 */
export const EntityArchiveDialogWithUsageStory: StoryObj = {
  name: "EntityArchiveDialog (with usage)",
  render: () => (
    <EntityArchiveDialogStoryBody
      usage={
        <div className="text-muted-foreground text-sm">
          Used by:
          <ul className="mt-1 list-disc pl-7">
            <li>3 active books</li>
            <li>1 active series</li>
          </ul>
        </div>
      }
    />
  ),
};

export const EntityArchiveDialogWithEntityArchiveUsageStory: StoryObj = {
  name: "EntityArchiveDialog (with EntityArchiveUsage)",
  render: () => (
    <EntityArchiveDialogStoryBody
      usage={
        <EntityArchiveUsage counts={[{ type: "Book", activeCount: 3, archivedCount: 2 }]} isLoading={false} keyPrefix="authors.archive" />
      }
    />
  ),
};
