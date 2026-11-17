/**
 * Imports
 */

import { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { ConfirmationStatusBadge } from "../../draft-confirmed/ConfirmationStatusBadge";
import { FilterByItems as FilterByItemsComponent } from "./FilterByItems";

/**
 * Meta Information
 */

const meta = {
  title: "Filters / FilterByItems",
} satisfies Meta;

export default meta;

/**
 * Mock Data
 */

const options = [
  { value: "apple", label: "Apple" },
  { value: "cherry", label: "Cherry" },
  { value: "peach", label: "Peach" },
  { value: "raspberry", label: "Raspberry" },
  { value: "strawberry", label: "Strawberry" },
];

/**
 * Stories
 */

export const FilterByItems: StoryObj<typeof meta> = {
  render: () => {
    const [singleValue, setSingleValue] = useState<string | undefined>(undefined);
    const [multipleValue, setMultipleValue] = useState<readonly string[] | undefined>(undefined);

    return (
      <div className="grid grid-cols-2 gap-8">
        <div>
          <pre className="font-semibold">{"mode: 'single'"}</pre>
          <FilterByItemsComponent
            mode="single"
            options={options}
            placeholder="Filter Fruits"
            value={singleValue}
            onChange={setSingleValue}
          />
          <div className="mt-12 border-t pt-4">
            <pre className="font-semibold">Selected Value:</pre>
            <pre className="bg-muted mt-1 px-4 py-2">{singleValue !== undefined ? JSON.stringify(singleValue, null, 2) : "undefined"}</pre>
          </div>
        </div>
        <div>
          <pre className="font-semibold">{"mode: 'multiple'"}</pre>
          <FilterByItemsComponent
            mode="multiple"
            options={options}
            placeholder="Filter Fruits"
            value={multipleValue}
            onChange={setMultipleValue}
          />
          <div className="mt-12 border-t pt-4">
            <pre className="font-semibold">Selected Values:</pre>
            <pre className="bg-muted mt-1 px-4 py-2">
              {multipleValue !== undefined ? JSON.stringify(multipleValue, null, 2) : "undefined"}
            </pre>
          </div>
        </div>
      </div>
    );
  },
};

/**
 * The `Panel` form with a custom component per option, in two columns: short options share a row, the option with the long label
 * stands alone in its row. The color mode of the Storybook toolbar shows Light and Dark.
 */
export const FilterByItemsRenderOption: StoryObj<typeof meta> = {
  render: () => {
    const [singleValue, setSingleValue] = useState<string | undefined>(ConfirmationStatus.Draft);
    const [multipleValue, setMultipleValue] = useState<readonly string[] | undefined>([
      ConfirmationStatus.Draft,
      ConfirmationStatus.Confirmed,
    ]);
    const badgeOptions = [
      ...Object.values(ConfirmationStatus).map((status) => ({
        value: status,
        label: status,
        popoverLabel: <ConfirmationStatusBadge status={status} variant="with-text" />,
      })),
      { value: "InReview", label: "In Prüfung" },
      { value: "Published", label: "Veröffentlicht" },
      { value: "Withdrawn", label: "Zurückgezogen" },
      { value: "OnHold", label: "Pausiert" },
      { value: "Rejected", label: "Abgelehnt" },
      { value: "WaitingForApproval", label: "Wartet auf die Freigabe durch die zuständige Fachabteilung" },
      { value: "Archived", label: "Archiviert" },
    ];

    return (
      <div className="grid grid-cols-2 gap-8">
        <div>
          <pre className="font-semibold">{"mode: 'single'"}</pre>
          <FilterByItemsComponent
            form="Panel"
            mode="single"
            options={badgeOptions}
            placeholder="Status"
            value={singleValue}
            onChange={setSingleValue}
          />
        </div>
        <div>
          <pre className="font-semibold">{"mode: 'multiple'"}</pre>
          <FilterByItemsComponent
            form="Panel"
            mode="multiple"
            options={badgeOptions}
            placeholder="Status"
            value={multipleValue}
            onChange={setMultipleValue}
          />
        </div>
      </div>
    );
  },
};
