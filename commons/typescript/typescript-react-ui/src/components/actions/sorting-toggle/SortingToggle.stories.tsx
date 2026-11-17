/**
 * Imports
 */

import { type SortOrder } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { SortingToggle } from "./SortingToggle";

/**
 * Meta Information
 */

const meta = {
  title: "Actions / SortingToggle",
} satisfies Meta;

export default meta;

/**
 * Constants
 */

const sortByName = "name" as const;
const sortByAge = "age" as const;
/**
 * Stories
 */

export const SortingToggleStory: StoryObj<typeof meta> = {
  name: "Sorting Toggle",
  render: () => {
    const [alphabeticalSort, setAlphabeticalSort] = useState<{ sortBy: "name"; sortOrder: SortOrder } | undefined>(undefined);
    const [numericalSort, setNumericalSort] = useState<{ sortBy: "age"; sortOrder: SortOrder } | undefined>(undefined);

    return (
      <div className="flex gap-16">
        <div className="w-80">
          <h2 className="text-lg font-semibold">sort by name</h2>
          <div className="flex flex-col items-start gap-4">
            <SortingToggle
              sortBy={sortByName}
              sortingParams={alphabeticalSort}
              iconVariant="alphabetical"
              onSortingParamsChange={setAlphabeticalSort}
            />
            <pre className="bg-muted rounded-md px-4 py-2">
              value: {alphabeticalSort ? JSON.stringify(alphabeticalSort, null, 2) : "undefined"}
            </pre>
          </div>
        </div>
        <div>
          <h2 className="text-lg font-semibold">numerical (only ascending)</h2>
          <div className="flex flex-col items-start gap-4">
            <SortingToggle
              sortBy={sortByAge}
              sortingParams={numericalSort}
              iconVariant="numerical"
              onSortingParamsChange={setNumericalSort}
            />
            <pre className="bg-muted rounded-md px-4 py-2">
              value: {numericalSort ? JSON.stringify(numericalSort, null, 2) : "undefined"}
            </pre>
          </div>
        </div>
      </div>
    );
  },
};
