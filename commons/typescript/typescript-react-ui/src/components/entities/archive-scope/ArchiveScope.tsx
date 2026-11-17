import { ArchiveFilter } from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { activeFilterSurface } from "../../filters/filter-trigger";
import { ToggleGroup, ToggleGroupItem } from "../../primitives/toggle-group";

interface ArchiveScopeProps {
  value: ArchiveFilter;
  onChange: (value: ArchiveFilter) => void;
  /** Translated, e.g. "Archiv"; names the group for assistive technology. */
  label: string;
}

/**
 * The view switch over an entity's archive state, a scope rather than a filter because archiving is a soft delete:
 * a toggle group with the options only active, only archived and all, of which exactly one is selected.
 */
export function ArchiveScope({ value, onChange, label }: ArchiveScopeProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const active = value !== ArchiveFilter.OnlyActive;

  return (
    <ToggleGroup
      type="single"
      variant="outline"
      aria-label={label}
      data-active={active}
      className="w-full"
      value={value}
      // Radix reports "" when the selected item is pressed again; the scope keeps its value then.
      onValueChange={(newValue) => (newValue === "" ? undefined : onChange(newValue as ArchiveFilter))}
    >
      {Object.values(ArchiveFilter).map((state) => (
        <ToggleGroupItem
          key={state}
          value={state}
          // The selected state other than the default takes the colors of an active filter button; the joined border needs its left edge.
          className={cn(
            "flex-1 px-4 whitespace-nowrap",
            active && state === value ? cn(activeFilterSurface, "data-[state=on]:bg-primary/10 z-10 border-l! not-first:-ml-px") : null,
          )}
        >
          {t(`archiveScope.${state}`)}
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  );
}
