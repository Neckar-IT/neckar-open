/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import type { Action } from "../../actions/Action";
import { EmptyState as EmptyStateComponent } from "./EmptyState";

/**
 * Meta Information
 */

export default {
  title: "Feedback / EmptyState",
} satisfies StoryDefault;

/**
 * Stories
 */

const createContraption: Action = { icon: undefined, label: "Contraption anlegen", onTrigger: () => {} };

export const EmptyStateStory: Story = () => {
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
};
EmptyStateStory.storyName = "EmptyState";
