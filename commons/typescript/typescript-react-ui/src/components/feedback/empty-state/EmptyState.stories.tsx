/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import type { Action } from "../../actions/Action";
import { EmptyState as EmptyStateComponent } from "./EmptyState";

/**
 * Meta Information
 */

const meta = {
  title: "Feedback / EmptyState",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

const createContraption: Action = { icon: undefined, label: "Contraption anlegen", onTrigger: () => {} };

export const EmptyStateStory: StoryObj<typeof meta> = {
  name: "EmptyState",
  render: () => {
    return (
      <div className="flex flex-col gap-4">
        <div>
          <pre>label only</pre>
          <EmptyStateComponent label="Noch keine Contraptions vorhanden." />
        </div>
        <div>
          <pre>label and action</pre>
          <EmptyStateComponent label="Noch keine Contraptions vorhanden." action={createContraption} />
        </div>
        <div>
          <pre>size section</pre>
          <EmptyStateComponent size="section" label="Noch keine Contraptions vorhanden." />
        </div>
      </div>
    );
  },
};
