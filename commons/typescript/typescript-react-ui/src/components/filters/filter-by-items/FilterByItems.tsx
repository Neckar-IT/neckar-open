/**
 * Imports
 */

import { cn } from "cn";
import { CheckIcon, ListFilterIcon, type LucideIcon } from "lucide-react";
import { type ReactNode, useState } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Checkbox as CheckboxPrimitive } from "../../primitives/checkbox";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList, CommandSeparator } from "../../primitives/command";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";
import { RadioGroup, RadioGroupItem } from "../../primitives/radio-group";
import { FilterTrigger } from "../filter-trigger";

/**
 * Type Declarations
 */

type Option = {
  value: string;
  /** Text of the option in the trigger button. */
  label: string;
  /** Rich rendering in the list and beside the radio or checkbox of the `Panel` form. Falls back to label. */
  popoverLabel?: ReactNode;
};

interface Single {
  mode: "single";
  value: string | undefined;
  onChange: (value: string | undefined) => void;
}

interface Multiple {
  mode: "multiple";
  value: readonly string[] | undefined;
  onChange: (value: readonly string[] | undefined) => void;
}

export type FilterByItemsProps = (Single | Multiple) & {
  options: Option[];
  /** The label of the filter, visible in every state. */
  placeholder: string;
  /** `Inline` is a button with a list in a popover, `Panel` a radio group or a group of checkboxes. Defaults to `Inline`. */
  form?: "Inline" | "Panel";
  icon?: LucideIcon;
  className?: string | undefined;
  disabled?: boolean | undefined;
};

/**
 * Filter control for one or several items of a fixed option list.
 */
export function FilterByItems(props: FilterByItemsProps): React.ReactElement {
  const selected: readonly string[] = selectedValuesOf(props);

  const toggle = (value: string) => {
    switch (props.mode) {
      case "single":
        props.onChange(selected.includes(value) ? undefined : value);
        break;
      case "multiple": {
        const next = selected.includes(value) ? selected.filter((selectedValue) => selectedValue !== value) : [...selected, value];
        props.onChange(next.length === 0 ? undefined : next);
        break;
      }
      default:
        throw new Error(`Unexpected value in Switch Statement: mode`);
    }
  };

  switch (props.form ?? "Inline") {
    case "Inline":
      return <InlineItems {...props} selected={selected} onToggle={toggle} />;
    case "Panel":
      return (
        <ItemList
          mode={props.mode}
          options={props.options}
          selected={selected}
          onToggle={toggle}
          onSelect={(value) => {
            if (props.mode === "single") {
              props.onChange(value);
            }
          }}
          label={props.placeholder}
          disabled={props.disabled === true}
          className={props.className}
        />
      );
    default:
      throw new Error(`Unexpected value in Switch Statement: form`);
  }
}

function selectedValuesOf(props: Single | Multiple): readonly string[] {
  switch (props.mode) {
    case "single":
      return props.value === undefined ? [] : [props.value];
    case "multiple":
      return props.value ?? [];
    default:
      throw new Error(`Unexpected value in Switch Statement: mode`);
  }
}

/**
 * The text of a selection in the trigger button: the labels up to two items, the count from three.
 */
function useSelectedItemsText(): (labels: readonly string[]) => string | undefined {
  const { t } = useEmbeddedTranslation();
  return (labels) => {
    if (labels.length === 0) {
      return undefined;
    }
    return labels.length < 3 ? labels.join(", ") : t({ key: "filterBar.itemsSelected", options: { count: labels.length } });
  };
}

function InlineItems(props: FilterByItemsProps & { selected: readonly string[]; onToggle: (value: string) => void }): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const selectedText = useSelectedItemsText();
  const [isOpen, setOpen] = useState(false);
  const { options, selected, onToggle, placeholder } = props;
  const hasSelection = selected.length > 0;

  const clear = () => {
    props.onChange(undefined);
  };

  return (
    <Popover open={isOpen} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <FilterTrigger
          icon={props.icon ?? ListFilterIcon}
          label={placeholder}
          valueText={selectedText(options.filter((option) => selected.includes(option.value)).map((option) => option.label))}
          active={hasSelection}
          onClear={props.disabled === true ? undefined : clear}
          className={props.className}
          disabled={options.length === 0 || props.disabled === true}
        />
      </PopoverTrigger>
      <PopoverContent align="start" aria-label={placeholder} className="w-[280px] p-0">
        <Command>
          {options.length > 7 ? (
            <CommandInput
              placeholder={placeholder}
              // workaround: dependency=pnpm:cmdk:1.1.1 observed-on=2026-10-07 upstream=https://github.com/pacocoursey/cmdk/issues/379
              // cmdk intercepts Home and End for list navigation and blocks the native cursor movement in the input.
              onKeyDown={(event) => {
                if (event.key === "Home" || event.key === "End") {
                  event.stopPropagation();
                }
              }}
            />
          ) : null}
          <CommandList label={placeholder} aria-multiselectable={props.mode === "multiple" ? true : undefined}>
            <CommandEmpty>{t("filterBar.noOptionsFound")}</CommandEmpty>
            <CommandGroup>
              {options.map((option) => (
                <CommandItem
                  key={option.value}
                  aria-checked={selected.includes(option.value)}
                  // The primitive draws its check mark on `data-checked`; a multiple choice shows its own checkbox instead.
                  data-checked={props.mode === "single" ? selected.includes(option.value) : undefined}
                  className="cursor-pointer"
                  onSelect={() => {
                    onToggle(option.value);
                    if (props.mode === "single") {
                      setOpen(false);
                    }
                  }}
                >
                  {props.mode === "multiple" ? <Checkbox checked={selected.includes(option.value)} /> : null}
                  <span>{option.popoverLabel ?? option.label}</span>
                </CommandItem>
              ))}
            </CommandGroup>
            {hasSelection ? (
              <>
                <CommandSeparator />
                <CommandGroup>
                  <CommandItem className="cursor-pointer justify-center text-center" onSelect={clear}>
                    {t("filterBar.clear")}
                  </CommandItem>
                </CommandGroup>
              </>
            ) : null}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}

/** The checkbox before an option of a multiple choice. */
function Checkbox({ checked }: { checked: boolean }): React.ReactElement {
  return (
    <span
      aria-hidden
      className={cn(
        "border-input grid size-4 shrink-0 place-items-center rounded-sm border",
        checked ? "bg-primary border-primary text-primary-foreground" : "[&_svg]:invisible",
      )}
    >
      <CheckIcon className="size-3" />
    </span>
  );
}

/**
 * The options in two columns where a label wider than a column takes the whole row, the form inside the filter popover: a radio group for a single
 * choice, a group of checkboxes for a multiple choice.
 */
function ItemList({
  mode,
  options,
  selected,
  onToggle,
  onSelect,
  label,
  disabled,
  className,
}: {
  mode: "single" | "multiple";
  options: Option[];
  selected: readonly string[];
  /** Flips one option of a multiple choice. */
  onToggle: (value: string) => void;
  /** Picks the option of a single choice. */
  onSelect: (value: string) => void;
  label: string;
  disabled: boolean;
  className?: string | undefined;
}): React.ReactElement {
  const rowClassName =
    "flex max-w-full min-w-[calc(50%-0.5rem)] grow cursor-pointer items-center gap-3 text-sm has-disabled:cursor-not-allowed has-disabled:opacity-50";

  switch (mode) {
    case "single":
      return (
        <RadioGroup
          aria-label={label}
          disabled={disabled}
          value={selected[0] ?? ""}
          onValueChange={onSelect}
          className={cn("flex flex-wrap gap-x-4 gap-y-2", className)}
        >
          {options.map((option) => (
            <label key={option.value} className={rowClassName}>
              <RadioGroupItem value={option.value} />
              {option.popoverLabel ?? option.label}
            </label>
          ))}
        </RadioGroup>
      );
    case "multiple":
      return (
        <div role="group" aria-label={label} className={cn("flex flex-wrap gap-x-4 gap-y-2", className)}>
          {options.map((option) => (
            <label key={option.value} className={rowClassName}>
              <CheckboxPrimitive
                checked={selected.includes(option.value)}
                disabled={disabled}
                onCheckedChange={() => onToggle(option.value)}
              />
              {option.popoverLabel ?? option.label}
            </label>
          ))}
        </div>
      );
    default:
      throw new Error(`Unexpected value in Switch Statement: mode`);
  }
}
