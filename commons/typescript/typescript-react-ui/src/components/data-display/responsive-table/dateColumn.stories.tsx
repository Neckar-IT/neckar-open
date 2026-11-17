import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/storybook";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { dateColumn, dateTimeColumn } from "./dateColumn";
import { ResponsiveTable } from "./ResponsiveTable";

const meta = {
  title: "Data display / Responsive Table",
  decorators: [
    (Story) => (
      <WithMemoryRouter>
        <Story />
      </WithMemoryRouter>
    ),
  ],
  tags: ["responsive"],
} satisfies Meta;

export default meta;

interface Row {
  id: string;
  title: string;
  releaseDate: string;
  createdAt: string;
}

const rows: Row[] = [
  { id: "r1", title: "First", releaseDate: "2026-04-01", createdAt: "2026-04-01T08:30:00Z" },
  { id: "r2", title: "Second", releaseDate: "2026-05-15", createdAt: "2026-05-15T14:00:00Z" },
  { id: "r3", title: "Third", releaseDate: "2026-06-22", createdAt: "2026-06-22T19:45:00Z" },
];

export const DateColumns: StoryObj<typeof meta> = {
  render: () => (
    <ResponsiveTable
      data={rows}
      config={{
        columns: [
          {
            id: "title",
            header: "Title",
            cell: (row) => row.title,
          },
          dateColumn<Row, never>({
            id: "releaseDate",
            header: "Release (LocalDate)",
            accessor: (row) => row.releaseDate,
          }),
          dateTimeColumn<Row, never>({
            id: "createdAt",
            header: "Created (Instant)",
            accessor: (row) => row.createdAt,
          }),
        ],
      }}
    />
  ),
};
