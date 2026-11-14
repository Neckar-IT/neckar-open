import { type Story, type StoryDefault } from "@ladle/react";
import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/ladle";
import { CheckCircle2Icon } from "lucide-react";
import { type HeaderContext, type TableConfig, DataTable } from "./DataTable";

/**
 * Meta Information
 */
export default {
  title: "Data display / Table",
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

/**
 * Mock Data
 */

interface Invoice {
  id: string;
  invoice: string;
  paymentStatus: string;
  totalAmount: number;
  paymentMethod: string;
}

const invoices: Invoice[] = [
  {
    id: "ovzpzS2QcR",
    invoice: "INV001",
    paymentStatus: "Paid",
    totalAmount: 250,
    paymentMethod: "Credit Card",
  },
  {
    id: "Hm4oOxWiWK",
    invoice: "INV002",
    paymentStatus: "Pending",
    totalAmount: 150,
    paymentMethod: "PayPal",
  },
  {
    id: "CydzjWA0ha",
    invoice: "INV003",
    paymentStatus: "Unpaid",
    totalAmount: 350,
    paymentMethod: "Bank Transfer",
  },
  {
    id: "oIEwxpAVAE",
    invoice: "INV004",
    paymentStatus: "Paid",
    totalAmount: 450,
    paymentMethod: "Credit Card",
  },
  {
    id: "NEMjAhulph",
    invoice: "INV005",
    paymentStatus: "Paid",
    totalAmount: 550,
    paymentMethod: "PayPal",
  },
  {
    id: "YtL9oJ4395",
    invoice: "INV006",
    paymentStatus: "Pending",
    totalAmount: 200,
    paymentMethod: "Bank Transfer",
  },
  {
    id: "xdp4wIcqJN",
    invoice: "INV007",
    paymentStatus: "Unpaid",
    totalAmount: 300,
    paymentMethod: "Credit Card",
  },
];

/**
 * Stories
 */

export const DefaultStory: Story = () => {
  const config: TableConfig<Invoice, "invoice" | "totalAmount"> = {
    rowLink: (item) => `/invoices/${item.invoice}`,
    columns: [
      {
        id: "number",
        header: "No.",
        sortBy: "invoice",
        cell: (item) => (
          <div className="flex items-center gap-2">
            <CheckCircle2Icon className="h-4 w-4" />
            <span>{item.invoice}</span>
          </div>
        ),
      },
      {
        id: "paymentStatus",
        header: "Payment Status",
        cell: (item) => item.paymentStatus,
      },
      {
        id: "paymentMethod",
        header: "Payment Method",
        cell: (item) => item.paymentMethod,
      },
      {
        id: "totalAmount",
        header: "Total Amount",
        sortBy: "totalAmount",
        sortingIconVariant: "numerical",
        cell: (item) => item.totalAmount,
      },
    ],
  };

  return (
    <DataTable
      config={config}
      data={invoices}
      sortBy="invoice"
      sortOrder="Asc"
      onSortingParamsChange={() => console.log("sorting params changed")}
    />
  );
};
DefaultStory.storyName = "Default";

export const WithRenderFunctionHeaderStory: Story = () => {
  const config: TableConfig<Invoice, "invoice" | "totalAmount"> = {
    rowLink: (item) => `/invoices/${item.invoice}`,
    columns: [
      {
        id: "number",
        header: (context: HeaderContext) => {
          const isActive = context.sorting?.sortBy === context.column.sortBy;
          return <span className={isActive ? "text-primary font-bold" : ""}>No.</span>;
        },
        sortBy: "invoice",
        cell: (item) => item.invoice,
      },
      {
        id: "paymentMethod",
        header: "Payment Method",
        cell: (item) => item.paymentMethod,
      },
      {
        id: "totalAmount",
        header: "Total Amount",
        sortBy: "totalAmount",
        sortingIconVariant: "numerical",
        cell: (item) => item.totalAmount,
      },
    ],
  };

  return (
    <DataTable
      config={config}
      data={invoices}
      sortBy="invoice"
      sortOrder="Asc"
      onSortingParamsChange={() => console.log("sorting params changed")}
    />
  );
};
WithRenderFunctionHeaderStory.storyName = "Render Function Header";

interface Customer {
  id: string;
  email: string;
  orderDate: string;
  note: string;
}

const customers: Customer[] = [
  {
    id: "c1",
    email: "playwright+1777746285416@example.test",
    orderDate: "03.05.2026",
    note: "Erstbestellung",
  },
  {
    id: "c2",
    email: "playwright+1777746285417@example.test",
    orderDate: "02.05.2026",
    note: "Express",
  },
  {
    id: "c3",
    email: "max.mustermann@example.com",
    orderDate: "01.05.2026",
    note: "Stammkunde",
  },
];

/**
 * Demonstrates that body cell content cannot overflow into neighbouring columns even when
 * a single cell holds an unbroken value wider than its column (#1723). The long email
 * column is clipped at its column boundary so the date column to its right stays at its
 * configured position — visually aligned with the date header.
 */
export const LongContentStory: Story = () => {
  const config: TableConfig<Customer, "email" | "orderDate"> = {
    columns: [
      {
        id: "email",
        header: "E-Mail",
        sortBy: "email",
        relativeWidth: 2,
        cell: (item) => item.email,
      },
      {
        id: "orderDate",
        header: "Bestelldatum",
        sortBy: "orderDate",
        relativeWidth: 1,
        cell: (item) => item.orderDate,
      },
      {
        id: "note",
        header: "Notiz",
        relativeWidth: 1,
        cell: (item) => item.note,
      },
    ],
  };

  return (
    <div className="max-w-2xl">
      <DataTable
        config={config}
        data={customers}
        sortBy="orderDate"
        sortOrder="Asc"
        onSortingParamsChange={() => console.log("sorting params changed")}
      />
    </div>
  );
};
LongContentStory.storyName = "Long Content Alignment";

export const WithCustomColumnWidthsStory: Story = () => {
  const config: TableConfig<Invoice, "invoice" | "totalAmount"> = {
    rowLink: (item) => `/invoices/${item.invoice}`,
    columns: [
      {
        id: "number",
        header: "No.",
        sortBy: "invoice",
        relativeWidth: 2,
        cell: (item) => (
          <div className="flex items-center gap-2">
            <CheckCircle2Icon className="h-4 w-4" />
            <span>{item.invoice}</span>
          </div>
        ),
      },
      {
        id: "paymentStatus",
        header: "Payment Status",
        relativeWidth: 1,
        cell: (item) => item.paymentStatus,
      },
      {
        id: "paymentMethod",
        header: "Payment Method",
        cell: (item) => item.paymentMethod,
      },
      {
        id: "totalAmount",
        header: "Total Amount",
        fixedWidth: "150px",
        sortBy: "totalAmount",
        sortingIconVariant: "numerical",
        cell: (item) => item.totalAmount,
      },
    ],
  };

  return (
    <DataTable
      config={config}
      data={invoices}
      sortBy="invoice"
      sortOrder="Asc"
      onSortingParamsChange={() => console.log("sorting params changed")}
    />
  );
};
WithCustomColumnWidthsStory.storyName = "With Custom Widths";
