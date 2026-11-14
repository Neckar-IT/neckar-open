/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { EyeIcon, EyeOffIcon } from "lucide-react";
import { useState } from "react";
import { Toggle as ToggleComponent } from "./Toggle";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Toggle",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Toggle: Story = () => {
  const [state, setState] = useState(false);

  return (
    <div className="grid grid-cols-4 gap-16">
      <div>
        <h3 className="font-bold">Variants</h3>
        <div className="mt-2 flex flex-col space-y-4">
          <div className="flex items-center space-x-2">
            <span className="w-20">default:</span>
            <ToggleComponent
              pressed={state}
              variant="default"
              onPressedChange={() => {
                setState((old) => !old);
              }}
            >
              {state ? <EyeIcon className="h-4 w-4" /> : <EyeOffIcon className="h-4 w-4" />}
            </ToggleComponent>
          </div>
          <div className="flex items-center space-x-2">
            <span className="w-20">outline:</span>
            <ToggleComponent
              pressed={state}
              variant="outline"
              onPressedChange={() => {
                setState((old) => !old);
              }}
            >
              {state ? <EyeIcon className="h-4 w-4" /> : <EyeOffIcon className="h-4 w-4" />}
            </ToggleComponent>
          </div>
        </div>
      </div>

      <div>
        <h3 className="font-bold">Sizes</h3>
        <div className="mt-2 flex flex-col space-y-4">
          <div className="flex items-center space-x-2">
            <span className="w-20">sm:</span>
            <ToggleComponent
              pressed={state}
              size="sm"
              variant="outline"
              onPressedChange={() => {
                setState((old) => !old);
              }}
            >
              {state ? <EyeIcon className="h-3 w-3" /> : <EyeOffIcon className="h-4 w-4" />}
            </ToggleComponent>
          </div>
          <div className="flex items-center space-x-2">
            <span className="w-20">default:</span>
            <ToggleComponent
              pressed={state}
              variant="outline"
              onPressedChange={() => {
                setState((old) => !old);
              }}
            >
              {state ? <EyeIcon className="h-4 w-4" /> : <EyeOffIcon className="h-4 w-4" />}
            </ToggleComponent>
          </div>
          <div className="flex items-center space-x-2">
            <span className="w-20">lg:</span>
            <ToggleComponent
              pressed={state}
              size="lg"
              variant="outline"
              onPressedChange={() => {
                setState((old) => !old);
              }}
            >
              {state ? <EyeIcon className="h-4 w-4" /> : <EyeOffIcon className="h-4 w-4" />}
            </ToggleComponent>
          </div>
        </div>
      </div>

      <div>
        <h3 className="font-bold">States</h3>
        <div className="mt-2 flex flex-col space-y-4">
          <div className="flex items-center space-x-2">
            <span className="w-20">disabled:</span>
            <ToggleComponent
              disabled
              pressed={state}
              variant="outline"
              onPressedChange={() => {
                setState((old) => !old);
              }}
            >
              {state ? <EyeIcon className="h-4 w-4" /> : <EyeOffIcon className="h-4 w-4" />}
            </ToggleComponent>
          </div>
        </div>
      </div>
    </div>
  );
};
