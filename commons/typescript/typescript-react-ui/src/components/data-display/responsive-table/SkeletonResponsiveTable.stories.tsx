import type { Meta, StoryObj } from "@storybook/react-vite";
import { type ResponsiveTableConfig } from "./ResponsiveTable";
import { SkeletonResponsiveTable } from "./SkeletonResponsiveTable";

const meta = {
  title: "Data display / Skeleton Responsive Table",
  tags: ["responsive"],
} satisfies Meta;

export default meta;

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

export const DefaultStory: StoryObj<typeof meta> = {
  name: "Default (10 rows)",
  render: () => <SkeletonResponsiveTable config={config} rowCount={10} />,
};

export const FewRowsStory: StoryObj<typeof meta> = {
  name: "Few Rows",
  render: () => <SkeletonResponsiveTable config={config} rowCount={3} />,
};

export const ManyRowsStory: StoryObj<typeof meta> = {
  name: "Many Rows",
  render: () => <SkeletonResponsiveTable config={config} rowCount={25} />,
};

const configWithoutMobileHeaders: ResponsiveTableConfig<Invoice, "invoice" | "totalAmount"> = {
  columns: [
    { id: "number", header: "No.", cell: (item) => item.invoice },
    { id: "paymentStatus", header: "Payment Status", cell: (item) => item.paymentStatus },
    { id: "paymentMethod", header: "Payment Method", cell: (item) => item.paymentMethod },
    { id: "totalAmount", header: "Total Amount", cell: (item) => item.totalAmount },
  ],
};

export const WithoutMobileHeadersStory: StoryObj<typeof meta> = {
  name: "Without Mobile Headers",
  render: () => <SkeletonResponsiveTable config={configWithoutMobileHeaders} rowCount={5} />,
};
