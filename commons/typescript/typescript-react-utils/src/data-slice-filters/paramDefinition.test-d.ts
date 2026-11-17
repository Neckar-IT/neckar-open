/**
 * Compiled by `tsc` in the module build: an `@ts-expect-error` without an error fails the build.
 */
import type { ArchiveFilter } from "@neckar-hub/typescript-utils/openapi-types";
import { expectTypeOf } from "vitest";
import type { DataSliceController, DataSliceParams } from "./DataSliceController";
import { archiveParam, enumParam, integerParam, noParams, type ParamDefinition, stringParam } from "./paramCodecs";
import { useDataSliceControllerFromUrl } from "./useDataSliceControllerFromUrl";

const OrderStatus = { Open: "Open", Shipped: "Shipped" } as const;
type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];

const OrderSortCriteria = { OrderDate: "OrderDate" } as const;
type OrderSortCriteria = (typeof OrderSortCriteria)[keyof typeof OrderSortCriteria];

interface OrderParams extends DataSliceParams<OrderSortCriteria> {
  archiveFilter?: ArchiveFilter;
  freeText?: string;
  status?: OrderStatus;
  amountMin?: number;
}

export const complete: ParamDefinition<OrderParams> = {
  archiveFilter: archiveParam(),
  freeText: stringParam(),
  status: enumParam(OrderStatus),
  amountMin: integerParam(),
};

// @ts-expect-error -- "amountMin" has no codec
export const missing: ParamDefinition<OrderParams> = {
  archiveFilter: archiveParam(),
  freeText: stringParam(),
  status: enumParam(OrderStatus),
};

export const excess: ParamDefinition<OrderParams> = {
  ...complete,
  // @ts-expect-error -- "stat" is no parameter of OrderParams
  stat: stringParam(),
};

export const wrongValueType: ParamDefinition<OrderParams> = {
  ...complete,
  // @ts-expect-error -- "amountMin" is a number, not a string
  amountMin: stringParam(),
};

export const wrongEnum: ParamDefinition<OrderParams> = {
  ...complete,
  // @ts-expect-error -- "status" takes the values of OrderStatus
  status: enumParam({ Draft: "Draft" } as const),
};

export const noPaginationOrSorting: ParamDefinition<OrderParams> = complete;

export function useOrderController(): DataSliceController<OrderParams, OrderSortCriteria> {
  const controller = useDataSliceControllerFromUrl<OrderParams>(complete, OrderSortCriteria);
  expectTypeOf(controller.params).toEqualTypeOf<OrderParams>();
  controller.setParam("amountMin", 3);
  // @ts-expect-error -- "amountMin" is a number
  controller.setParam("amountMin", "3");
  // @ts-expect-error -- pagination is set by setPagination
  controller.setParam("pageIndex", 1);
  return controller;
}

interface UnfilteredParams extends DataSliceParams<"Name"> {}

export function useUnfilteredController(): DataSliceController<UnfilteredParams, "Name"> {
  return useDataSliceControllerFromUrl<UnfilteredParams>(noParams, { Name: "Name" });
}
