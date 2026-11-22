/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Breadcrumbs } from "./Breadcrumbs";

const meta = {
  title: "Navigation / Breadcrumbs",
  component: Breadcrumbs,
} satisfies Meta;

export default meta;

export const Default: StoryObj = {
  render: () => {
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
  },
};

/**
 * A renderer item longer than the item's maximum width ends with an ellipsis; the separator keeps its size.
 */
export const LongItem: StoryObj = {
  render: () => {
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
  },
};
