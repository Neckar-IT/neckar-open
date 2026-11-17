import type { Meta, StoryObj } from "@storybook/react-vite";
import { EMail } from "./EMail";

const meta = {
  title: "Text / EMail",
} satisfies Meta;

export default meta;

export const EMailStory: StoryObj<typeof meta> = {
  name: "E Mail",
  render: () => {
    return <EMail value="test@email.com" />;
  },
};
