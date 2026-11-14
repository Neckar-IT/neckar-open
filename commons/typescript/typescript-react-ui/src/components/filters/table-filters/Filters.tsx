import { SlidersHorizontalIcon, XIcon } from "lucide-react";
import { Fragment, type ReactElement } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Marker, MarkerContent } from "../../primitives/marker";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";
import { FilterTrigger } from "../filter-trigger";
import type { TableFilter } from "./TableFilter";

export interface FiltersProps {
  /** Shown in the toolbar, each as a button; a set one shows its value and an X. */
  primary: readonly TableFilter[];
  /** Shown in the popover behind the button "Filter"; the button turns blue and counts the set ones. */
  secondary?: readonly TableFilter[];
  /** The archive filter; the popover of the secondary filters shows it last, in a section of its own. */
  archive?: TableFilter;
}

/**
 * The filters of a table: the primary ones side by side, the secondary ones and the archive filter in one popover at every width.
 */
export function Filters({ primary: allPrimary, secondary: allSecondary = [], archive }: FiltersProps): ReactElement {
  const { t } = useEmbeddedTranslation();
  const primary = allPrimary.filter(isShown);
  const secondary = allSecondary.filter(isShown);

  return (
    <div role="group" aria-label={t("filterBar.filters")} className="flex w-full flex-wrap items-center gap-2">
      {primary.map((filter) => (
        <Fragment key={filter.key}>{filter.render({ label: t(filter.label), form: "Inline" })}</Fragment>
      ))}
      {secondary.length === 0 && archive === undefined ? null : <SecondaryFilters filters={secondary} archive={archive} />}
    </div>
  );
}

function isShown(filter: TableFilter): boolean {
  return filter.onlyWhileSet !== true || filter.active;
}

function SecondaryFilters({ filters, archive }: { filters: readonly TableFilter[]; archive: TableFilter | undefined }): ReactElement {
  const { t } = useEmbeddedTranslation();
  const label = t("filterBar.filters");
  const hasSecondary = filters.length > 0;
  const activeCount = [...filters, ...(archive ? [archive] : [])].filter((filter) => filter.active).length;

  return (
    <Popover>
      <PopoverTrigger asChild>
        <FilterTrigger
          icon={SlidersHorizontalIcon}
          label={label}
          count={activeCount}
          active={activeCount > 0}
          accessibleName={activeCount > 0 ? t({ key: "filterBar.filtersActive", options: { count: activeCount } }) : label}
        />
      </PopoverTrigger>
      <PopoverContent
        align="start"
        aria-label={label}
        className="flex max-h-[70vh] w-auto max-w-[min(26rem,calc(100vw-2rem))] min-w-[min(22rem,calc(100vw-2rem))] flex-col gap-4 overflow-y-auto p-4"
      >
        {filters.map((filter) => {
          const filterLabel = t(filter.label);
          return (
            <section key={filter.key} className="flex flex-col gap-2">
              <div className="flex h-6 items-center justify-between gap-2">
                <h3 className="text-muted-foreground text-xs font-semibold tracking-wider uppercase">{filterLabel}</h3>
                {filter.active && filter.reset !== undefined ? (
                  <button
                    type="button"
                    className="text-primary focus-visible:ring-ring/50 inline-flex items-center gap-1 rounded-md text-sm font-medium outline-none focus-visible:ring-3"
                    onClick={filter.reset}
                  >
                    <XIcon aria-hidden className="size-3.5" />
                    {t("filterBar.reset")}
                  </button>
                ) : null}
              </div>
              {filter.render({ label: filterLabel, form: "Panel" })}
            </section>
          );
        })}
        {archive ? (
          <section key={archive.key} className="flex flex-col gap-2">
            <Marker variant={hasSecondary ? "separator" : "default"}>
              <MarkerContent role="heading" aria-level={3} className="text-xs font-semibold tracking-wider uppercase">
                {t(archive.label)}
              </MarkerContent>
            </Marker>
            {archive.render({ label: t(archive.label), form: "Panel" })}
          </section>
        ) : null}
      </PopoverContent>
    </Popover>
  );
}
