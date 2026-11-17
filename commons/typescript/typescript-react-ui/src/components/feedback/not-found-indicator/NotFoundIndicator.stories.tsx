/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { PlusIcon } from "lucide-react";
import { Button } from "../../primitives/button";
import { NotFoundIndicator as NotFoundIndicatorComponent } from "./NotFoundIndicator";

/**
 * Meta Information
 */

const meta = {
  title: "Feedback / NotFoundIndicator",
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

export const NotFoundIndicatorStory: StoryObj<Args> = {
  name: "NotFoundIndicator",
  args: {
    textKey: "notFoundIndicator.label",
  },
  render: (args: Args) => {
    return (
      <div className="flex flex-col gap-4">
        <div>
          <pre>variant=&quot;default&quot;</pre>
          <div className="h-80">
            <NotFoundIndicatorComponent variant="default" textKey={args.textKey} />
          </div>
        </div>
        <div>
          <pre>variant=&quot;default&quot; (with custom component</pre>
          <div className="h-80">
            <NotFoundIndicatorComponent
              variant="default"
              labelComponent={
                <div className="flex flex-col items-center gap-2">
                  <span className="text-muted-foreground text-sm">Noch keine Daten vorhanden.</span>
                  <Button variant="outline">
                    <PlusIcon />
                    <span>Erstellen</span>
                  </Button>
                </div>
              }
            />
          </div>
        </div>
        <div>
          <pre>variant=&quot;small&quot;</pre>
          <div className="">
            <NotFoundIndicatorComponent variant="small" textKey={args.textKey} />
          </div>
        </div>
        <div>
          <pre>variant=&quot;icon&quot;</pre>
          <div>
            <NotFoundIndicatorComponent variant="icon" textKey={args.textKey} />
          </div>
        </div>
      </div>
    );
  },
};
