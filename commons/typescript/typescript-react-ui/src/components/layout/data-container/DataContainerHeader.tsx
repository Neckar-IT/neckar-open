/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { Loader2Icon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Delayed } from "../../feedback/delayed";

/**
 * Prop Types
 */

export interface DataContainerHeaderTextKeys {
  loading: TextKey;
}

export interface DataContainerHeaderProps {
  topLeftComponents?: React.ReactNode | undefined;
  topRightComponents?: React.ReactNode | undefined;
  isFetching?: boolean | undefined;
  textKeys?: Partial<DataContainerHeaderTextKeys>;
}

/**
 * Data Container Header
 *
 * @deprecated Use `DataSliceView` with `ResponsiveTable` instead.
 */

export function DataContainerHeader({ topLeftComponents, topRightComponents, isFetching = false, textKeys }: DataContainerHeaderProps) {
  const { t } = useEmbeddedTranslation();

  return (
    <div className="flex flex-col items-start justify-between space-y-2 lg:flex-row lg:space-y-0">
      <div className="flex w-full items-center space-x-4 lg:max-w-fit">
        {topLeftComponents}
        {(topLeftComponents !== undefined || topRightComponents !== undefined) && isFetching ? (
          <Delayed>
            <div className="flex items-center space-x-1.5">
              <Loader2Icon className="text-muted-foreground h-4 w-4 animate-spin" />
              <span className="text-muted-foreground text-sm whitespace-nowrap">{t(textKeys?.loading ?? "dataContainer.loading")}</span>
            </div>
          </Delayed>
        ) : null}
      </div>
      {topRightComponents}
    </div>
  );
}
