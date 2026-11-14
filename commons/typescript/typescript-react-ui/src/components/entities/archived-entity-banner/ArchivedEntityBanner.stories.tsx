import type { Story, StoryDefault } from "@ladle/react";
import { ArchiveState } from "@neckar-hub/typescript-utils/openapi-types";
import { ArchiveRestoreIcon } from "lucide-react";
import { type Action } from "../../actions";
import { CodeBlock } from "../../data-display/code-block";
import { ArchivedEntityBanner } from "./ArchivedEntityBanner";

export default {
  title: "Entities / ArchivedEntityBanner",
} satisfies StoryDefault;

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

export const ArchivedEntityBannerStory: Story = () => {
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
};
ArchivedEntityBannerStory.storyName = "ArchivedEntityBanner";
