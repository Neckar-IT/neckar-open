import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import { ChevronLeftIcon, ChevronRightIcon, ChevronsLeftIcon, ChevronsRightIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button } from "../../primitives/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../primitives/select";

export type OnPaginationChange = (pageIndex: number, pageSize: number) => void;

export interface PaginationTextKeys {
  itemsPerPage: TextKey;
  page: TextKey;
  goToFirstPage: TextKey;
  goToPreviousPage: TextKey;
  goToNextPage: TextKey;
  goToLastPage: TextKey;
}

export type PaginationProps = {
  pageIndex: number;
  pageSize: number;
  pageSizeOptions?: number[] | undefined;
  pageCount: number;
  onChange: OnPaginationChange;
  className?: string;
  disabled?: boolean | undefined;
  textKeys?: Partial<PaginationTextKeys>;
};

const DEFAULT_PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

/**
 * Custom Pagination Component, inspired by shadcn's Task Table Pagination (https://ui.shadcn.com/examples/tasks)
 */
export function Pagination({
  pageIndex,
  pageSize,
  pageSizeOptions = DEFAULT_PAGE_SIZE_OPTIONS,
  pageCount,
  onChange,
  className,
  disabled = false,
  textKeys,
}: PaginationProps) {
  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults: PaginationTextKeys = {
    itemsPerPage: textKeys?.itemsPerPage ?? "pagination.itemsPerPage",
    page: textKeys?.page ?? "pagination.page",
    goToFirstPage: textKeys?.goToFirstPage ?? "pagination.goToFirstPage",
    goToPreviousPage: textKeys?.goToPreviousPage ?? "pagination.goToPreviousPage",
    goToNextPage: textKeys?.goToNextPage ?? "pagination.goToNextPage",
    goToLastPage: textKeys?.goToLastPage ?? "pagination.goToLastPage",
  };

  // when changing the page size, we want to adjust the page index to try to keep mostly the same items in view
  const handleChangePageSize = (value: string) => {
    const newPageSize = Number(value);
    const newPageIndex = Math.floor((pageIndex * pageSize) / newPageSize);
    onChange(newPageIndex, newPageSize);
  };

  return (
    <div className={cn("flex flex-col items-end space-y-2 sm:flex-row sm:items-center sm:space-y-0 sm:space-x-6 lg:space-x-8", className)}>
      <div className="flex items-center space-x-2">
        <p className={cn("text-sm font-medium", { "text-muted-foreground": disabled })}>{t(textKeysWithDefaults.itemsPerPage)}</p>
        <Select disabled={disabled} value={pageSize.toString()} onValueChange={handleChangePageSize}>
          <SelectTrigger className="h-8 w-[70px]" data-testid="page-size-selector">
            <SelectValue placeholder={pageSize} />
          </SelectTrigger>
          <SelectContent side="top">
            {pageSizeOptions.map((desiredPageSize) => (
              <SelectItem key={desiredPageSize} value={desiredPageSize.toString()}>
                {desiredPageSize}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="flex space-x-2">
        <div
          className={cn("flex items-center text-sm font-medium sm:justify-center", {
            "text-muted-foreground": disabled,
          })}
        >
          {t({ key: textKeysWithDefaults.page, options: { pageNumber: pageIndex + 1, pageCount } })}
        </div>
        <div className="flex items-center">
          <Button
            className="h-8 w-8 rounded-se-none rounded-ee-none p-0"
            data-testid="first-page-button"
            disabled={disabled || pageIndex === 0}
            variant="outline"
            onClick={() => {
              onChange(0, pageSize);
            }}
          >
            <span className="sr-only">{t(textKeysWithDefaults.goToFirstPage)}</span>
            <ChevronsLeftIcon className="h-4 w-4" />
          </Button>
          <Button
            className="h-8 w-8 rounded-none border-l-0 p-0"
            data-testid="previous-page-button"
            disabled={disabled || pageIndex === 0}
            variant="outline"
            onClick={() => {
              onChange(pageIndex - 1, pageSize);
            }}
          >
            <span className="sr-only">{t(textKeysWithDefaults.goToPreviousPage)}</span>
            <ChevronLeftIcon className="h-4 w-4" />
          </Button>
          <Button
            className="h-8 w-8 rounded-none border-x-0 p-0"
            data-testid="next-page-button"
            disabled={disabled || pageIndex >= pageCount - 1}
            variant="outline"
            onClick={() => {
              onChange(pageIndex + 1, pageSize);
            }}
          >
            <span className="sr-only">{t(textKeysWithDefaults.goToNextPage)}</span>
            <ChevronRightIcon className="h-4 w-4" />
          </Button>
          <Button
            className="h-8 w-8 rounded-ss-none rounded-es-none p-0"
            data-testid="last-page-button"
            disabled={disabled || pageIndex >= pageCount - 1}
            variant="outline"
            onClick={() => {
              onChange(pageCount - 1, pageSize);
            }}
          >
            <span className="sr-only">{t(textKeysWithDefaults.goToLastPage)}</span>
            <ChevronsRightIcon className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );
}
