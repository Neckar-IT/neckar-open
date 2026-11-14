/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useRef } from "react";
import { DefaultComponentStore } from "./ComponentStore";
import { LifeCycleWrapper } from "./LifeCycleWrapper";
import { TestWrappedComponent } from "./TestWrappedComponent";

/**
 * Meta Information
 */

export default {
  title: "Layout / LifeCycleWrapper",
} satisfies StoryDefault;

/**
 * Stories
 */
export const LifeCycleWrapperStory: Story = () => {
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
};
LifeCycleWrapperStory.storyName = "LifeCycleWrapper";
