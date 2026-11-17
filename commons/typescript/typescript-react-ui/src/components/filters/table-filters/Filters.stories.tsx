import {
  archiveParam,
  type DataSliceParams,
  enumParam,
  instantParam,
  integerParam,
  localDateParam,
  type ParamDefinition,
  stringParam,
  useDataSliceControllerFromState,
} from "@neckar-hub/typescript-react-utils/data-slice-filters";
import { NowProvider } from "@neckar-hub/typescript-react-utils/providers";
import { ArchiveFilter, ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { DataSliceToolbar, DataSliceView } from "../../layout/data-slice-view";
import { Button } from "../../primitives/button";
import { Filters } from "./Filters";
import {
  archiveFilter,
  confirmationStatusFilter,
  dateRangeFilter,
  dateTimeRangeFilter,
  enumFilter,
  euroRangeFilter,
  integerRangeFilter,
  searchFilter,
  textFilter,
} from "./tableFilters";

const meta = {
  title: "Filters / Filters",
  tags: ["responsive"],
} satisfies Meta;

export default meta;

const OrderStatus = { InPreparation: "InPreparation", Completed: "Completed" } as const;
type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];

interface OrderParams extends DataSliceParams<"OrderDate"> {
  freeText?: string;
  status?: OrderStatus;
  confirmationStatus?: ConfirmationStatus;
  orderDateFrom?: string;
  orderDateTo?: string;
  archiveFilter?: ArchiveFilter;
  deliveryDateFrom?: string;
  deliveryDateTo?: string;
  totalAmountMin?: number;
  totalAmountMax?: number;
  totalPriceMin?: number;
  totalPriceMax?: number;
  customerName?: string;
}

const orderParams: ParamDefinition<OrderParams> = {
  freeText: stringParam(),
  status: enumParam(OrderStatus),
  confirmationStatus: enumParam(ConfirmationStatus),
  orderDateFrom: instantParam(),
  orderDateTo: instantParam(),
  archiveFilter: archiveParam(),
  deliveryDateFrom: localDateParam(),
  deliveryDateTo: localDateParam(),
  totalAmountMin: integerParam(),
  totalAmountMax: integerParam(),
  totalPriceMin: integerParam(),
  totalPriceMax: integerParam(),
  customerName: stringParam(),
};

/** A search field, three primary filters and the archive filter, modelled on the book orders; the other filters sit behind "Filter". */
function FilteredOrders({ initialValues }: { initialValues?: Partial<OrderParams> }): React.ReactElement {
  const controller = useDataSliceControllerFromState<OrderParams>(orderParams, initialValues);

  return (
    <NowProvider value={new Date(2026, 9, 5, 9, 30)}>
      <DataSliceView
        toolbar={
          <DataSliceToolbar
            filters={
              <Filters
                primary={[
                  searchFilter(controller, { label: "Bestellungen suchen" }),
                  enumFilter(controller, { param: "status", values: OrderStatus, label: "Status" }),
                  confirmationStatusFilter(controller),
                  dateTimeRangeFilter(controller, { from: "orderDateFrom", to: "orderDateTo", label: "Bestellt" }),
                ]}
                secondary={[
                  textFilter(controller, { param: "customerName", label: "Kunde" }),
                  dateRangeFilter(controller, { from: "deliveryDateFrom", to: "deliveryDateTo", label: "Liefertermin" }),
                  integerRangeFilter(controller, { min: "totalAmountMin", max: "totalAmountMax", label: "Anzahl Bücher" }),
                  euroRangeFilter(controller, { min: "totalPriceMin", max: "totalPriceMax", label: "Summe" }),
                ]}
                archive={archiveFilter(controller)}
              />
            }
            actions={<Button>Bestellung anlegen</Button>}
          />
        }
        paginationInfo={{ pageIndex: 0, pageSize: 10, pageCount: 2, filteredElementsCount: 12, totalElementsCount: 41 }}
        onPaginationParamsChange={() => {}}
      >
        <div className="flex h-40 items-center justify-center">
          <p>Bestellungen</p>
        </div>
      </DataSliceView>
    </NowProvider>
  );
}

export const WithoutValue: StoryObj<typeof meta> = {
  render: () => <FilteredOrders />,
};

export const WithActivePrimaryFilters: StoryObj<typeof meta> = {
  render: () => (
    <FilteredOrders
      initialValues={{
        freeText: "Krämer",
        status: OrderStatus.InPreparation,
        confirmationStatus: ConfirmationStatus.Confirmed,
        orderDateFrom: new Date(2026, 8, 1).toISOString(),
        orderDateTo: new Date(2026, 8, 30, 23, 59, 59, 999).toISOString(),
        archiveFilter: ArchiveFilter.All,
      }}
    />
  ),
};

export const WithActiveSecondaryFilters: StoryObj<typeof meta> = {
  render: () => (
    <FilteredOrders initialValues={{ deliveryDateFrom: "2026-10-01", totalAmountMin: 3, totalAmountMax: 12, totalPriceMax: 25000 }} />
  ),
};
