/**
 * Imports
 */

import { action, type Story, type StoryDefault } from "@ladle/react";
import { MenuIcon, XIcon } from "lucide-react";
import { UnstyledButton } from "./UnstyledButton";

/**
 * Meta Information
 */

export default {
  title: "Primitives / UnstyledButton",
} satisfies StoryDefault;

/**
 * Stories
 */

export const UnstyledButtonStory: Story = () => (
  <div className="grid grid-cols-2 gap-16">
    <div>
      <h3>Default (icon + text)</h3>
      <p className="text-muted-foreground mb-4 text-sm">
        No background, padding or border — only behaviour: cursor, focus ring, disabled state.
      </p>
      <div className="flex flex-col gap-4">
        <UnstyledButton aria-label="Close menu" onClick={action("onClick:close")}>
          <XIcon className="h-6 w-6" />
        </UnstyledButton>
        <UnstyledButton onClick={action("onClick:plain-text")}>Plain text button</UnstyledButton>
      </div>
    </div>

    <div>
      <h3>States</h3>
      <p className="text-muted-foreground mb-4 text-sm">
        Disabled state shows <code>cursor-not-allowed</code> + <code>opacity-50</code>.
      </p>
      <div className="flex flex-col gap-4">
        <UnstyledButton onClick={action("onClick:enabled")}>Enabled</UnstyledButton>
        <UnstyledButton disabled onClick={action("onClick:disabled")}>
          Disabled
        </UnstyledButton>
      </div>
    </div>

    <div>
      <h3>Custom styling via className</h3>
      <p className="text-muted-foreground mb-4 text-sm">The consumer decides visual surface. Common shapes:</p>
      <div className="flex flex-col gap-4">
        <UnstyledButton className="text-primary text-sm underline" onClick={action("onClick:link-look")}>
          Link-look button
        </UnstyledButton>
        <UnstyledButton
          className="hover:bg-muted/30 flex w-full items-center gap-2 rounded-sm px-2 py-1 disabled:hover:bg-transparent"
          onClick={action("onClick:panel")}
        >
          <MenuIcon className="h-4 w-4" />
          <span>Panel-style button</span>
        </UnstyledButton>
      </div>
    </div>

    <div>
      <h3>asChild — render as &lt;a&gt;</h3>
      <p className="text-muted-foreground mb-4 text-sm">
        When a link should behave and focus like a button, <code>asChild</code> merges the props onto the child element instead of emitting
        a separate <code>&lt;button&gt;</code>.
      </p>
      <div className="flex flex-col gap-4">
        <UnstyledButton asChild>
          <a className="text-primary underline" href="https://ladle.dev/" rel="noreferrer" target="_blank">
            Open ladle.dev (rendered as &lt;a&gt;)
          </a>
        </UnstyledButton>
      </div>
    </div>
  </div>
);
UnstyledButtonStory.storyName = "UnstyledButton";
