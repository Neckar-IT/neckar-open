/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { FilterByEntity as FilterByEntityComponent } from "./FilterByEntity";

/**
 * Meta Information
 */

export default {
  title: "Filters / FilterByEntity",
} satisfies StoryDefault;

/**
 * Stories
 */

function SelectDialogStub({ onSelect, onOpenChange }: { onSelect: () => void; onOpenChange: (open: boolean) => void }): React.ReactElement {
  return (
    <div role="dialog" className="bg-background fixed top-1/4 left-1/2 -translate-x-1/2 rounded-md border p-4 shadow-md">
      <button
        type="button"
        onClick={() => {
          onSelect();
          onOpenChange(false);
        }}
      >
        Select Jonas Weber
      </button>
    </div>
  );
}

function Case({ title, initial }: { title: string; initial: React.ReactNode | undefined }): React.ReactElement {
  const [selected, setSelected] = useState<React.ReactNode | undefined>(initial);
  return (
    <div className="w-80">
      <pre className="font-semibold">{title}</pre>
      <FilterByEntityComponent
        form="Panel"
        label="Author"
        active={selected !== undefined}
        selected={selected}
        onClear={() => setSelected(undefined)}
        renderSelectDialog={(onOpenChange) => <SelectDialogStub onSelect={() => setSelected("Jonas Weber")} onOpenChange={onOpenChange} />}
      />
    </div>
  );
}

export const FilterByEntity: Story = () => (
  <div className="flex flex-col gap-8">
    <Case title="no selection" initial={undefined} />
    <Case title="selection" initial="Jonas Weber" />
    <Case title="preview pending" initial="…" />
  </div>
);
