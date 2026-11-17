import type { Meta, StoryObj } from "@storybook/react-vite";
import { TooltipProvider } from "../../primitives/tooltip";
import { BadgeWithHint } from "./BadgeWithHint";

const meta = {
  title: "Feedback / BadgeWithHint",
} satisfies Meta;

export default meta;

export const BadgeWithHintStory: StoryObj<typeof meta> = {
  name: "BadgeWithHint",
  render: () => (
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
  ),
};
