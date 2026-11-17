import { ArchiveFilter } from "@neckar-hub/typescript-utils/openapi-types";
import { act, renderHook } from "@testing-library/react";
import { NuqsTestingAdapter, type UrlUpdateEvent } from "nuqs/adapters/testing";
import type { PropsWithChildren, ReactElement } from "react";
import { describe, expect, it, vi } from "vitest";
import type { DataSliceParams } from "./DataSliceController";
import {
  archiveParam,
  booleanParam,
  enumParam,
  enumSetParam,
  floatParam,
  instantParam,
  integerParam,
  localDateParam,
  type ParamDefinition,
  stringParam,
} from "./paramCodecs";
import { useDataSliceControllerFromState } from "./useDataSliceControllerFromState";
import { useDataSliceControllerFromUrl } from "./useDataSliceControllerFromUrl";

const OrderStatus = { Open: "Open", Shipped: "Shipped" } as const;
type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];

const OrderSortCriteria = { OrderDate: "OrderDate", Customer: "Customer" } as const;
type OrderSortCriteria = (typeof OrderSortCriteria)[keyof typeof OrderSortCriteria];

interface OrderParams extends DataSliceParams<OrderSortCriteria> {
  archiveFilter?: ArchiveFilter;
  freeText?: string;
  customerName?: string;
  status?: OrderStatus;
  statuses?: OrderStatus[];
  hanging?: boolean;
  orderDateFrom?: string;
  orderDateTo?: string;
  deliveryDateFrom?: string;
  deliveryDateTo?: string;
  amountMin?: number;
  amountMax?: number;
  powerMin?: number;
}

const orderParams: ParamDefinition<OrderParams> = {
  archiveFilter: archiveParam(),
  freeText: stringParam(),
  customerName: stringParam(),
  status: enumParam(OrderStatus),
  statuses: enumSetParam(OrderStatus),
  hanging: booleanParam(),
  orderDateFrom: instantParam(),
  orderDateTo: instantParam(),
  deliveryDateFrom: localDateParam(),
  deliveryDateTo: localDateParam(),
  amountMin: integerParam(),
  amountMax: integerParam(),
  powerMin: floatParam(),
};

function makeWrapper(searchParams: string, onUrlUpdate: (event: UrlUpdateEvent) => void = () => {}) {
  return function Wrapper({ children }: PropsWithChildren): ReactElement {
    return (
      <NuqsTestingAdapter hasMemory searchParams={searchParams} onUrlUpdate={onUrlUpdate}>
        {children}
      </NuqsTestingAdapter>
    );
  };
}

describe("useDataSliceControllerFromUrl", () => {
  it("reads every parameter from its prefixed URL key", () => {
    const { result } = renderHook(() => useDataSliceControllerFromUrl<OrderParams>(orderParams, OrderSortCriteria, "orders"), {
      wrapper: makeWrapper(
        "?ordersFreeText=Hannah%20Kr%C3%A4mer&ordersArchiveFilter=All&ordersStatus=Open&ordersStatuses=Shipped,Open&ordersHanging=true" +
          "&ordersOrderDateFrom=2026-09-01T00:00:00.000Z&ordersDeliveryDateTo=2026-10-09&ordersAmountMin=3&ordersPowerMin=4.5" +
          "&ordersSortBy=Customer&ordersSortOrder=Asc&ordersPageIndex=2&freeText=ignored",
      ),
    });

    expect(result.current.params).toEqual({
      pageIndex: 2,
      pageSize: 10,
      sortBy: OrderSortCriteria.Customer,
      sortOrder: "Asc",
      archiveFilter: ArchiveFilter.All,
      freeText: "Hannah Krämer",
      customerName: undefined,
      status: OrderStatus.Open,
      statuses: [OrderStatus.Open, OrderStatus.Shipped],
      hanging: true,
      orderDateFrom: "2026-09-01T00:00:00.000Z",
      orderDateTo: undefined,
      deliveryDateFrom: undefined,
      deliveryDateTo: "2026-10-09",
      amountMin: 3,
      amountMax: undefined,
      powerMin: 4.5,
    });
    expect(result.current.freeTextTokens).toEqual(["Hannah", "Krämer"]);
  });

  it("starts on the archive state OnlyActive and on the given page size while the URL names none", () => {
    const { result } = renderHook(() => useDataSliceControllerFromUrl<OrderParams>(orderParams, OrderSortCriteria, undefined, 20), {
      wrapper: makeWrapper(""),
    });

    expect(result.current.params).toMatchObject({ pageIndex: 0, pageSize: 20, archiveFilter: ArchiveFilter.OnlyActive });
    expect(result.current.freeTextTokens).toBeUndefined();
  });

  it("writes several parameters in one URL update and goes back to the first page", async () => {
    const onUrlUpdate = vi.fn<(event: UrlUpdateEvent) => void>();
    const { result } = renderHook(() => useDataSliceControllerFromUrl<OrderParams>(orderParams, OrderSortCriteria, "orders"), {
      wrapper: makeWrapper("?ordersPageIndex=3&ordersAmountMin=1", onUrlUpdate),
    });

    await act(async () => {
      result.current.setParams({ deliveryDateFrom: "2026-09-01", deliveryDateTo: "2026-09-30" });
    });

    await vi.waitFor(() => expect(onUrlUpdate).toHaveBeenCalled());
    expect(onUrlUpdate).toHaveBeenCalledTimes(1);
    expect(onUrlUpdate.mock.calls[0]?.[0].queryString).toBe(
      "?ordersAmountMin=1&ordersDeliveryDateFrom=2026-09-01&ordersDeliveryDateTo=2026-09-30",
    );
    expect(result.current.params).toMatchObject({ deliveryDateFrom: "2026-09-01", deliveryDateTo: "2026-09-30", pageIndex: 0 });
  });

  it("removes a parameter at its default value from the URL", async () => {
    const onUrlUpdate = vi.fn<(event: UrlUpdateEvent) => void>();
    const { result } = renderHook(() => useDataSliceControllerFromUrl<OrderParams>(orderParams, OrderSortCriteria), {
      wrapper: makeWrapper("?archiveFilter=Archived&statuses=Open&hanging=true&freeText=Weber", onUrlUpdate),
    });

    await act(async () => {
      result.current.setParams({ archiveFilter: ArchiveFilter.OnlyActive, statuses: [], hanging: false, freeText: "" });
    });

    await vi.waitFor(() => expect(onUrlUpdate).toHaveBeenCalled());
    expect(onUrlUpdate.mock.lastCall?.[0].queryString).toBe("");
  });

  it("drops URL values a codec cannot read", () => {
    const { result } = renderHook(() => useDataSliceControllerFromUrl<OrderParams>(orderParams, OrderSortCriteria), {
      wrapper: makeWrapper(
        "?status=Cancelled&archiveFilter=Some&amountMin=1.5&amountMax=abc&deliveryDateFrom=01.10.2026&orderDateFrom=yesterday",
      ),
    });

    expect(result.current.params).toMatchObject({
      status: undefined,
      archiveFilter: ArchiveFilter.OnlyActive,
      amountMin: undefined,
      amountMax: undefined,
      deliveryDateFrom: undefined,
      orderDateFrom: undefined,
    });
  });

  it("refuses a parameter outside the definition", async () => {
    const { result } = renderHook(() => useDataSliceControllerFromUrl<OrderParams>(orderParams, OrderSortCriteria), {
      wrapper: makeWrapper(""),
    });

    expect(() => result.current.setParams({ unknownParam: "x" } as Partial<OrderParams>)).toThrow(
      'No codec for the parameter "unknownParam"',
    );
  });
});

describe("useDataSliceControllerFromState", () => {
  it("holds the same parameters the URL controller reads", async () => {
    const initialValues = { archiveFilter: ArchiveFilter.All, amountMin: 2, amountMax: 5 };
    const fromState = renderHook(() => useDataSliceControllerFromState<OrderParams>(orderParams, initialValues));
    const fromUrl = renderHook(() => useDataSliceControllerFromUrl<OrderParams>(orderParams, OrderSortCriteria), {
      wrapper: makeWrapper("?archiveFilter=All&amountMin=2&amountMax=5"),
    });

    expect(fromState.result.current.params).toEqual(fromUrl.result.current.params);

    await act(async () => {
      fromState.result.current.setParams({ orderDateFrom: "2026-09-01T00:00:00.000Z", status: OrderStatus.Shipped });
      fromUrl.result.current.setParams({ orderDateFrom: "2026-09-01T00:00:00.000Z", status: OrderStatus.Shipped });
    });

    expect(fromState.result.current.params).toEqual(fromUrl.result.current.params);
  });

  it("goes back to the first page on every change", async () => {
    const { result } = renderHook(() => useDataSliceControllerFromState<OrderParams>(orderParams));

    await act(async () => {
      result.current.setPagination({ pageIndex: 3, pageSize: 10 });
    });
    await act(async () => {
      result.current.setParam("freeText", "Weber");
    });
    expect(result.current.params).toMatchObject({ pageIndex: 0, freeText: "Weber" });
    expect(result.current.freeTextTokens).toEqual(["Weber"]);
  });

  it("keeps the other parameters when several change one after the other", async () => {
    const { result } = renderHook(() => useDataSliceControllerFromState<OrderParams>(orderParams));

    await act(async () => {
      result.current.setParam("status", OrderStatus.Open);
      result.current.setParam("customerName", "Weber");
    });

    expect(result.current.params).toMatchObject({ status: OrderStatus.Open, customerName: "Weber" });
  });

  it("refuses a parameter outside the definition", () => {
    const { result } = renderHook(() => useDataSliceControllerFromState<OrderParams>(orderParams));

    expect(() => result.current.setParams({ unknownParam: "x" } as Partial<OrderParams>)).toThrow(
      'No codec for the parameter "unknownParam"',
    );
  });
});
