/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/ladle";
import { CatchAll } from "./CatchAll";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Pages / CatchAll",
  decorators: [
    (Story) => (
      <WithMemoryRouter>
        <Story />
      </WithMemoryRouter>
    ),
  ],
};

export default meta;

/**
 * Stories
 */

export const CatchAllStory: Story = () => <CatchAll />;
CatchAllStory.storyName = "Catch All";
