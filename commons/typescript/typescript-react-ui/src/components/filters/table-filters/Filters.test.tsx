import {
  archiveParam,
  type DataSliceParams,
  enumParam,
  enumSetParam,
  instantParam,
  integerParam,
  type ParamDefinition,
  stringParam,
  useDataSliceControllerFromState,
} from "@neckar-hub/typescript-react-utils/data-slice-filters";
import { NowProvider } from "@neckar-hub/typescript-react-utils/providers";
import { ArchiveFilter, ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen, within } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { Filters } from "./Filters";
import {
  archiveFilter,
  confirmationStatusFilter,
  dateTimeRangeFilter,
  entityFilter,
  enumFilter,
  enumSetFilter,
  euroRangeFilter,
  integerRangeFilter,
  keyFilter,
  searchFilter,
} from "./tableFilters";

const OrderStatus = { InPreparation: "InPreparation", Completed: "Completed" } as const;
type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];
const OrderChannel = { Web: "Web", Store: "Store", Phone: "Phone" } as const;
type OrderChannel = (typeof OrderChannel)[keyof typeof OrderChannel];

interface OrderParams extends DataSliceParams<"OrderDate"> {
  archiveFilter?: ArchiveFilter;
  freeText?: string;
  status?: OrderStatus;
  shipmentStatus?: OrderStatus;
  confirmationStatus?: ConfirmationStatus;
  channels?: OrderChannel[];
  orderDateFrom?: string;
  orderDateTo?: string;
  totalPriceMin?: number;
  totalPriceMax?: number;
  totalAmountMin?: number;
  totalAmountMax?: number;
  customerId?: string;
  customerEmail?: string;
}

const orderParams: ParamDefinition<OrderParams> = {
  archiveFilter: archiveParam(),
  freeText: stringParam(),
  status: enumParam(OrderStatus),
  shipmentStatus: enumParam(OrderStatus),
  confirmationStatus: enumParam(ConfirmationStatus),
  channels: enumSetParam(OrderChannel),
  orderDateFrom: instantParam(),
  orderDateTo: instantParam(),
  totalPriceMin: integerParam(),
  totalPriceMax: integerParam(),
  totalAmountMin: integerParam(),
  totalAmountMax: integerParam(),
  customerId: stringParam(),
  customerEmail: stringParam(),
};

interface OrderFilterState {
  customer?: { id: string; name: string } | null;
}

function OrdersFilters({
  initialValues,
  filterState,
}: {
  initialValues?: Partial<OrderParams>;
  filterState?: OrderFilterState;
}): React.ReactElement {
  const controller = useDataSliceControllerFromState<OrderParams>(orderParams, initialValues);
  return (
    <NowProvider value={new Date(2026, 9, 5, 14, 30)}>
      <Filters
        primary={[
          searchFilter(controller, { label: "Search orders" }),
          enumFilter(controller, { param: "status", values: OrderStatus, label: "Status" }),
          enumSetFilter(controller, { param: "channels", values: OrderChannel, label: "Channels" }),
          dateTimeRangeFilter(controller, { from: "orderDateFrom", to: "orderDateTo", label: "Ordered" }),
          integerRangeFilter(controller, { min: "totalAmountMin", max: "totalAmountMax", label: "Amount" }),
        ]}
        archive={archiveFilter(controller)}
        secondary={[
          confirmationStatusFilter(controller),
          enumFilter(controller, { param: "shipmentStatus", values: OrderStatus, label: "Shipment" }),
          euroRangeFilter(controller, { min: "totalPriceMin", max: "totalPriceMax", label: "Total" }),
          entityFilter(controller, filterState, {
            param: "customerId",
            label: "Customer",
            preview: (state: OrderFilterState | undefined) => state?.customer,
            renderPreview: (customer) => customer.name,
            SelectDialog: ({ onSelect, onOpenChange }) => (
              <div role="dialog" aria-label="Select customer">
                <button
                  type="button"
                  onClick={() => {
                    onSelect("c-2");
                    onOpenChange(false);
                  }}
                >
                  Jonas Weber
                </button>
              </div>
            ),
          }),
          keyFilter(controller, { param: "customerEmail", label: "Customer email" }),
        ]}
      />
      <output data-testid="params">{JSON.stringify(controller.params)}</output>
    </NowProvider>
  );
}

function params(): Record<string, unknown> {
  return JSON.parse(screen.getByTestId("params").textContent) as Record<string, unknown>;
}

describe("Filters", () => {
  it("renders every primary filter as a button, the search field as a field and the secondary filters behind one button", () => {
    render(<OrdersFilters />);
    const filters = screen.getByRole("group", { name: "Filters" });

    expect(within(filters).getByRole("textbox", { name: "Search orders" })).toBeInTheDocument();
    for (const name of ["Status", "Channels", "Ordered", "Amount"]) {
      expect(within(filters).getByRole("button", { name })).toBeInTheDocument();
    }
    expect(within(filters).getByRole("button", { name: "Filters" })).toBeInTheDocument();
    for (const name of ["Total", "Customer", "Customer: Select"]) {
      expect(within(filters).queryByRole("button", { name })).not.toBeInTheDocument();
    }
  });

  it("shows a set primary filter with its value and clears it through its X", async () => {
    const user = userEvent.setup();
    render(<OrdersFilters initialValues={{ status: OrderStatus.InPreparation }} />);

    expect(screen.getByRole("button", { name: "Status: InPreparation" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Remove Status" }));

    expect(screen.getByRole("button", { name: "Status" })).toBeInTheDocument();
    expect(params().status).toBeUndefined();
  });

  it("picks an enum value from the list of its button", async () => {
    const user = userEvent.setup();
    render(<OrdersFilters />);

    await user.click(screen.getByRole("button", { name: "Status" }));
    await user.click(screen.getByRole("option", { name: "Completed" }));

    expect(params()).toMatchObject({ status: "Completed" });
  });

  it("picks several enum values from a multi-select list", async () => {
    const user = userEvent.setup();
    render(<OrdersFilters />);

    await user.click(screen.getByRole("button", { name: "Channels" }));
    const list = screen.getByRole("listbox", { name: "Channels" });
    expect(list).toHaveAttribute("aria-multiselectable", "true");
    await user.click(within(list).getByRole("option", { name: "Web" }));
    await user.click(within(list).getByRole("option", { name: "Store" }));

    expect(params()).toMatchObject({ channels: ["Web", "Store"] });
    expect(screen.getByRole("button", { name: "Channels: Web, Store" })).toBeInTheDocument();
  });

  it("sets both bounds of a range through the fields of its popover", async () => {
    const user = userEvent.setup();
    render(<OrdersFilters />);

    await user.click(screen.getByRole("button", { name: "Amount" }));
    const popover = screen.getByRole("dialog");
    await user.type(within(popover).getByRole("textbox", { name: "Amount from" }), "3");
    await user.tab();

    expect(params()).toMatchObject({ totalAmountMin: 3 });
    expect(within(screen.getByRole("dialog")).getByRole("textbox", { name: "Amount until" })).toHaveFocus();
    expect(screen.getByRole("button", { name: "Amount: from 3" })).toBeInTheDocument();
  });

  it("sets a date range from a preset against the injected today", async () => {
    const user = userEvent.setup();
    render(<OrdersFilters />);

    await user.click(screen.getByRole("button", { name: "Ordered" }));
    await user.click(screen.getByRole("button", { name: "Last 7 days" }));

    expect(params()).toMatchObject({
      orderDateFrom: new Date(2026, 8, 29).toISOString(),
      orderDateTo: new Date(2026, 9, 5, 23, 59, 59, 999).toISOString(),
    });
  });

  it("shows the secondary filters in one popover, whose button stays neutral while none is set", async () => {
    const user = userEvent.setup();
    render(<OrdersFilters />);
    const button = screen.getByRole("button", { name: "Filters" });

    expect(button.closest("[data-slot=filter-trigger]")).toHaveAttribute("data-active", "false");

    await user.click(button);
    const popover = screen.getByRole("dialog", { name: "Filters" });

    expect(within(popover).getByRole("heading", { name: "Total" })).toBeInTheDocument();
    expect(within(popover).getByRole("heading", { name: "Customer" })).toBeInTheDocument();
    expect(within(popover).getByRole("textbox", { name: "Total from" })).toBeInTheDocument();
  });

  it("shows a key filter only while a link set it, as its value with an X that clears it", async () => {
    const user = userEvent.setup();
    const { unmount } = render(<OrdersFilters />);
    await user.click(screen.getByRole("button", { name: "Filters" }));
    expect(
      within(screen.getByRole("dialog", { name: "Filters" })).queryByRole("heading", { name: "Customer email" }),
    ).not.toBeInTheDocument();
    unmount();

    render(<OrdersFilters initialValues={{ customerEmail: "anna@example.com" }} />);
    await user.click(screen.getByRole("button", { name: "Filters, 1 active" }));
    const popover = screen.getByRole("dialog", { name: "Filters" });
    expect(within(popover).getByRole("button", { name: "Customer email: anna@example.com" })).toBeInTheDocument();

    await user.click(within(popover).getByRole("button", { name: "Remove Customer email" }));

    expect(params()).not.toHaveProperty("customerEmail");
  });

  it("turns the button of the secondary filters blue and counts the set ones", () => {
    render(<OrdersFilters initialValues={{ totalPriceMin: 1000, customerId: "c-2" }} />);

    const button = screen.getByRole("button", { name: "Filters, 2 active" });

    expect(button.closest("[data-slot=filter-trigger]")).toHaveAttribute("data-active", "true");
    expect(within(button).getByText("2")).toBeInTheDocument();
  });

  it("counts the archive state but not a set primary filter", () => {
    render(<OrdersFilters initialValues={{ status: OrderStatus.Completed, archiveFilter: ArchiveFilter.All, freeText: "Weber" }} />);

    const button = screen.getByRole("button", { name: "Filters, 1 active" });

    expect(button.closest("[data-slot=filter-trigger]")).toHaveAttribute("data-active", "true");
  });

  it("shows the archive filter as a toggle group below the secondary filters, headed by the label in the divider, and writes the picked state", async () => {
    const user = userEvent.setup();
    render(<OrdersFilters />);

    await user.click(screen.getByRole("button", { name: "Filters" }));
    const popover = screen.getByRole("dialog", { name: "Filters" });
    expect(within(popover).getByRole("heading", { name: "Archive" })).toBeInTheDocument();
    expect(within(popover).getByRole("heading", { name: "Archive" }).closest("[data-slot=marker]")).toHaveAttribute(
      "data-variant",
      "separator",
    );
    expect(within(popover).getByRole("radiogroup", { name: "Archive" })).toBeInTheDocument();
    expect(within(popover).getByRole("radio", { name: "Active" })).toHaveAttribute("aria-checked", "true");

    await user.click(within(popover).getByRole("radio", { name: "Archived" }));

    expect(params().archiveFilter).toBe(ArchiveFilter.Archived);
  });

  it("shows the archive heading without divider lines when no secondary filter exists", async () => {
    const user = userEvent.setup();
    function ArchiveOnly(): React.ReactElement {
      const controller = useDataSliceControllerFromState<OrderParams>(orderParams);
      return <Filters primary={[]} archive={archiveFilter(controller)} />;
    }
    render(<ArchiveOnly />);

    await user.click(screen.getByRole("button", { name: "Filters" }));
    const popover = screen.getByRole("dialog", { name: "Filters" });

    expect(within(popover).getByRole("heading", { name: "Archive" })).toBeInTheDocument();
    expect(within(popover).getByRole("heading", { name: "Archive" }).closest("[data-slot=marker]")).toHaveAttribute(
      "data-variant",
      "default",
    );
  });

  it("keeps a secondary filter in the popover once it is set", async () => {
    const user = userEvent.setup();
    render(<OrdersFilters />);

    await user.click(screen.getByRole("button", { name: "Filters" }));
    const popover = screen.getByRole("dialog", { name: "Filters" });
    await user.type(within(popover).getByRole("textbox", { name: "Total from" }), "10");
    await user.tab();

    expect(params()).toMatchObject({ totalPriceMin: 1000 });
    expect(within(popover).getByRole("textbox", { name: "Total from" })).toHaveDisplayValue(/10/);
    expect(screen.getByRole("button", { name: "Filters, 1 active" })).toBeInTheDocument();
  });

  it("selects an entity in its dialog and names it from the echoed filter state", async () => {
    const user = userEvent.setup();
    const { rerender } = render(<OrdersFilters />);

    await user.click(screen.getByRole("button", { name: "Filters" }));
    await user.click(screen.getByRole("button", { name: "Customer: Select" }));
    await user.click(within(screen.getByRole("dialog", { name: "Select customer" })).getByRole("button", { name: "Jonas Weber" }));

    expect(params()).toMatchObject({ customerId: "c-2" });

    rerender(<OrdersFilters filterState={{ customer: { id: "c-2", name: "Jonas Weber" } }} />);

    expect(screen.getByRole("button", { name: "Filters, 1 active" })).toBeInTheDocument();
  });

  it("marks a set euro range and a non-default archive state blue in the popover, and clears the range through its X", async () => {
    const user = userEvent.setup();
    render(<OrdersFilters initialValues={{ totalPriceMin: 1000, archiveFilter: ArchiveFilter.All }} />);

    await user.click(screen.getByRole("button", { name: /^Filters/ }));
    const popover = screen.getByRole("dialog", { name: "Filters" });

    expect(within(popover).getByRole("textbox", { name: "Total from" }).closest("[data-active]")).toHaveAttribute("data-active", "true");
    expect(within(popover).getByRole("radiogroup", { name: "Archive" })).toHaveAttribute("data-active", "true");

    await user.click(within(popover).getByRole("button", { name: "Clear" }));

    expect(params()).not.toHaveProperty("totalPriceMin");
  });

  it("shows the button Reset in the label row of a set enum filter in the popover and clears the filter through it", async () => {
    const user = userEvent.setup();
    render(<OrdersFilters initialValues={{ shipmentStatus: OrderStatus.InPreparation }} />);

    await user.click(screen.getByRole("button", { name: /^Filters/ }));
    const popover = screen.getByRole("dialog", { name: "Filters" });

    await user.click(within(popover).getByRole("button", { name: "Reset" }));

    expect(params()).not.toHaveProperty("shipmentStatus");
    expect(within(popover).queryByRole("button", { name: "Reset" })).not.toBeInTheDocument();
  });

  it("shows no button Reset while the enum filter is unset", async () => {
    const user = userEvent.setup();
    render(<OrdersFilters />);

    await user.click(screen.getByRole("button", { name: /^Filters/ }));

    expect(within(screen.getByRole("dialog", { name: "Filters" })).queryByRole("button", { name: "Reset" })).not.toBeInTheDocument();
  });

  it("shows the button Reset in the label row of a set confirmation status filter", async () => {
    const user = userEvent.setup();
    render(<OrdersFilters initialValues={{ confirmationStatus: ConfirmationStatus.Draft }} />);

    await user.click(screen.getByRole("button", { name: /^Filters/ }));
    await user.click(within(screen.getByRole("dialog", { name: "Filters" })).getByRole("button", { name: "Reset" }));

    expect(params()).not.toHaveProperty("confirmationStatus");
  });
});
