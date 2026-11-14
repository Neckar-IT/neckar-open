import { cn } from "cn";
import { ChevronDownIcon, type LucideIcon, XIcon } from "lucide-react";
import type { ComponentProps, ReactElement, ReactNode } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";

/** The surface of a filter control while its value differs from the default. */
export const activeFilterSurface = "border-primary/40 bg-primary/10 text-primary hover:bg-primary/15";

export interface FilterTriggerProps extends Omit<ComponentProps<"button">, "value" | "children"> {
  icon: LucideIcon;
  /** The translated label, visible in every state. */
  label: string;
  /** The set value; without it the button shows the label alone. */
  valueText?: ReactNode;
  /** A number badge after the label, e.g. the set filters on "Filter (n)". */
  count?: number | undefined;
  active: boolean;
  /** Renders the X that clears the filter while it is active. */
  onClear?: (() => void) | undefined;
  /** The accessible name of the button; defaults to label and value, read off the content for a value that is no text. */
  accessibleName?: string | undefined;
}

/**
 * The one button design of every filter: 44 px high, 36 px from the container width `@3xl` of the enclosing
 * `@container` on, solid border, label always visible, the set value in the button and colored while active. Spreads
 * the remaining props onto the button, so a `PopoverTrigger asChild` can wrap it.
 */
export function FilterTrigger({
  icon: Icon,
  label,
  valueText,
  count,
  active,
  onClear,
  accessibleName,
  className,
  ...buttonProps
}: FilterTriggerProps): ReactElement {
  const { t } = useEmbeddedTranslation();
  const showsValue = valueText !== undefined && valueText !== null;
  const defaultName = showsValue ? (typeof valueText === "string" ? `${label}: ${valueText}` : undefined) : label;

  return (
    <span
      data-slot="filter-trigger"
      data-active={active}
      className={cn(
        "bg-background inline-flex h-11 max-w-full items-center rounded-md border text-sm shadow-xs transition-colors @3xl:h-9",
        active ? activeFilterSurface : "border-input hover:border-muted-foreground",
        className,
      )}
    >
      <button
        type="button"
        aria-label={accessibleName ?? defaultName}
        className="focus-visible:ring-ring/50 inline-flex h-full min-w-0 items-center gap-1.5 rounded-md px-3 outline-none focus-visible:ring-3"
        {...buttonProps}
      >
        <Icon aria-hidden className={cn("size-4 shrink-0", active ? "text-primary" : "text-muted-foreground")} />
        <span className={cn("shrink-0", active ? "text-primary" : "text-muted-foreground")}>
          {label}
          {showsValue ? ":" : null}
        </span>
        {showsValue ? (
          <>
            {" "}
            <span className="truncate font-medium">{valueText}</span>
          </>
        ) : null}
        {count !== undefined && count > 0 ? (
          <span className="bg-primary text-primary-foreground inline-grid h-5 min-w-5 place-items-center rounded-full px-1.5 text-xs font-semibold tabular-nums">
            {count}
          </span>
        ) : null}
        {buttonProps["aria-haspopup"] === undefined ? null : (
          <ChevronDownIcon aria-hidden className="text-muted-foreground size-3.5 shrink-0" />
        )}
      </button>
      {active && onClear !== undefined ? (
        <button
          type="button"
          aria-label={t({ key: "filterBar.removeFilter", options: { label } })}
          className="text-muted-foreground hover:text-foreground focus-visible:ring-ring/50 inline-grid h-full place-items-center rounded-md pr-2.5 pl-1 outline-none focus-visible:ring-3"
          onClick={onClear}
        >
          <XIcon aria-hidden className="size-3.5" />
        </button>
      ) : null}
    </span>
  );
}
