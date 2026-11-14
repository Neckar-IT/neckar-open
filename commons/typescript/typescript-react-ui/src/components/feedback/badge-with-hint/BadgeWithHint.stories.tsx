import type { Story, StoryDefault } from "@ladle/react";
import { TooltipProvider } from "../../primitives/tooltip";
import { BadgeWithHint } from "./BadgeWithHint";

export default {
  title: "Feedback / BadgeWithHint",
} satisfies StoryDefault;

export const BadgeWithHintStory: Story = () => (
  <TooltipProvider>
    <div className="flex gap-4 p-8">
      <BadgeWithHint variant="destructive" hint="No finished event: the run crashed or is still running.">
        Hanging
      </BadgeWithHint>
      <BadgeWithHint variant="default" hint="Every deploy task of the module succeeded." href="https://example.com/job/1">
        Succeeded
      </BadgeWithHint>
    </div>
  </TooltipProvider>
);
BadgeWithHintStory.storyName = "BadgeWithHint";
