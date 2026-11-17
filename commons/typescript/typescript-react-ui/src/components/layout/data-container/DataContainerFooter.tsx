/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import type { PaginationInfo } from "@neckar-hub/typescript-utils/openapi-types";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { type OnPaginationChange, Pagination, type PaginationTextKeys } from "../../navigation/pagination";

/**
 * Prop Types
 */

export type DataContainerFooterProps = {
  paginationInfo: PaginationInfo | undefined;
  onSetPagination: OnPaginationChange;
  disabledPagination?: boolean | undefined;
  pageSizeOptions?: number[] | undefined;
  textKeys?: Partial<PaginationTextKeys & { itemsLabel: TextKey }>;
};

/**
 * Data Container Footer Component
 *
 * @deprecated Use `DataSliceView` with `ResponsiveTable` instead.
 */

export function DataContainerFooter({
  paginationInfo,
  onSetPagination,
  disabledPagination = false,
  pageSizeOptions,
  textKeys,
}: DataContainerFooterProps) {
  const { t } = useEmbeddedTranslation();

  if (paginationInfo !== undefined) {
    return (
      <div className="flex flex-col items-end justify-between space-y-2 lg:flex-row lg:items-center lg:space-y-0">
        <p className="text-muted-foreground text-sm">
          {t({
            key: textKeys?.itemsLabel ?? "dataContainer.items",
            options: {
              filteredElementsCount: paginationInfo.filteredElementsCount,
              totalElementsCount: paginationInfo.totalElementsCount,
            },
          })}
        </p>
        <Pagination
          disabled={disabledPagination}
          pageCount={paginationInfo.pageCount}
          pageIndex={paginationInfo.pageIndex}
          pageSize={paginationInfo.pageSize}
          pageSizeOptions={pageSizeOptions}
          onChange={onSetPagination}
          textKeys={textKeys}
        />
      </div>
    );
  }

  return (
    <div className="mt-2 flex items-center justify-end">
      <Pagination
        disabled={true}
        pageCount={0}
        pageIndex={0}
        pageSize={pageSizeOptions?.[0] ?? 10}
        pageSizeOptions={pageSizeOptions}
        onChange={onSetPagination}
      />
    </div>
  );
}
