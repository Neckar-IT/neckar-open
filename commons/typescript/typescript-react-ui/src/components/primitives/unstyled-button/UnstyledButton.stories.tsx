/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { MenuIcon, XIcon } from "lucide-react";
import { fn } from "storybook/test";
import { UnstyledButton } from "./UnstyledButton";

const meta = {
  title: "Primitives / UnstyledButton",
  component: UnstyledButton,
} satisfies Meta;

export default meta;

export const UnstyledButtonStory: StoryObj = {
  name: "UnstyledButton",
  render: () => (
    <div className="grid grid-cols-2 gap-16">
      <div>
        <h3>Default (icon + text)</h3>
        <p className="text-muted-foreground mb-4 text-sm">
          No background, padding or border — only behaviour: cursor, focus ring, disabled state.
        </p>
        <div className="flex flex-col gap-4">
          <UnstyledButton aria-label="Close menu" onClick={fn().mockName("onClick:close")}>
            <XIcon className="h-6 w-6" />
          </UnstyledButton>
          <UnstyledButton onClick={fn().mockName("onClick:plain-text")}>Plain text button</UnstyledButton>
        </div>
      </div>

      <div>
        <h3>States</h3>
        <p className="text-muted-foreground mb-4 text-sm">
          Disabled state shows <code>cursor-not-allowed</code> + <code>opacity-50</code>.
        </p>
        <div className="flex flex-col gap-4">
          <UnstyledButton onClick={fn().mockName("onClick:enabled")}>Enabled</UnstyledButton>
          <UnstyledButton disabled onClick={fn().mockName("onClick:disabled")}>
            Disabled
          </UnstyledButton>
        </div>
      </div>

      <div>
        <h3>Custom styling via className</h3>
        <p className="text-muted-foreground mb-4 text-sm">The consumer decides visual surface. Common shapes:</p>
        <div className="flex flex-col gap-4">
          <UnstyledButton className="text-primary text-sm underline" onClick={fn().mockName("onClick:link-look")}>
            Link-look button
          </UnstyledButton>
          <UnstyledButton
            className="hover:bg-muted/30 flex w-full items-center gap-2 rounded-sm px-2 py-1 disabled:hover:bg-transparent"
            onClick={fn().mockName("onClick:panel")}
          >
            <MenuIcon className="h-4 w-4" />
            <span>Panel-style button</span>
          </UnstyledButton>
        </div>
      </div>

      <div>
        <h3>asChild — render as &lt;a&gt;</h3>
        <p className="text-muted-foreground mb-4 text-sm">
          When a link should behave and focus like a button, <code>asChild</code> merges the props onto the child element instead of
          emitting a separate <code>&lt;button&gt;</code>.
        </p>
        <div className="flex flex-col gap-4">
          <UnstyledButton asChild>
            <a className="text-primary underline" href="https://storybook.js.org/" rel="noreferrer" target="_blank">
              Open storybook.js.org (rendered as &lt;a&gt;)
            </a>
          </UnstyledButton>
        </div>
      </div>
    </div>
  ),
};
