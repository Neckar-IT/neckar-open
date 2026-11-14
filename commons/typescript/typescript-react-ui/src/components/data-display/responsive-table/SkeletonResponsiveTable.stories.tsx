import { type Story, type StoryDefault } from "@ladle/react";
import { type ResponsiveTableConfig } from "./ResponsiveTable";
import { SkeletonResponsiveTable } from "./SkeletonResponsiveTable";

export default {
  title: "Data display / Skeleton Responsive Table",
  meta: {
    responsive: true,
  },
} satisfies StoryDefault;

interface Invoice {
  id: string;
  invoice: string;
  paymentStatus: string;
  paymentMethod: string;
  totalAmount: number;
}

const config: ResponsiveTableConfig<Invoice, "invoice" | "totalAmount"> = {
  columns: [
    {
      id: "number",
      header: "No.",
      sortBy: "invoice",
      viewOnMobile: "primary-header",
      cell: (item) => item.invoice,
    },
    {
      id: "paymentStatus",
      header: "Payment Status",
      viewOnMobile: "secondary-header",
      cell: (item) => item.paymentStatus,
    },
    { id: "paymentMethod", header: "Payment Method", cell: (item) => item.paymentMethod },
    { id: "totalAmount", header: "Total Amount", sortBy: "totalAmount", cell: (item) => item.totalAmount },
  ],
};

export const DefaultStory: Story = () => <SkeletonResponsiveTable config={config} rowCount={10} />;
DefaultStory.storyName = "Default (10 rows)";

export const FewRowsStory: Story = () => <SkeletonResponsiveTable config={config} rowCount={3} />;
FewRowsStory.storyName = "Few Rows";

export const ManyRowsStory: Story = () => <SkeletonResponsiveTable config={config} rowCount={25} />;
ManyRowsStory.storyName = "Many Rows";

const configWithoutMobileHeaders: ResponsiveTableConfig<Invoice, "invoice" | "totalAmount"> = {
  columns: [
    { id: "number", header: "No.", cell: (item) => item.invoice },
    { id: "paymentStatus", header: "Payment Status", cell: (item) => item.paymentStatus },
    { id: "paymentMethod", header: "Payment Method", cell: (item) => item.paymentMethod },
    { id: "totalAmount", header: "Total Amount", cell: (item) => item.totalAmount },
  ],
};

export const WithoutMobileHeadersStory: Story = () => <SkeletonResponsiveTable config={configWithoutMobileHeaders} rowCount={5} />;
WithoutMobileHeadersStory.storyName = "Without Mobile Headers";
