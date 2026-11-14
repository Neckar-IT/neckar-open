import type { Story, StoryDefault } from "@ladle/react";
import { EMail } from "./EMail";

const meta: StoryDefault = {
  title: "Text / EMail",
};

export default meta;

export const EMailStory: Story = () => {
  return <EMail value="test@email.com" />;
};
EMailStory.storyName = "E Mail";
