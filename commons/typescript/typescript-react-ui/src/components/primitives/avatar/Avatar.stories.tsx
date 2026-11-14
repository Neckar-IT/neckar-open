/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { avatar1 } from "../../../assets/avatars";
import { AvatarFallback, AvatarImage, Avatar as AvatarRoot } from "./Avatar";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Avatar",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Avatar: Story = () => {
  return (
    <div className="flex space-x-8">
      <div>
        <pre className="mb-2 text-sm">valid image:</pre>
        <AvatarRoot>
          <AvatarImage alt="Jane Doe" src={avatar1} />
          <AvatarFallback>JD</AvatarFallback>
        </AvatarRoot>
      </div>

      <div>
        <pre className="mb-2 text-sm">invalid image (fallback):</pre>
        <AvatarRoot>
          <AvatarImage alt="Jane Doe" src="" />
          <AvatarFallback>JD</AvatarFallback>
        </AvatarRoot>
      </div>
    </div>
  );
};
