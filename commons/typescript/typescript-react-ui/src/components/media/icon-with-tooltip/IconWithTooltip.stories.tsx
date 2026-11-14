import type { Story, StoryDefault } from "@ladle/react";
import { HelpCircleIcon } from "lucide-react";
import { IconWithTooltip } from "./IconWithTooltip";

export default {
  title: "Media / IconWithTooltip",
} satisfies StoryDefault;

export const IconWithTooltipStory: Story = () => {
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
};
