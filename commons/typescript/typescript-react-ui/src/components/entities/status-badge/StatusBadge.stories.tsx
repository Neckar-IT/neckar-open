import type { Meta, StoryObj } from "@storybook/react-vite";
import { ArchiveIcon, CircleXIcon, HourglassIcon } from "lucide-react";
import { CodeBlock } from "../../data-display/code-block";
import { TooltipProvider } from "../../primitives/tooltip";
import { StatusBadge, type StatusBadgeTone } from "./StatusBadge";

const meta = {
  title: "Entities / StatusBadge",
  component: StatusBadge,
} satisfies Meta;

export default meta;

const tones: StatusBadgeTone[] = ["muted", "info", "success", "warning", "danger"];
const variants = ["icon", "with-text"] as const;

export const StatusBadgeStory: StoryObj = {
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

export const WithHintAndLink: StoryObj = {
  name: "StatusBadge with hint and link",
  render: () => {
    return (
      <TooltipProvider>
        <div className="flex items-center gap-4">
          {variants.map((variant) => (
            <StatusBadge
              key={variant}
              tone="warning"
              icon={<HourglassIcon />}
              label="Hanging"
              hint="The newest started event has no finished event."
              variant={variant}
            />
          ))}
          {variants.map((variant) => (
            <StatusBadge
              key={variant}
              tone="danger"
              icon={<CircleXIcon />}
              label="Failed"
              hint="One of the module's deploy tasks failed."
              href="https://git.example/job/1"
              variant={variant}
            />
          ))}
        </div>
      </TooltipProvider>
    );
  },
};
