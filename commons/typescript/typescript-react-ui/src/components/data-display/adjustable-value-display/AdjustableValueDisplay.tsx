import { type TextKey } from "@neckar-hub/typescript-react-i18n";
import {
  AdjustableValueAdjustedStringWithTypeType,
  AdjustableValueAutoStringWithTypeType,
  AdjustableValueManualStringWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { InfoIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";
import { NoValue } from "../../text/no-value";

/**
 * Destructure type variables to enhance readability
 */
const { Adjusted } = AdjustableValueAdjustedStringWithTypeType;
const { Auto } = AdjustableValueAutoStringWithTypeType;
const { Manual } = AdjustableValueManualStringWithTypeType;

/**
 * Read-only shape of an `AdjustableValue<T>`.
 *
 * The generated wire types are monomorphized per value type (`AdjustableValueString`,
 * `AdjustableValueDouble`, `AdjustableValueAddressConfirmed`, …); they all match this structure, so
 * one display component serves every one of them.
 */
export type AdjustableValueLike<T> =
  | { type: typeof Auto; value: T }
  | { type: typeof Adjusted; calculated: T; value: T }
  | { type: typeof Manual; value: T };

/**
 * Prop Types
 */
export interface AdjustableValueDisplayProps<T> {
  /**
   * The value to render, or null/undefined when there is none
   */
  value: AdjustableValueLike<T> | null | undefined;
  /**
   * Renders the effective value. Defaults to rendering it as text — pass this for values that are
   * not strings (money, dates, addresses)
   */
  renderValue?: (value: T) => React.ReactNode;
  /**
   * Rendered when there is no value at all
   */
  fallback?: React.ReactNode;
  /**
   * Additional class name for the wrapper span element
   */
  className?: string | undefined;
  textKeys?: {
    calculatedValueLabel?: TextKey;
  };
}

/**
 * Renders an adjustable value read-only: the effective value, plus — when the user overrode a
 * calculated one — an info popover disclosing what the calculation produced.
 *
 * This is the read-only counterpart of [AdjustableValueTextInput] and shares its popover and its
 * text key, so an overridden value looks the same whether it is being edited or just displayed.
 * `Auto` and `Manual` render the plain value: neither carries a second value to disclose.
 */
export function AdjustableValueDisplay<T>({
  value,
  renderValue = (effective: T) => String(effective),
  fallback,
  className,
  textKeys,
}: AdjustableValueDisplayProps<T>): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const calculatedValueLabel = textKeys?.calculatedValueLabel ?? "adjustableValueFormField.calculatedValueLabel";

  if (value == null) {
    return <NoValue className={className}>{fallback}</NoValue>;
  }

  switch (value.type) {
    case Auto:
    case Manual:
      return <span className={className}>{renderValue(value.value)}</span>;

    case Adjusted:
      return (
        <span className={`inline-flex items-center gap-1 ${className ?? ""}`}>
          {renderValue(value.value)}
          <Popover>
            <PopoverTrigger asChild>
              <button type="button" className="text-muted-foreground inline-flex cursor-pointer items-center">
                <InfoIcon className="size-4" />
              </button>
            </PopoverTrigger>
            <PopoverContent className="text-sm">
              <div className="flex items-center justify-between gap-2 overflow-hidden">
                <div className="text-muted-foreground truncate">{t(calculatedValueLabel)}</div>
                <div className="text-nowrap">{renderValue(value.calculated)}</div>
              </div>
            </PopoverContent>
          </Popover>
        </span>
      );

    default:
      throw new Error(`Unexpected value in Switch Statement: value.type`);
  }
}
