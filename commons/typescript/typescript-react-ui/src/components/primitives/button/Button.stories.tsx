/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { MailIcon } from "lucide-react";
import { Button } from "./Button";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Button",
} satisfies StoryDefault;

/**
 * Stories
 */

export const ButtonStory: Story = () => (
  <div className="grid grid-cols-4 gap-16">
    <div>
      <h3>Variants</h3>
      <div className="flex flex-col space-y-4">
        <Button>Primary</Button>
        <Button variant="confirm">Confirm</Button>
        <Button variant="secondary">Secondary</Button>
        <Button variant="destructive">Destructive</Button>
        <Button variant="outline">Outline</Button>
        <Button variant="ghost">Ghost</Button>
        <Button variant="link">Link</Button>
        <Button>
          <MailIcon />
          <span>with Icon</span>
        </Button>
      </div>
    </div>
    <div>
      <h3>Sizes</h3>
      <div className="flex flex-col space-y-4">
        <Button size="sm">Small</Button>
        <Button size="default">Default</Button>
        <Button size="lg">Large</Button>
        <Button size="icon">
          <MailIcon />
        </Button>
      </div>
    </div>
    <div>
      <h3>States</h3>
      <div className="flex flex-col space-y-4">
        <Button disabled={true}>Disabled</Button>
        <Button loading={true}>Loading</Button>
        <Button variant="outline" loading={true}>
          <MailIcon />
          Loading with Icon
        </Button>
      </div>
    </div>
  </div>
);
ButtonStory.storyName = "Button";
