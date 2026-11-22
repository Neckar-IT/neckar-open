import type { Meta, StoryObj } from "@storybook/react-vite";
import { HelpCircleIcon } from "lucide-react";
import { IconWithTooltip } from "./IconWithTooltip";

const meta = {
  title: "Media / IconWithTooltip",
  component: IconWithTooltip,
} satisfies Meta;

export default meta;

export const IconWithTooltipStory: StoryObj = {
  render: () => {
    return (
      <div className="flex flex-col gap-8">
        <IconWithTooltip tooltip="This is a tooltip">
          <HelpCircleIcon />
        </IconWithTooltip>

        <IconWithTooltip tooltip="This is a tooltip with custom icon size" customIconSize={true}>
          <HelpCircleIcon className="size-6 text-blue-600" />
        </IconWithTooltip>
      </div>
    );
  },
};
