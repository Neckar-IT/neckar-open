/**
 * Imports
 */

import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/storybook";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { CatchAll } from "./CatchAll";

const meta = {
  title: "Pages / CatchAll",
  component: CatchAll,
  decorators: [
    (Story) => (
      <WithMemoryRouter>
        <Story />
      </WithMemoryRouter>
    ),
  ],
} satisfies Meta;

export default meta;

export const CatchAllStory: StoryObj = {
  name: "Catch All",
  render: () => <CatchAll />,
};
