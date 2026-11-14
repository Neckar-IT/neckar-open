/**
 * Imports
 */

import { useDebounceCallback } from "@neckar-hub/typescript-react-utils/utility-hooks";
import { cn } from "cn";
import { SearchIcon, TextCursorInputIcon, XIcon } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Input, type InputProps } from "../../primitives/input";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";
import { FilterTrigger } from "../filter-trigger";

/**
 * Prop Types
 */

interface FilterByTextInputProps extends Omit<InputProps, "type" | "value" | "defaultValue" | "onChange" | "form"> {
  delay?: number | undefined;
  value: string | undefined;
  /** Debounced by `delay`. */
  onChange: (value: string | undefined) => void;
  /**
   * `Search` is the search field of the filter bar, `Inline` a filter button with the field in a popover, `Panel` the
   * plain field. Defaults to `Search`.
   */
  form?: "Search" | "Inline" | "Panel";
  /** The label of the filter; the accessible name of the field and the text of the `Inline` button. */
  label?: string | undefined;
}

/**
 * Text input for filtering tables or lists.
 *
 * @prop {number} [delay=300] - Debounce delay in milliseconds.
 */
export function FilterByTextInput({ form = "Search", ...props }: FilterByTextInputProps): React.ReactElement {
  switch (form) {
    case "Search":
      return (
        <TextField
          deliverPendingOnUnmount={false}
          {...props}
          className={cn("min-w-0 flex-1 @3xl:w-72 @3xl:flex-none", props.className)}
          inputClassName="h-11 text-base @3xl:h-9 @3xl:text-sm"
        />
      );
    case "Inline":
      return <InlineTextField {...props} />;
    case "Panel":
      return <TextField {...props} inputClassName="h-11 text-base" deliverPendingOnUnmount />;
    default:
      throw new Error(`Unexpected value in Switch Statement: form`);
  }
}

function InlineTextField(props: Omit<FilterByTextInputProps, "form">): React.ReactElement {
  const label = props.label ?? "";
  const active = props.value !== undefined && props.value !== "";
  return (
    <Popover>
      <PopoverTrigger asChild>
        <FilterTrigger
          icon={TextCursorInputIcon}
          label={label}
          valueText={props.value}
          active={active}
          onClear={() => props.onChange(undefined)}
        />
      </PopoverTrigger>
      <PopoverContent align="start" className="w-72 p-2">
        <TextField {...props} autoFocus deliverPendingOnUnmount />
      </PopoverContent>
    </Popover>
  );
}

/**
 * The field keeps its own text while the debounce runs. A changed value from outside that is not the echo of its own
 * change wins, e.g. "Alle zurücksetzen"; closing a popover delivers the pending text instead of dropping it.
 */
function TextField({
  delay,
  value: outsideValue,
  onChange,
  label,
  className,
  inputClassName,
  deliverPendingOnUnmount,
  ...inputProps
}: Omit<FilterByTextInputProps, "form"> & { inputClassName?: string; deliverPendingOnUnmount: boolean }): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const [value, setValue] = useState<string | undefined>(outsideValue);
  const latestOnChange = useRef(onChange);
  useEffect(() => {
    latestOnChange.current = onChange;
  });
  const pendingChange = useRef<{ value: string | undefined } | undefined>(undefined);
  const lastEmitted = useRef<string | undefined>(outsideValue);

  const emitChange = useCallback((newValue: string | undefined) => {
    pendingChange.current = undefined;
    lastEmitted.current = newValue;
    latestOnChange.current(newValue);
  }, []);
  const debouncedOnChange = useDebounceCallback(emitChange, delay ?? 300);

  useEffect(() => {
    if (outsideValue !== lastEmitted.current) {
      debouncedOnChange.cancel();
      pendingChange.current = undefined;
      lastEmitted.current = outsideValue;
      setValue(outsideValue);
    }
  }, [outsideValue, debouncedOnChange]);

  useEffect(
    () => () => {
      debouncedOnChange.cancel();
      // The search field unmounts with its page; its pending text would land in the URL of the next one.
      if (deliverPendingOnUnmount && pendingChange.current !== undefined) {
        latestOnChange.current(pendingChange.current.value);
      }
    },
    [debouncedOnChange, deliverPendingOnUnmount],
  );

  const handleChange = (newValue: string | undefined) => {
    const changed = newValue === "" ? undefined : newValue;
    setValue(newValue);
    pendingChange.current = { value: changed };
    debouncedOnChange(changed);
  };

  return (
    <div className={cn("relative", className)}>
      <SearchIcon aria-hidden className="text-muted-foreground absolute top-1/2 left-3 size-4 -translate-y-1/2" />
      <Input
        aria-label={label}
        placeholder={label}
        {...inputProps}
        className={cn("pr-10 pl-9", value ? "border-primary/40 bg-primary/10" : null, inputClassName)}
        value={value ?? ""}
        onChange={(event) => handleChange(event.target.value)}
      />
      {value ? (
        <button
          type="button"
          aria-label={t("filterBar.clearSearch")}
          className="text-muted-foreground hover:text-foreground focus-visible:ring-ring/50 absolute top-1/2 right-1 grid size-8 -translate-y-1/2 place-items-center rounded-md outline-none focus-visible:ring-3"
          onClick={() => handleChange(undefined)}
        >
          <XIcon aria-hidden className="size-4" />
        </button>
      ) : null}
    </div>
  );
}
