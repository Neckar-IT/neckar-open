import {
  enumParam,
  instantParam,
  type ParamDefinition,
  useDataSliceControllerFromUrl,
} from "@neckar-hub/typescript-react-utils/data-slice-filters";
import { PriceTimelineEntriesSortCriteria, ValueTimelineEntryStatus } from "@neckar-hub/typescript-utils/openapi-types";
import type { DataSliceController, DataSliceParams } from "../layout/data-slice-view";

/**
 * The request parameters every value-timeline overview shares: pagination, sorting and the filters `validFromAfter`
 * and `status`. The generated `Get*TimelineFor*Params` of each parent extend them.
 */
export interface TimelineDataSliceParams extends DataSliceParams<PriceTimelineEntriesSortCriteria> {
  /** ISO-8601 instant; keeps the entries with a later `validFrom`. */
  validFromAfter?: string;
  /** One value of `ValueTimelineEntryStatus`. */
  status?: string;
}

/**
 * Pagination, sorting and filter state of a value-timeline overview table. Sort defaults to descending `validFrom`
 * (newest first) on the backend when no `sortBy` is passed.
 */
export type TimelineDataSliceController<TParams extends TimelineDataSliceParams> = Pick<
  DataSliceController<TParams, PriceTimelineEntriesSortCriteria>,
  "params" | "setPagination" | "setSorting" | "setParam" | "setParams"
>;

export const timelineParamDefinition: ParamDefinition<TimelineDataSliceParams> = {
  validFromAfter: instantParam(),
  status: enumParam(ValueTimelineEntryStatus),
};

/**
 * The controller of a value-timeline overview whose state lives in the URL; a filter change goes back to the first page.
 */
export function useTimelineDataSliceControllerFromUrl<TParams extends TimelineDataSliceParams>(
  prefix?: string,
): TimelineDataSliceController<TParams> {
  // The parent-specific `TParams` add only parameters the overview never sets, such as `scope`.
  return useDataSliceControllerFromUrl<TParams>(
    timelineParamDefinition as ParamDefinition<TParams>,
    PriceTimelineEntriesSortCriteria,
    prefix,
  );
}
