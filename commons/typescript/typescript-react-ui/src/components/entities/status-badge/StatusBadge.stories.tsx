import type { Meta, StoryObj } from "@storybook/react-vite";
import { ArchiveIcon } from "lucide-react";
import { CodeBlock } from "../../data-display/code-block";
import { StatusBadge, type StatusBadgeTone } from "./StatusBadge";

const meta = {
  title: "Entities / StatusBadge",
} satisfies Meta;

export default meta;

const tones: StatusBadgeTone[] = ["muted", "info", "success", "warning", "danger"];
const variants = ["icon", "with-text"] as const;

export const StatusBadgeStory: StoryObj<typeof meta> = {
  name: "StatusBadge",
  render: () => {
    return (
      <div className="grid grid-cols-3 gap-4">
        {tones.map((tone) => (
          <div key={tone}>
            <CodeBlock className="mb-4">{tone}</CodeBlock>
            <div className="flex items-center gap-4">
              {variants.map((variant) => (
                <StatusBadge key={variant} tone={tone} icon={<ArchiveIcon />} label="Archived" variant={variant} />
              ))}
              <StatusBadge tone={tone} icon={<ArchiveIcon />} label="Archived" variant="with-text" loading />
            </div>
          </div>
        ))}
      </div>
    );
  },
};
