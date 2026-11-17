import { cn } from "cn";
import { CheckIcon, ToggleLeftIcon } from "lucide-react";
import { FilterTrigger } from "../filter-trigger";

interface FilterByBooleanInputProps {
  /** Translated. */
  label: string;
  value: boolean;
  onChange: (value: boolean) => void;
  /** `Inline` is a filter button that toggles, `Panel` a toggle chip. Defaults to `Inline`. */
  form?: "Inline" | "Panel";
  className?: string | undefined;
}

/**
 * Filter control for a yes/no question whose "no" filters nothing, e.g. "only hanging modules".
 */
export function FilterByBooleanInput({
  label,
  value,
  onChange,
  form = "Inline",
  className,
}: FilterByBooleanInputProps): React.ReactElement {
  switch (form) {
    case "Inline":
      return (
        <FilterTrigger
          icon={ToggleLeftIcon}
          label={label}
          active={value}
          aria-pressed={value}
          className={className}
          onClick={() => onChange(!value)}
        />
      );
    case "Panel":
      return (
        <button
          type="button"
          aria-pressed={value}
          className={cn(
            "focus-visible:ring-ring/50 inline-flex min-h-11 w-fit items-center gap-1.5 rounded-full border px-4 text-sm outline-none focus-visible:ring-3",
            value ? "bg-primary border-primary text-primary-foreground font-medium" : "border-input bg-background",
            className,
          )}
          onClick={() => onChange(!value)}
        >
          {value ? <CheckIcon aria-hidden className="size-3.5" /> : null}
          {label}
        </button>
      );
    default:
      throw new Error(`Unexpected value in Switch Statement: form`);
  }
}
