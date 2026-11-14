import { type Story, type StoryDefault } from "@ladle/react";
import { type TableConfig } from "./DataTable";
import { SkeletonTable } from "./SkeletonTable";

export default {
  title: "Data display / Skeleton Table",
} satisfies StoryDefault;

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

export const DefaultStory: Story = () => <SkeletonTable config={config} rowCount={10} />;
DefaultStory.storyName = "Default (10 rows)";

export const FewRowsStory: Story = () => <SkeletonTable config={config} rowCount={3} />;
FewRowsStory.storyName = "Few Rows";

export const ManyRowsStory: Story = () => <SkeletonTable config={config} rowCount={25} />;
ManyRowsStory.storyName = "Many Rows";

export const LargerRowHeightStory: Story = () => <SkeletonTable config={{ ...config, estimatedRowHeight: 96 }} rowCount={5} />;
LargerRowHeightStory.storyName = "Larger Row Height (estimatedRowHeight: 96)";
