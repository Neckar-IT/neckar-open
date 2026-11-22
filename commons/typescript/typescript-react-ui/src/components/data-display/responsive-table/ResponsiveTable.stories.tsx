import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/storybook";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { CheckCircle2Icon } from "lucide-react";
import { TruncateTextContent } from "../../text/text-overflow";
import { type ResponsiveTableConfig, type ResponsiveColumnDefinition, ResponsiveTable } from "./ResponsiveTable";

const meta = {
  title: "Data display / Responsive Table",
  component: ResponsiveTable,
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
    id: "INV001",
    invoice: "INV001",
    paymentStatus: "Paid",
    totalAmount: 250,
    paymentMethod: "Credit Card",
  },
  {
    id: "INV002",
    invoice: "INV002",
    paymentStatus: "Pending",
    totalAmount: 150,
    paymentMethod: "PayPal",
  },
  {
    id: "INV003",
    invoice: "INV003",
    paymentStatus: "Unpaid",
    totalAmount: 350,
    paymentMethod: "Bank Transfer",
  },
  {
    id: "INV004",
    invoice: "INV004",
    paymentStatus: "Paid",
    totalAmount: 450,
    paymentMethod: "Credit Card",
  },
  {
    id: "INV005",
    invoice: "INV005",
    paymentStatus: "Paid",
    totalAmount: 550,
    paymentMethod: "PayPal",
  },
  {
    id: "INV006",
    invoice: "INV006",
    paymentStatus: "Pending",
    totalAmount: 200,
    paymentMethod: "Bank Transfer",
  },
  {
    id: "INV007",
    invoice: "INV007",
    paymentStatus: "Unpaid",
    totalAmount: 300,
    paymentMethod: "Credit Card",
  },
];

const invoiceNoColumn: ResponsiveColumnDefinition<Invoice, "invoice" | "totalAmount"> = {
  id: "number",
  header: "No.",
  sortBy: "invoice",
  cell: (item) => (
    <div className="flex items-center gap-2">
      <CheckCircle2Icon className="h-4 w-4" />
      <span>{item.invoice}</span>
    </div>
  ),
};

export const DefaultStory: StoryObj = {
  name: "Default",
  render: () => {
    const config: ResponsiveTableConfig<Invoice, "invoice" | "totalAmount"> = {
      rowLink: (item) => `/invoices/${item.invoice}`,
      columns: [
        invoiceNoColumn,
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
      <ResponsiveTable
        config={config}
        data={invoices}
        sortBy="invoice"
        sortOrder="Asc"
        onSortingParamsChange={() => console.log("sorting params changed")}
      />
    );
  },
};

export const WithMobileHeaderStory: StoryObj = {
  name: "With Custom Mobile Headers",
  render: () => {
    const config: ResponsiveTableConfig<Invoice, "invoice" | "totalAmount"> = {
      rowLink: (item) => `/invoices/${item.invoice}`,
      columns: [
        invoiceNoColumn,
        {
          id: "paymentStatus",
          header: "Payment Status",
          viewOnMobile: "secondary-header",
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
      <ResponsiveTable
        config={config}
        data={invoices}
        sortBy="invoice"
        sortOrder="Asc"
        onSortingParamsChange={() => console.log("sorting params changed")}
      />
    );
  },
};

/**
 * Long Content (Card View)
 *
 * Forces the CardView by constraining the container below the `@3xl` breakpoint and feeds
 * the table extremely long titles, country names, and detail values wrapped in
 * `data-text-content` spans. Wrapped in `TruncateTextContent` because Storybook does not
 * install the body-level truncation classes that `main.tsx` applies in the real app.
 *
 * Regression coverage for #1924: the card layout must keep the surrounding card width
 * and let `data-text-content` clip with an ellipsis on every slot — primary header,
 * secondary header, and every detail row.
 */
interface Author {
  id: string;
  name: string;
  country: string;
  birthYear: number;
  books: number;
}

const authors: Author[] = [
  {
    id: "AUT001",
    name: "The Distinguished Author of the Acclaimed Multi-Volume History of the Forgotten Kingdoms",
    country: "Demokratyczna Federacyjna Rzeczpospolita Zjednoczonych Prowincji Północnych",
    birthYear: 1879,
    books: 2,
  },
  {
    id: "AUT002",
    name: "El Insigne y Muy Distinguido Cronista de las Tierras Olvidadas y los Reinos Perdidos del Norte",
    country: "Königlich-Vereinigte Föderation der Nordatlantischen Provinzen und der Südpazifischen Inseln",
    birthYear: 1902,
    books: 17,
  },
  {
    id: "AUT003",
    name: "Short Name",
    country: "France",
    birthYear: 1950,
    books: 5,
  },
];

const nameColumn: ResponsiveColumnDefinition<Author> = {
  id: "name",
  header: "Name",
  viewOnMobile: "primary-header",
  cell: (item) => (
    <span data-text-content>
      <span>{item.name}</span>
    </span>
  ),
};

const booksColumn: ResponsiveColumnDefinition<Author> = {
  id: "books",
  header: "Books",
  viewOnMobile: "secondary-header",
  cell: (item) => (
    <span>
      <span>{item.books}</span>
    </span>
  ),
};

const countryColumn: ResponsiveColumnDefinition<Author> = {
  id: "country",
  header: "Country",
  cell: (item) => (
    <span data-text-content>
      <span>{item.country}</span>
    </span>
  ),
};

const birthYearColumn: ResponsiveColumnDefinition<Author> = {
  id: "birthYear",
  header: "Birth Year",
  sortingIconVariant: "numerical",
  cell: (item) => (
    <span data-text-content>
      <span>{item.birthYear}</span>
    </span>
  ),
};

export const LongContentStory: StoryObj = {
  name: "Long Content",
  render: () => {
    const config: ResponsiveTableConfig<Author, "name" | "birthYear"> = {
      rowLink: (item) => `/authors/${item.id}`,
      columns: [nameColumn, booksColumn, countryColumn, birthYearColumn],
    };

    return (
      <TruncateTextContent>
        <div className="border-border max-w-md border">
          <ResponsiveTable
            config={config}
            data={authors}
            sortBy="name"
            sortOrder="Asc"
            onSortingParamsChange={() => console.log("sorting params changed")}
          />
        </div>
      </TruncateTextContent>
    );
  },
};

/**
 * Two-Column Full Width
 *
 * Regression coverage for #2244: a two-column table (Name / Adresse) with short content
 * must fill the full container width at the desktop breakpoint, with both columns sharing
 * the width 50/50. The red dashed frame marks the container edge — the `<table>` must reach
 * the right edge instead of shrinking to its content.
 *
 * The original bug applied the desktop `@3xl:block` toggle to the `<table>` element, which
 * set `display: block` and disabled `table-fixed` + the `<colgroup>` widths, so the table
 * collapsed to content width and left empty space on the right.
 */
interface Place {
  id: string;
  name: string;
  address: string;
}

const places: Place[] = [
  { id: "PLC001", name: "Musterhaus", address: "Musterstr. 12, 12345 Musterstadt" },
  { id: "PLC002", name: "Beispielhof", address: "Beispielweg 4, 54321 Beispielstadt" },
];

export const TwoColumnFullWidthStory: StoryObj = {
  name: "Two-Column Full Width",
  render: () => {
    const config: ResponsiveTableConfig<Place> = {
      rowLink: (item) => `/places/${item.id}`,
      columns: [
        { id: "name", header: "Name", viewOnMobile: "primary-header", cell: (item) => item.name },
        { id: "address", header: "Adresse", cell: (item) => item.address },
      ],
    };

    return (
      <div className="w-full border-2 border-dashed border-red-500">
        <ResponsiveTable
          config={config}
          data={places}
          sortBy={undefined}
          sortOrder="Asc"
          onSortingParamsChange={() => console.log("sorting params changed")}
        />
      </div>
    );
  },
};
