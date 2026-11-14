/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import type { PaginationInfo } from "@neckar-hub/typescript-utils/openapi-types";
import { AlertTriangleIcon } from "lucide-react";
import type { PropsWithChildren } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Empty } from "../../feedback/empty";

/**
 * Prop Types
 */

export interface DataContainerContentTextKeys {
  invalidPagination: TextKey;
  noItemsFound: TextKey;
}

export type DataContainerContentProps = PropsWithChildren<{
  paginationInfo: PaginationInfo | undefined;
  textKeys?: Partial<DataContainerContentTextKeys>;
}>;

/**
 * Data Container Content
 *
 * @deprecated Use `DataSliceView` with `ResponsiveTable` instead.
 */

export function DataContainerContent({ children, paginationInfo, textKeys }: DataContainerContentProps) {
  const hasNoItems = paginationInfo?.filteredElementsCount === 0;
  if (hasNoItems) {
    return <NoItemsFound textKey={textKeys?.noItemsFound} />;
  }

  const paginationIsInvalid = paginationInfo !== undefined && paginationInfo.pageIndex >= paginationInfo.pageCount;
  if (paginationIsInvalid) {
    return <InvalidPagination textKey={textKeys?.invalidPagination} />;
  }

  return children;
}

/**
 * Invalid Pagination
 */

function InvalidPagination({ textKey }: { textKey?: TextKey }) {
  const { t } = useEmbeddedTranslation();

  return (
    <div className="flex h-80 flex-col items-center justify-center">
      <AlertTriangleIcon className="text-muted-foreground h-12 w-12" />
      <p className="text-muted-foreground mt-2 max-w-64 text-center text-sm">{t(textKey ?? "dataContainer.invalidPagination")}</p>
    </div>
  );
}

/**
 * No Items Found
 */

function NoItemsFound({ textKey }: { textKey?: TextKey }) {
  const { t } = useEmbeddedTranslation();

  return (
    <div className="flex h-80 flex-col items-center justify-center">
      <Empty className="w-20" />
      <p className="text-muted-foreground mt-2 max-w-64 text-center text-sm">{t(textKey ?? "dataContainer.noItemsFound")}</p>
    </div>
  );
}
