/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Breadcrumbs } from "./Breadcrumbs";

/**
 * Meta Information
 */

export default {
  title: "Navigation / Breadcrumbs",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Default: Story = () => {
  return (
    <Breadcrumbs>
      <a data-text-content href="#overview">
        Overview
      </a>
      <span data-text-content className="font-mono">
        f247c3742da
      </span>
    </Breadcrumbs>
  );
};

/**
 * A renderer item longer than the item's maximum width ends with an ellipsis; the separator keeps its size.
 */
export const LongItem: Story = () => {
  return (
    <Breadcrumbs>
      <a data-text-content href="#overview">
        Overview
      </a>
      <span data-text-content className="font-mono text-sm">
        <span>:services:orchestration:agent-orchestrator-deployment-with-a-long-module-path</span>
      </span>
    </Breadcrumbs>
  );
};
