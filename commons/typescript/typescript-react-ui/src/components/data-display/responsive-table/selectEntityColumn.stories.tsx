import { type Story, type StoryDefault } from "@ladle/react";
import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/ladle";
import { useState } from "react";
import { ResponsiveTable } from "./ResponsiveTable";
import { selectEntityColumn } from "./selectEntityColumn";

export default {
  title: "Data display / Responsive Table",
  decorators: [
    (Story) => (
      <WithMemoryRouter>
        <Story />
      </WithMemoryRouter>
    ),
  ],
  meta: {
    responsive: true,
  },
} satisfies StoryDefault;

interface Row {
  id: string;
  title: string;
}

const rows: Row[] = [
  { id: "r1", title: "First" },
  { id: "r2", title: "Second" },
  { id: "r3", title: "Third" },
];

export const SelectEntityColumnStory: Story = () => {
  const [selectedRow, setSelectedRow] = useState<Row | undefined>(rows[1]);

  return (
    <div className="flex flex-col gap-4">
      <ResponsiveTable
        data={rows}
        config={{
          columns: [
            {
              id: "title",
              header: "Title",
              cell: (row) => row.title,
              viewOnMobile: "primary-header",
            },
            selectEntityColumn<Row, never>({
              isSelected: (row) => row.id === selectedRow?.id,
              onSelect: setSelectedRow,
              selectLabel: "Select",
            }),
          ],
        }}
      />
      <output className="text-muted-foreground text-sm">Selected: {selectedRow?.title ?? "none"}</output>
    </div>
  );
};
