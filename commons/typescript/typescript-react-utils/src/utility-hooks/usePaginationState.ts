import { useState } from "react";
import {
  DEFAULT_PAGE_INDEX,
  DEFAULT_PAGE_SIZE,
  type OnPaginationParamsChange,
  type PaginationParams,
} from "../search-params/paginationParams";

export interface PaginationStateController {
  pagination: PaginationParams;
  setPagination: OnPaginationParamsChange;

  /**
   * Wraps the setter of a filter, of the free text or of the sorting so that every change also
   * resets the pageIndex. The local-state counterpart of {@link useSetterWithPaginationReset}.
   */
  withPaginationReset: <T>(setParam: (value: T) => void) => (value: T) => void;
}

/**
 * Pagination in the component's own state, for a list whose paging must not reach the URL.
 * React drops the state with the component, so the list opens on the first page again.
 */
export function usePaginationState(defaultPageSize: number = DEFAULT_PAGE_SIZE): PaginationStateController {
  const [pagination, setPaginationValue] = useState<PaginationParams>({ pageIndex: DEFAULT_PAGE_INDEX, pageSize: defaultPageSize });

  const setPagination: OnPaginationParamsChange = (valueOrUpdater) => {
    if (valueOrUpdater === undefined) {
      setPaginationValue({ pageIndex: DEFAULT_PAGE_INDEX, pageSize: defaultPageSize });
    } else {
      setPaginationValue(valueOrUpdater);
    }
  };

  return {
    pagination,
    setPagination,
    withPaginationReset: (setParam) => (value) => {
      setParam(value);
      setPaginationValue((old) => ({ ...old, pageIndex: DEFAULT_PAGE_INDEX }));
    },
  };
}
