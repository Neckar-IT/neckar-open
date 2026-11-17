import type { SortingParams } from "@neckar-hub/typescript-react-utils/search-params";
import { SortOrder } from "@neckar-hub/typescript-utils/openapi-types";
import { ChevronDownIcon, EllipsisIcon } from "lucide-react";
import { useState } from "react";
import { DataTable, type ColumnDefinition, type TableConfig } from "../components/data-display/table";
import { Pagination } from "../components/navigation/pagination";
import { Button } from "../components/primitives/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../components/primitives/card";
import { Checkbox } from "../components/primitives/checkbox";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "../components/primitives/dropdown-menu";
import { Input } from "../components/primitives/input";

interface Payment {
  id: string;
  amount: number;
  status: "pending" | "processing" | "success" | "failed";
  email: string;
}

type PaymentSortCriteria = "email";

const data: Payment[] = [
  {
    id: "m5gr84i9",
    amount: 316,
    status: "success",
    email: "ken99@yahoo.com",
  },
  {
    id: "3u1reuv4",
    amount: 242,
    status: "success",
    email: "Abe45@gmail.com",
  },
  {
    id: "derv1ws0",
    amount: 837,
    status: "processing",
    email: "Monserrat44@gmail.com",
  },
  {
    id: "5kma53ae",
    amount: 874,
    status: "success",
    email: "Silas22@gmail.com",
  },
  {
    id: "bhqecj4p",
    amount: 721,
    status: "failed",
    email: "carmella@hotmail.com",
  },
];

const currencyFormat = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
});

/** Columns the visibility dropdown must not hide — they carry controls, not data. */
const alwaysVisibleColumnIds = new Set(["select", "actions"]);

const defaultPageSize = 3;
const pageSizeOptions = [defaultPageSize, 5, 10];

export function CardsDataTable() {
  const [emailFilter, setEmailFilter] = useState("");
  const [sorting, setSorting] = useState<SortingParams<PaymentSortCriteria> | undefined>(undefined);
  const [hiddenColumnIds, setHiddenColumnIds] = useState<string[]>([]);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(defaultPageSize);

  const filtered = data.filter((payment) => payment.email.toLowerCase().includes(emailFilter.toLowerCase()));
  const sorted = sortPayments(filtered, sorting);

  const pageCount = Math.max(1, Math.ceil(sorted.length / pageSize));
  // The filter can shrink the result below the current page, which would leave an empty table.
  const currentPageIndex = Math.min(pageIndex, pageCount - 1);
  const pageItems = sorted.slice(currentPageIndex * pageSize, currentPageIndex * pageSize + pageSize);

  // Counted within the filtered set, so the filter cannot leave more rows selected than are visible.
  const selectedFilteredCount = sorted.filter((payment) => selectedIds.includes(payment.id)).length;

  const allOnPageSelected = pageItems.length > 0 && pageItems.every((payment) => selectedIds.includes(payment.id));
  const someOnPageSelected = pageItems.some((payment) => selectedIds.includes(payment.id));

  const toggleAllOnPage = (selected: boolean) => {
    const idsOnPage = pageItems.map((payment) => payment.id);
    setSelectedIds(selected ? [...new Set([...selectedIds, ...idsOnPage])] : selectedIds.filter((id) => !idsOnPage.includes(id)));
  };

  const toggleRow = (payment: Payment, selected: boolean) => {
    setSelectedIds(selected ? [...selectedIds, payment.id] : selectedIds.filter((id) => id !== payment.id));
  };

  const columns: ColumnDefinition<Payment, PaymentSortCriteria>[] = [
    {
      id: "select",
      fixedWidth: "48px",
      header: (
        <Checkbox
          aria-label="Select all"
          checked={allOnPageSelected || (someOnPageSelected && "indeterminate")}
          onCheckedChange={(value) => {
            toggleAllOnPage(value !== "indeterminate" ? value : false);
          }}
        />
      ),
      cell: (payment) => (
        <Checkbox
          aria-label="Select row"
          checked={selectedIds.includes(payment.id)}
          onCheckedChange={(value) => {
            toggleRow(payment, value !== "indeterminate" ? value : false);
          }}
        />
      ),
    },
    {
      id: "status",
      header: "Status",
      cell: (payment) => <div className="capitalize">{payment.status}</div>,
    },
    {
      id: "email",
      header: "Email",
      sortBy: "email",
      relativeWidth: 2,
      cell: (payment) => <div className="lowercase">{payment.email}</div>,
    },
    {
      id: "amount",
      header: "Amount",
      headerClassName: "justify-end",
      cellClassName: "text-right font-medium",
      cell: (payment) => currencyFormat.format(payment.amount),
    },
    {
      id: "actions",
      fixedWidth: "56px",
      cell: (payment) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button className="h-8 w-8 p-0" variant="ghost">
              <span className="sr-only">Open menu</span>
              <EllipsisIcon className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>Actions</DropdownMenuLabel>
            <DropdownMenuItem onClick={() => void window.navigator.clipboard.writeText(payment.id)}>Copy payment ID</DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem>View customer</DropdownMenuItem>
            <DropdownMenuItem>View payment details</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    },
  ];

  const config: TableConfig<Payment, PaymentSortCriteria> = {
    columns: columns.filter((column) => !hiddenColumnIds.includes(column.id)),
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Payments</CardTitle>
        <CardDescription>Manage your payments.</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="mb-4 flex items-center gap-4">
          <Input
            className="max-w-sm"
            placeholder="Filter emails..."
            value={emailFilter}
            onChange={(event) => {
              setEmailFilter(event.target.value);
              setPageIndex(0);
            }}
          />
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button className="ml-auto" variant="outline">
                Columns <ChevronDownIcon className="ml-2 h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              {columns
                .filter((column) => !alwaysVisibleColumnIds.has(column.id))
                .map((column) => (
                  <DropdownMenuCheckboxItem
                    key={column.id}
                    checked={!hiddenColumnIds.includes(column.id)}
                    className="capitalize"
                    onCheckedChange={(visible) => {
                      setHiddenColumnIds(visible ? hiddenColumnIds.filter((id) => id !== column.id) : [...hiddenColumnIds, column.id]);
                    }}
                  >
                    {column.id}
                  </DropdownMenuCheckboxItem>
                ))}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
        <div className="rounded-md border">
          <DataTable
            config={config}
            data={pageItems}
            sortBy={sorting?.sortBy}
            sortOrder={sorting?.sortOrder}
            onSortingParamsChange={setSorting}
          />
        </div>
        <div className="flex flex-col gap-2 pt-4">
          <div className="text-muted-foreground text-sm">
            {selectedFilteredCount} of {sorted.length} row(s) selected.
          </div>
          <Pagination
            className="self-end"
            pageCount={pageCount}
            pageIndex={currentPageIndex}
            pageSize={pageSize}
            pageSizeOptions={pageSizeOptions}
            onChange={(newPageIndex, newPageSize) => {
              setPageIndex(newPageIndex);
              setPageSize(newPageSize);
            }}
          />
        </div>
      </CardContent>
    </Card>
  );
}

/**
 * Sorts by the single sortable column of this preview; without sorting params the input order is kept.
 */
function sortPayments(payments: Payment[], sorting: SortingParams<PaymentSortCriteria> | undefined): Payment[] {
  if (sorting === undefined) {
    return payments;
  }

  const direction = sorting.sortOrder === SortOrder.Asc ? 1 : -1;
  return payments.toSorted((first, second) => direction * first.email.localeCompare(second.email));
}
