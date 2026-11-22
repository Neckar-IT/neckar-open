/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useRef } from "react";
import { DefaultComponentStore } from "./ComponentStore";
import { LifeCycleWrapper } from "./LifeCycleWrapper";
import { TestWrappedComponent } from "./TestWrappedComponent";

const meta = {
  title: "Layout / LifeCycleWrapper",
  component: LifeCycleWrapper,
} satisfies Meta;

export default meta;

export const LifeCycleWrapperStory: StoryObj = {
  name: "LifeCycleWrapper",
  render: () => {
    const componentStoreRef = useRef(new DefaultComponentStore());
    return (
      <div className="bg-amber-200">
        <LifeCycleWrapper
          className="w-[300px] bg-amber-500"
          componentStore={componentStoreRef.current}
          create={() => new TestWrappedComponent()}
        />
      </div>
    );
  },
};
