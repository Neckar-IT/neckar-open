/**
 * Imports
 */

import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/storybook";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { CatchAll } from "./CatchAll";

/**
 * Meta Information
 */

const meta = {
  title: "Pages / CatchAll",
  decorators: [
    (Story) => (
      <WithMemoryRouter>
        <Story />
      </WithMemoryRouter>
    ),
  ],
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const CatchAllStory: StoryObj<typeof meta> = {
  name: "Catch All",
  render: () => <CatchAll />,
};
