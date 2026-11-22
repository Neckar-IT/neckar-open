import { ArchiveState, ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { CodeBlock } from "../../data-display/code-block";
import { EntityStatusBadge } from "./EntityStatusBadge";

const meta = {
  title: "Entities / EntityStatusBadge",
  component: EntityStatusBadge,
} satisfies Meta;

export default meta;

const variants = ["icon", "with-text"] as const;

interface Case {
  label: string;
  entity: { archiveState: ArchiveState; confirmationStatus?: ConfirmationStatus } | null;
}

const cases: Case[] = [
  { label: "Archived", entity: { archiveState: ArchiveState.Archived } },
  {
    label: "Archived + Draft (archive dominates)",
    entity: { archiveState: ArchiveState.Archived, confirmationStatus: ConfirmationStatus.Draft },
  },
  {
    label: "Archived + Confirmed (archive dominates)",
    entity: { archiveState: ArchiveState.Archived, confirmationStatus: ConfirmationStatus.Confirmed },
  },
  { label: "Active + Draft", entity: { archiveState: ArchiveState.Active, confirmationStatus: ConfirmationStatus.Draft } },
  { label: "Active + Confirmed", entity: { archiveState: ArchiveState.Active, confirmationStatus: ConfirmationStatus.Confirmed } },
  { label: "Active (no confirmationStatus — renders nothing)", entity: { archiveState: ArchiveState.Active } },
  { label: "null entity (renders nothing)", entity: null },
];

export const EntityStatusBadgeStory: StoryObj = {
  name: "EntityStatusBadge",
  render: () => {
    return (
      <div className="grid grid-cols-2 gap-4">
        {cases.flatMap((entry) =>
          variants.map((variant) => (
            <div key={`${entry.label}-${variant}`}>
              <CodeBlock className="mb-4">{`${entry.label} — ${variant}`}</CodeBlock>
              <EntityStatusBadge entity={entry.entity} variant={variant} />
            </div>
          )),
        )}
      </div>
    );
  },
};
