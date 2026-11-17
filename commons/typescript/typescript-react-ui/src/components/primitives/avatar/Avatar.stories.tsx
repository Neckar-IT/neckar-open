/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { avatar1 } from "../../../assets/avatars";
import { AvatarFallback, AvatarImage, Avatar as AvatarRoot } from "./Avatar";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Avatar",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Avatar: StoryObj<typeof meta> = {
  render: () => {
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
  },
};
