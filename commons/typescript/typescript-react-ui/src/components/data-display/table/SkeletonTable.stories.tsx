import type { Meta, StoryObj } from "@storybook/react-vite";
import { type TableConfig } from "./DataTable";
import { SkeletonTable } from "./SkeletonTable";

const meta = {
  title: "Data display / Skeleton Table",
} satisfies Meta;

export default meta;

interface Invoice {
  id: string;
  invoice: string;
  paymentStatus: string;
  totalAmount: number;
}

const config: TableConfig<Invoice, "invoice" | "totalAmount"> = {
  columns: [
    { id: "number", header: "No.", sortBy: "invoice", cell: (item) => item.invoice },
    { id: "paymentStatus", header: "Payment Status", cell: (item) => item.paymentStatus },
    { id: "totalAmount", header: "Total Amount", sortBy: "totalAmount", cell: (item) => item.totalAmount },
  ],
};

export const DefaultStory: StoryObj<typeof meta> = {
  name: "Default (10 rows)",
  render: () => <SkeletonTable config={config} rowCount={10} />,
};

export const FewRowsStory: StoryObj<typeof meta> = {
  name: "Few Rows",
  render: () => <SkeletonTable config={config} rowCount={3} />,
};

export const ManyRowsStory: StoryObj<typeof meta> = {
  name: "Many Rows",
  render: () => <SkeletonTable config={config} rowCount={25} />,
};

export const LargerRowHeightStory: StoryObj<typeof meta> = {
  name: "Larger Row Height (estimatedRowHeight: 96)",
  render: () => <SkeletonTable config={{ ...config, estimatedRowHeight: 96 }} rowCount={5} />,
};
