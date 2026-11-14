import { parseAsInteger, useQueryStates } from "nuqs";
import { DEFAULT_PAGE_INDEX, DEFAULT_PAGE_SIZE, type OnPaginationParamsChange, type PaginationParams } from "./paginationParams";

/**
 * Hook to manage pagination search parameters
 *
 * @param defaultPageSize - The default page size to use if not specified (defaults to 10)
 * @param prefix - Optional prefix for the URL keys
 *
 * @returns A triple of the current pagination parameters, a function to update them and a function that resets the
 *          pageIndex alone. The pagination parameters will always be defined, if not set in the URL, they will default
 *          to specified values.
 */

export function usePaginationSearchParams(defaultPageSize: number = DEFAULT_PAGE_SIZE, prefix?: string) {
  const [param, setParam] = useQueryStates(
    {
      pageIndex: parseAsInteger.withDefault(DEFAULT_PAGE_INDEX),
      pageSize: parseAsInteger.withDefault(defaultPageSize),
    },
    {
      history: "push",
      urlKeys: {
        pageIndex: prefix ? `${prefix}PageIndex` : "pageIndex",
        pageSize: prefix ? `${prefix}PageSize` : "pageSize",
      },
    },
  );

  const setPagination: OnPaginationParamsChange = (valueOrUpdater) => {
    if (typeof valueOrUpdater === "function") {
      void setParam((old) => {
        const oldPaginationValue: PaginationParams = {
          pageIndex: old.pageIndex,
          pageSize: old.pageSize,
        };
        return valueOrUpdater(oldPaginationValue);
      });
    } else if (valueOrUpdater === undefined) {
      void setParam(null); // reset to defaults
    } else {
      void setParam(valueOrUpdater);
    }
  };

  // Writes the partial, so a table whose pageSize differs from the default keeps it in the URL.
  const resetPageIndex = () => void setParam({ pageIndex: DEFAULT_PAGE_INDEX });

  const pagination: PaginationParams = {
    pageIndex: param.pageIndex,
    pageSize: param.pageSize,
  };

  return [pagination, setPagination, resetPageIndex] as const;
}
