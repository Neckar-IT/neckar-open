/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { ErrorIndicator as ErrorIndicatorComponent } from "./ErrorIndicator";

/**
 * Meta Information
 */

export default {
  title: "Feedback / ErrorIndicator",
} satisfies StoryDefault;

/**
 * Args
 */

interface Args {
  textKey: TextKey;
}

/**
 * Stories
 */

export const ErrorIndicator: Story<Args> = (args) => {
  return (
    <div className="grid grid-cols-3 gap-6">
      <div>
        <pre>variant=&quot;small&quot;</pre>
        <div className="flex h-80 items-center justify-center rounded-md border">
          <ErrorIndicatorComponent variant="small" textKey={args.textKey} />
        </div>
      </div>

      <div>
        <pre>variant=&quot;default&quot;</pre>
        <div className="flex h-80 items-center justify-center rounded-md border">
          <ErrorIndicatorComponent variant="default" textKey={args.textKey} />
        </div>
      </div>

      <div>
        <pre>variant=&quot;icon&quot;</pre>
        <div className="flex h-80 items-center justify-center rounded-md border">
          <ErrorIndicatorComponent variant="icon" textKey={args.textKey} />
        </div>
      </div>
    </div>
  );
};
ErrorIndicator.args = {
  textKey: "errorIndicator.label",
};
