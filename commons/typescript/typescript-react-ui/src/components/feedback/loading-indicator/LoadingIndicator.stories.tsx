/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { LoadingIndicator as LoadingIndicatorComponent } from "./LoadingIndicator";

/**
 * Meta Information
 */

const meta = {
  title: "Feedback / LoadingIndicator",
} satisfies Meta;

export default meta;

/**
 * Args
 */

interface Args {
  textKey: TextKey;
}

/**
 * Stories
 */

export const LoadingIndicator: StoryObj<Args> = {
  args: {
    textKey: "loadingIndicator.label",
  },
  render: (args: Args) => {
    return (
      <div className="grid grid-cols-3 gap-6">
        <div>
          <pre>variant=&quot;small&quot;</pre>
          <div className="flex h-80 items-center justify-center rounded-md border">
            <LoadingIndicatorComponent variant="small" textKey={args.textKey} />
          </div>
        </div>

        <div>
          <pre>variant=&quot;default&quot;</pre>
          <div className="h-80 rounded-md border">
            <LoadingIndicatorComponent variant="default" textKey={args.textKey} />
          </div>
        </div>

        <div>
          <pre>variant=&quot;icon&quot;</pre>
          <div className="flex h-80 items-center justify-center rounded-md border">
            <LoadingIndicatorComponent variant="icon" textKey={args.textKey} />
          </div>
        </div>
      </div>
    );
  },
};
