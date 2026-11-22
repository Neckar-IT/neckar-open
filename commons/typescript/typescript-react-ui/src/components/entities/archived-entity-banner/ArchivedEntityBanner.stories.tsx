import { ArchiveState } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { ArchiveRestoreIcon } from "lucide-react";
import { type Action } from "../../actions";
import { CodeBlock } from "../../data-display/code-block";
import { ArchivedEntityBanner } from "./ArchivedEntityBanner";

const meta = {
  title: "Entities / ArchivedEntityBanner",
  component: ArchivedEntityBanner,
} satisfies Meta;

export default meta;

const unarchiveAction: Action = {
  icon: <ArchiveRestoreIcon />,
  label: "Reaktivieren",
  onTrigger: () => {
    /* no-op for story preview */
  },
};

interface Case {
  label: string;
  archiveState: ArchiveState;
}

const cases: Case[] = [
  { label: "Archived — banner renders", archiveState: ArchiveState.Archived },
  { label: "Active — banner returns null", archiveState: ArchiveState.Active },
];

export const ArchivedEntityBannerStory: StoryObj = {
  name: "ArchivedEntityBanner",
  render: () => {
    return (
      <div className="flex flex-col gap-8">
        {cases.map((entry) => (
          <div key={entry.label}>
            <CodeBlock className="mb-4">{entry.label}</CodeBlock>
            <ArchivedEntityBanner archiveState={entry.archiveState} unarchiveAction={unarchiveAction} />
          </div>
        ))}
      </div>
    );
  },
};
